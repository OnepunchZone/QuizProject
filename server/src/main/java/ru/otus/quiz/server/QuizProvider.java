package ru.otus.quiz.server;

import java.sql.SQLException;

public interface QuizProvider {
    void quizListRequest(ClientPart client) throws SQLException;
    void startQuiz(String quizName, ClientPart client) throws SQLException;
}
