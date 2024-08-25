package ru.otus.quiz.server;

import java.sql.SQLException;

public interface AuthenticationProvider extends AutoCloseable{

    boolean authenticate(ClientPart clientPart, String login, String password) throws SQLException;

    int getUserIdByUsername(String login) throws SQLException;

    void createUser(String login, String username, String password) throws SQLException;

    boolean doesNameExist(String name) throws SQLException;

    boolean isUserExists(String login, String username, String password) throws SQLException;
}
