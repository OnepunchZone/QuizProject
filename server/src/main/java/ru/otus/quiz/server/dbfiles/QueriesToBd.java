package ru.otus.quiz.server.dbfiles;

public class QueriesToBd {
    public static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/chat_db";
    public static final String LOGIN_USERNAME_PASSWORD_QUERY = "SELECT login, username, password FROM users";
    public static final String CREATE_USER_QUERY = "INSERT INTO users (login, username, password) " +
            "VALUES (?, ?, ?)";
    public static final String GET_QUIZ_NAME = "SELECT quiz_name FROM quizzes";
    public static final String INSERT_QUIZ_SQL = "INSERT INTO quizzes (quiz_name, owner_name) VALUES (?, ?)" +
            " RETURNING quiz_id";
    public static final String INSERT_QUESTION_SQL = "INSERT INTO questions (quiz_id, question_text, correct_answer)" +
            " VALUES (?, ?, ?) RETURNING question_id";
    public static final String INSERT_ANSWER_SQL = "INSERT INTO answers (question_id, answer_text) VALUES (?, ?)";
    public static final String IS_USER_EXIST = "SELECT COUNT(*) FROM users WHERE login = ? AND password = ?" +
            " OR username = ?";
    public static final String GET_USER_ID_BY_USERNAME = "SELECT id FROM users WHERE username = ?";
    public static final String UPDATE_CORRECT_ANSWER = "UPDATE questions SET correct_answer = ? WHERE question_id = ?";
    public static final String GET_QUIZ = "SELECT q.quiz_name, q.rating, u.username AS owner_name " +
            "FROM quizzes q JOIN users u ON q.owner_name = u.id";
    public static final String GET_QUIZ_ID_BY_Q_NAME = "SELECT quiz_id FROM quizzes WHERE quiz_name = ?";
    public static final String GET_QUESTIONS = "SELECT question_id, question_text FROM questions WHERE quiz_id = ?";
    public static final String GET_ANSWERS = "SELECT answer_text FROM answers WHERE question_id = ?";
    public static final String GET_CORRECT_ANSWER = "SELECT correct_answer FROM questions WHERE question_id = ?";
    public static final String INSERT_QUIZ_RATING = "UPDATE quizzes SET rating = ? WHERE quiz_name = ?";
    public static final String GET_QUIZ_RATING = "SELECT rating FROM quizzes WHERE quiz_name = ?";
    public static final String INSERT_RESULT = "INSERT INTO results (result_user_id, result_quiz_id, score) " +
            "VALUES (?, ?, ?)";
    public static final String SELECT_RESULT_BY_U_ID_AND_Q_ID = "SELECT COUNT(*) FROM results " +
            "WHERE result_user_id = ? AND result_quiz_id = ?";
    public static final String SELECT_USER_BY_NAME = "SELECT COUNT(*) FROM users WHERE username = ?";
}