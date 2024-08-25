package ru.otus.quiz.server.dbfiles.quizstart;

import ru.otus.quiz.server.ClientPart;
import ru.otus.quiz.server.QuizProvider;
import ru.otus.quiz.server.Server;
import ru.otus.quiz.server.dbfiles.QueriesToBd;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.*;
import java.util.concurrent.*;

public class QuizStartQuizProvider implements QuizProvider, AutoCloseable {
    private final Connection connection;
    private Server server;
    private int correctAnswerCount = 0;

    public QuizStartQuizProvider(Server server) throws SQLException {
        connection = DriverManager.getConnection(QueriesToBd.DATABASE_URL, "sazon", "sazon12345");
        this.server = server;
    }

    @Override
    public void quizListRequest(ClientPart client) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(QueriesToBd.GET_QUIZ)) {
            ResultSet rs = stmt.executeQuery();
            StringBuilder quizList = new StringBuilder("Доступные викторины:\n");

            int count = 1;
            while (rs.next()) {
                String quizName = rs.getString("quiz_name");
                double quizRating = rs.getDouble("rating");
                String ownerName = rs.getString("owner_name");
                quizList.append(count).append(". ").append(quizName).append(". Рейтинг ")
                        .append(String.format("%.2f", quizRating)).append(". Владелец: ")
                        .append(ownerName).append("\n");
                count++;
            }

            client.sendMessage(quizList.toString());
        }
    }

    @Override
    public void startQuiz(String quizName, ClientPart client) throws SQLException {

        if (hasUserTakenQuiz(getUserId(client), getQuizId(quizName))) {
            client.sendMessage("Вы уже проходили эту викторину. Выберите другую.");
            return;
        }

        try (PreparedStatement quizIdPs = connection.prepareStatement(QueriesToBd.GET_QUIZ_ID_BY_Q_NAME)) {
            quizIdPs.setString(1, quizName);
            ResultSet quizIdRs = quizIdPs.executeQuery();

            if (quizIdRs.next()) {
                int quizId = quizIdRs.getInt("quiz_id");

                try (PreparedStatement questionsPs = connection.prepareStatement(QueriesToBd.GET_QUESTIONS)) {
                    questionsPs.setInt(1, quizId);
                    ResultSet rs = questionsPs.executeQuery();

                    int questionNumber = 1;
                    while (rs.next()) {
                        int questionId = rs.getInt("question_id");
                        String questionText = rs.getString("question_text");
                        client.sendMessage("Вопрос №" + questionNumber + ": " + questionText);
                        questionNumber ++;

                        try (PreparedStatement ansPs = connection.prepareStatement(QueriesToBd.GET_ANSWERS)) {
                            ansPs.setInt(1, questionId);
                            ResultSet ansRs = ansPs.executeQuery();
                            client.sendMessage("Варианты ответов:");

                            while (ansRs.next()) {
                                String answerText = ansRs.getString("answer_text");
                                client.sendMessage(answerText);
                            }
                        }

                        boolean flag = false;
                        String clientAnswer = null;
                        try {
                            clientAnswer = getAnswer(client);
                            flag = true;
                        } catch (TimeoutException e) {
                            client.sendMessage("Время на ответ истекло.");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        if(flag) {
                            checkAnswer(questionId, clientAnswer, client);
                        }
                    }

                    client.sendMessage("Викторина завершена!");
                    quizRate(client, quizName);
                    recordQuizResult(client, quizId, quizName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    throw new RuntimeException(e);
                }
            } else {
                client.sendMessage("Викторина с таким именем не найдена.");
            }
        }
    }

    private String getAnswer(ClientPart client) throws TimeoutException, IOException {
        client.getSocket().setSoTimeout(20000);

        try {
            return client.getIn().readUTF();
        } catch (SocketTimeoutException e) {
            throw new TimeoutException("Время вышло.");
        } finally {
            client.getSocket().setSoTimeout(0);
        }
    }

    private void checkAnswer(int questionId, String clientAnswer, ClientPart client) throws SQLException, IOException, TimeoutException {
        try (PreparedStatement correctPs = connection.prepareStatement(QueriesToBd.GET_CORRECT_ANSWER)) {
            correctPs.setInt(1, questionId);
            ResultSet rs = correctPs.executeQuery();

            if (rs.next()) {
                int correct = rs.getInt("correct_answer");
                int attempts = 3;

                while (attempts > 0) {
                    try {
                        int clientAns = Integer.parseInt(clientAnswer);

                        if (clientAns < 1 || clientAns > 4) {
                            attempts --;
                            if (attempts > 0) {
                                client.sendMessage("Осталось попыток для корректного ввода: " + attempts +
                                        "\nОтвет должен быть числом от 1 до 4.");
                                clientAnswer = getAnswer(client);
                            } else {
                                client.sendMessage("Перехожу на следующий вопрос.");
                                break;
                            }

                        } else if (correct == clientAns) {
                            correctAnswerCount++;
                            client.sendMessage("Правильный ответ!");
                            break;
                        } else {
                            client.sendMessage("Ответ неверный =(");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        attempts --;
                        if (attempts > 0) {
                            client.sendMessage("Осталось попыток для корректного ввода: " + attempts +
                                    "\nОтвет должен быть числом от 1 до 4.");
                            clientAnswer = getAnswer(client);
                        } else {
                            client.sendMessage("Перехожу к следующему вопросу.");
                            break;
                        }
                    } catch (TimeoutException e) {
                        client.sendMessage("Время на ответ истекло.");
                        break;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void quizRate(ClientPart client, String quizName) throws SQLException {
        double clientRate;

        while (true) {
            client.sendMessage("Оцените викторину от 1 до 10:");

            try {
                clientRate = Integer.parseInt(client.getIn().readUTF());

                if (clientRate < 1 || clientRate > 10) {
                    client.sendMessage("Введите число от 1 до 10.");
                } else {
                    client.sendMessage("Спасибо за оценку!");
                    break;
                }
            } catch (NumberFormatException e) {
                client.sendMessage("Рейтинг должен быть числом от 1 до 10.");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (PreparedStatement getRatingPs = connection.prepareStatement(QueriesToBd.GET_QUIZ_RATING)) {
            getRatingPs.setString(1, quizName);
            ResultSet ratingRs = getRatingPs.executeQuery();

            while (ratingRs.next()) {
                double currentRating = ratingRs.getDouble("rating");

                double newRating;
                if (currentRating == 0) {
                    newRating = clientRate;
                } else {
                    newRating = (currentRating + clientRate)/2;
                }

                try (PreparedStatement setRatingPs = connection.prepareStatement(QueriesToBd.INSERT_QUIZ_RATING)){
                    setRatingPs.setDouble(1, newRating);
                    setRatingPs.setString(2, quizName);
                    setRatingPs.executeUpdate();
                }
            }
        }
    }

    private int getUserId(ClientPart client) throws SQLException {
        try (PreparedStatement getUserIdPs = connection.prepareStatement(QueriesToBd.GET_USER_ID_BY_USERNAME)) {
            getUserIdPs.setString(1, client.getUsername());

            try (ResultSet rs = getUserIdPs.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Пользователь с логином " + client.getUsername() + " не найден.");
                }
            }
        }
    }

    private int getQuizId(String quizName) throws SQLException {
        try (PreparedStatement quizIdPs = connection.prepareStatement(QueriesToBd.GET_QUIZ_ID_BY_Q_NAME)) {
            quizIdPs.setString(1, quizName);

            try (ResultSet rs = quizIdPs.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quiz_id");
                } else {
                    throw new SQLException("Викторина с именем " + quizName + " не найдена.");
                }
            }
        }
    }

    private void recordQuizResult(ClientPart client, int quizId, String quizName) throws SQLException {
        int userId = getUserId(client);

        try (PreparedStatement stmt = connection.prepareStatement(QueriesToBd.INSERT_RESULT)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, quizId);
            stmt.setInt(3, correctAnswerCount);
            stmt.executeUpdate();
        }
        System.out.println("Результат сохранен в бд.");

        client.sendMessage("РЕЗУЛЬТАТ :\n"
                + "Викторина: " + quizName + ". Правильных ответов: " + correctAnswerCount + " из 20.");
    }

    private boolean hasUserTakenQuiz(int userId, int quizId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(QueriesToBd.SELECT_RESULT_BY_U_ID_AND_Q_ID)) {
            ps.setInt(1, userId);
            ps.setInt(2, quizId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
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
