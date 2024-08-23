package ru.otus.quiz.server.dbfiles.quizcreateintodb;

import ru.otus.quiz.server.dbfiles.QueriesToBd;

import java.io.*;
import java.sql.*;

public class QuizCreateDb implements AutoCloseable{
    private final Connection connection;

    public QuizCreateDb() throws SQLException {
        connection = DriverManager.getConnection(QueriesToBd.DATABASE_URL, "sazon", "sazon12345");
    }

    public void saveQuizToDb(File file, int userId, DataOutputStream out) throws SQLException {
        connection.setAutoCommit(false);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String quizName = reader.readLine();

            if (ifQuizNameIsBusy(quizName)) {
                out.writeUTF("Викторина с таким именем уже существует");
                return;
            }

            int quizId;
            try (PreparedStatement quizSt = connection.prepareStatement(QueriesToBd.INSERT_QUIZ_SQL)) {
                quizSt.setString(1, quizName);
                quizSt.setInt(2, userId);
                ResultSet rs = quizSt.executeQuery();
                rs.next();
                quizId = rs.getInt(1);
            }

            String questionText;
            while ((questionText = reader.readLine()) != null) {
                int questionId;

                try (PreparedStatement questionSt = connection.prepareStatement(QueriesToBd.INSERT_QUESTION_SQL)) {
                    questionSt.setInt(1, quizId);
                    questionSt.setString(2, questionText);
                    questionSt.setInt(3, 1);

                    try (ResultSet rs = questionSt.executeQuery()) {
                        if (rs.next()) {
                            questionId = rs.getInt(1);
                        } else {
                            throw new SQLException("Ошибка вставки вопроса.");
                        }
                    }
                }

                for (int i = 1; i <= 4; i++) {
                    String answerText = reader.readLine();

                    try (PreparedStatement answerSt = connection.prepareStatement(QueriesToBd.INSERT_ANSWER_SQL)) {
                        answerSt.setInt(1, questionId);
                        answerSt.setString(2, answerText);
                        answerSt.executeUpdate();
                    }
                }

                int correctAnswer = Integer.parseInt(reader.readLine());
                try (PreparedStatement correctPs = connection.prepareStatement(QueriesToBd.UPDATE_CORRECT_ANSWER)) {
                    correctPs.setInt(1, correctAnswer);
                    correctPs.setInt(2, questionId);
                    correctPs.executeUpdate();
                }
            }

            connection.commit();
            System.out.println("Викторина успешно сохранена в базе данных.");
            out.writeUTF("Викторина добавлена в базу.");
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            connection.rollback();
        } finally {
            connection.setAutoCommit(true);
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
