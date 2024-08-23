package ru.otus.quiz.server;

import java.sql.SQLException;

public class ServerApp {
    public static void main(String[] args) throws SQLException {
        new Server(3232).start();
    }
}
