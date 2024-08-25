package ru.otus.quiz.server.dbfiles.quizcreateintodb;

import ru.otus.quiz.server.dbfiles.QueriesToBd;

import java.io.*;
import java.sql.*;

public class QuizFileCreate implements AutoCloseable {
    private File file;
    private final Connection connection;

    public QuizFileCreate() throws SQLException {
        connection = DriverManager.getConnection(QueriesToBd.DATABASE_URL, "sazon", "sazon12345");
    }

    public File getFile() {
        return file;
    }

    public void fileWrite(DataInputStream in, DataOutputStream out) throws IOException {

        while (true) {
            out.writeUTF("Введите имя файла.");
            String fileName = "server/src/main/java/ru/otus/quiz/server/files/" + in.readUTF() + ".txt";
            file = new File(fileName);

            if (file.exists()) {
                out.writeUTF("Файл с таким именем уже существует. Попробуйте еще раз.");
            } else {
                break;
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

            while (true) {
                out.writeUTF("Введите название викторины:");
                String userInputQuizName = in.readUTF();

                if (ifQuizNameIsBusy(userInputQuizName)) {
                    out.writeUTF("Викторина с таким именем уже существует");
                } else {
                    writer.write(userInputQuizName + "\n");
                    break;
                }
            }

            int n = 1;
            while (n < 21) {
                out.writeUTF("Введите вопрос №" + n);
                writer.write(in.readUTF() + "\n");
                for (int i = 1; i < 5; i++) {
                    out.writeUTF("Введите вариант ответа №" + i);
                    writer.write(i + ". " + in.readUTF() + "\n");
                }
                out.writeUTF("Укажите правильный вариант ответа");

                int correct;
                while (true) {
                    try {
                        out.writeUTF("Укажите правильный вариант ответа (от 1 до 4):");
                        correct = Integer.parseInt(in.readUTF());

                        if (correct >= 1 && correct <= 4) {
                            writer.write(correct + "\n");
                            break;
                        } else {
                            out.writeUTF("Неправильный ввод. Вариант ответа должен быть от 1 до 4.");
                        }
                    } catch (NumberFormatException e) {
                        out.writeUTF("Неверный формат. Введите число от 1 до 4.");
                    }
                }

                n++;
            }

            out.writeUTF("Викторина создана.");
            System.out.println("Запись в файл завершена.");
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private boolean ifQuizNameIsBusy (String name) throws SQLException {
        try (PreparedStatement getNamePs = connection.prepareStatement(QueriesToBd.GET_QUIZ_NAME)) {
            ResultSet quizNameRs = getNamePs.executeQuery();

            while (quizNameRs.next()) {
                String quizName = quizNameRs.getString("quiz_name");

                if (name.equals(quizName)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("Соединение с базой данных закрыто.");
        }
    }
}
