package ru.otus.quiz.server;

import ru.otus.quiz.server.dbfiles.quizcreateintodb.QuizCreateDb;
import ru.otus.quiz.server.dbfiles.quizcreateintodb.QuizFileCreate;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;

public class ClientPart {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private int userId = 1;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public DataInputStream getIn() {
        return in;
    }

    public Socket getSocket() {
        return socket;
    }

    public ClientPart(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                System.out.println("Подключился новый клиент");
                sendMessage("Список команд /help");

                while (true) {
                    String message = in.readUTF();

                    if (message.equals("/exit")) {
                        sendMessage("/disconnect");
                        break;
                    } else if (message.equals("/help")) {
                        commandHelp();
                    } else if (message.startsWith("/auth ")) {
                        String[] str = message.split(" ");
                        if (str.length != 3) {
                            sendMessage("Неверный формат команды /auth");
                            continue;
                        }
                        if (this.server.getAuthenticationProvider().authenticate(this, str[1], str[2])) {
                            userId = this.server.getAuthenticationProvider().getUserIdByUsername(username);
                            server.getClients().put(username, this);
                            break;
                        }
                        continue;
                    } else if (message.startsWith("/reg")) {
                        String login = inputData("Введите логин:");
                        String name = inputData("Введите имя:");
                        String password = inputData("Введите пароль:");

                        if (this.server.getAuthenticationProvider().isUserExists(login, name, password)) {
                            sendMessage("Такой пользователь уже зарегестрирован или имя занято.");
                            continue;
                        } else {
                            this.server.getAuthenticationProvider().createUser(login, name, password);
                            sendMessage("Вы успешно прошли регистрацию.");
                            userId = this.server.getAuthenticationProvider().getUserIdByUsername(name);
                            this.server.getAuthenticationProvider().authenticate(this, login, password);
                            server.getClients().put(name, this);
                            break;
                        }
                    }

                    sendMessage("Сначала нужно пройти аутентификацию '/auth login password' или регистрацию " +
                            "'/reg'");
                }

                while (true) {
                    String message = in.readUTF();

                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMessage("/disconnect");
                            break;
                        } else if (message.equals("/createquiz")) {
                            commandCreateQuiz();
                        } else if (message.equals("/insertquiz")) {
                            sendMessage("Введите имя файла:");
                            String fileName = in.readUTF();
                            insertQuizFromFileToDb(fileName);
                        } else if (message.equals("/quizzes")) {
                            commandGetQuizzes();
                        } else if (message.startsWith("/quiz ")) {
                            commandStartQuiz(message);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }).start();
    }
    
    private void commandGetQuizzes() throws SQLException {
        this.server.getQuizProvider().quizListRequest(this);
    }

    private void commandStartQuiz(String msg) throws SQLException {
        if (takeQuizName(msg) == null) {
            sendMessage("Введите название викторины через пробел после команды /quiz quiz_name :");
        } else {
            this.server.getQuizProvider().startQuiz(takeQuizName(msg), this);
        }
    }

    private String takeQuizName(String str) {
        String[] part = str.split(" ", 2);
        if (part.length < 2) {
            return null;
        }
        return part[1].trim();
    }

    private void commandHelp() {
        sendMessage("""
                /reg – регистрация
                /auth – аутентификация
                /join quiz_name – подключиться к викторине
                /quizes – запросить список викторин
                /insertquiz - добавление викторины из ранее созданного файла
                /createquiz - создание викторины (в том числе и файл с ней)
                /exit – выход (для клиента)
                """);
    }

    private void commandCreateQuiz() throws SQLException, IOException {
        try (QuizFileCreate quizFile = new QuizFileCreate()) {
            quizFile.fileWrite(in, out);
            new QuizCreateDb().saveQuizToDb(quizFile.getFile(), userId, out);
        }
    }

    private void insertQuizFromFileToDb(String name) throws SQLException {
        File file = new File("server/src/main/java/ru/otus/quiz/server/files/" + name + ".txt");
        if (!file.exists()) {
            sendMessage("Файла с таким именем не существует: " + name);
        } else if (file.exists()) {
            try (QuizCreateDb qc = new QuizCreateDb()) {
                qc.saveQuizToDb(file, userId, out);
            }
        } else {
            sendMessage("Ошибка");
        }
    }

    private String inputData(String str) throws IOException, SQLException {
        String input;
        while (true) {
            sendMessage(str);
            input = in.readUTF().trim();

            if (input.isEmpty()) {
                sendMessage("Поле не может быть пустым.");
            } else if (server.getAuthenticationProvider().doesNameExist(input)) {
                sendMessage("Имя занято.");
            } else {
                break;
            }
        }
        return input;
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.getClients().remove(username);

        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
