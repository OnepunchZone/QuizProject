package ru.otus.quiz.server.dbfiles.registrationandauthentication;

import ru.otus.quiz.server.AuthenticationProvider;
import ru.otus.quiz.server.ClientPart;
import ru.otus.quiz.server.Server;
import ru.otus.quiz.server.dbfiles.QueriesToBd;

import java.sql.*;

public class UserDbAuthenticationProvider implements AuthenticationProvider, AutoCloseable {
    private final Connection connection;
    private final Statement statement;
    private Server server;

    public UserDbAuthenticationProvider(Server server) throws SQLException {
        this.server = server;
        connection = DriverManager.getConnection(QueriesToBd.DATABASE_URL, "sazon", "sazon12345");
        statement = connection.createStatement();
    }

    @Override
    public boolean authenticate(ClientPart clientPart, String login, String password) {
        String authName = getUsernameByLogAndPass(login, password);

        if (authName == null) {
            clientPart.sendMessage("Некорректный логин/пароль");
            return false;
        }
        if (server.usernameIsBusy(authName)) {
            clientPart.sendMessage("Данное имя занято.");
            return false;
        }

        clientPart.setUsername(authName);
        clientPart.sendMessage("/correctauth " + authName);
        return true;
    }

    private String getUsernameByLogAndPass(String login, String password) {
        try (ResultSet rs = statement.executeQuery(QueriesToBd.LOGIN_USERNAME_PASSWORD_QUERY)) {
            while (rs.next()) {
                String log = rs.getString(1);
                String pass = rs.getString(3);

                if (log.equals(login) && pass.equals(password)) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int getUserIdByUsername(String name) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(QueriesToBd.GET_USER_ID_BY_USERNAME)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Пользователь с логином " + name + " не найден.");
                }
            }
        }
    }

    @Override
    public void createUser(String login, String username, String password) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(QueriesToBd.CREATE_USER_QUERY);

        ps.setString(1, login);
        ps.setString(2, username);
        ps.setString(3, password);
        ps.executeUpdate();

        System.out.println("В БД добавлен пользователь: " + login);
    }

    @Override
    public boolean doesNameExist(String name) throws SQLException {

        try (PreparedStatement ps = connection.prepareStatement(QueriesToBd.SELECT_USER_BY_NAME)) {
            ps.setString(1, name);

            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isUserExists(String login, String username, String password) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(QueriesToBd.IS_USER_EXIST);

        ps.setString(1, login);
        ps.setString(2, password);
        ps.setString(3, username);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        }

        return false;
    }

    @Override
    public void close() throws Exception {
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
