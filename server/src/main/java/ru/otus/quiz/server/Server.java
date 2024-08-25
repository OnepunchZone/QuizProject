package ru.otus.quiz.server;

import ru.otus.quiz.server.dbfiles.quizstart.QuizStartQuizProvider;
import ru.otus.quiz.server.dbfiles.registrationandauthentication.UserDbAuthenticationProvider;


import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private int port;
    private Map<String, ClientPart> clients;
    private AuthenticationProvider authenticationProvider;
    private QuizProvider quizProvider;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public QuizProvider getQuizProvider() {
        return quizProvider;
    }

    public Map<String, ClientPart> getClients() {
        return clients;
    }

    public Server(int port) throws SQLException {
        this.port = port;
        this.clients = new HashMap<>();
        this.authenticationProvider = new UserDbAuthenticationProvider(this);
        this.quizProvider = new QuizStartQuizProvider(this);
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Сервер запущен. Порт : " + port);

            while (true) {
                Socket socket = server.accept();
                new ClientPart(this, socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean usernameIsBusy(String username) {
        return clients.containsKey(username);
    }
}
