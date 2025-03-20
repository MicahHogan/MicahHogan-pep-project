package DAO;

import Model.Message;
import Util.ConnectionUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import Exception.DatabaseException;

/**
 * Data Access Object (DAO) for handling message-related database operations.
 * This class provides methods for creating, retrieving, updating, and deleting messages in the database.
 * It also includes transaction management and error handling for database operations.
 */
public class MessageDAO {

    /**
     * Logger instance for logging information, warnings, and errors in the SocialMediaController class.
     * Utilizes SLF4J to log relevant actions, including method entries, business decisions, and error handling.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageDAO.class);

    /**
     * Constant to refer to the index of the message ID column in the result set.
     * This value is used when extracting the message ID from the generated keys after a message is created.
     */
    private static final int COLUMN_MESSAGE_ID_INDEX = 1;

    /**
     * No-argument constructor for MessageDAO.
     * This constructor allows Spring or other frameworks to instantiate the class without passing arguments.
     */
    public MessageDAO() {}

    /**
     * Creates a new message in the database.
     * 
     * This method attempts to insert a new message into the database with the provided information
     * (posted_by, message_text, time_posted_epoch). If the message is successfully created,
     * the new message with the generated message ID is returned.
     * 
     * @param message The message object containing the message information.
     * @return An Optional containing the newly created Message if successful, or Optional.empty() if the message could not be created.
     * @throws DatabaseException If there is an error during the database operation, including during transaction rollback.
     */
    public Optional<Message> createMessage(Message message) {
        LOGGER.info("Received request to add new message to the database - MessageDAO.createMessage(Message message) method");
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet primaryKeyResultSet = null;
        Message createdMessage = null;

        try {
            connection = ConnectionUtil.getConnection();
            checkConnection(connection);
            
            setConnectionAutoCommitFalse(connection);

            String sql = "INSERT INTO message (posted_by, message_text, time_posted_epoch) VALUES (?, ?, ?);";
            preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, message.getPosted_by());
            preparedStatement.setString(2, message.getMessage_text());
            preparedStatement.setLong(3, message.getTime_posted_epoch());
            preparedStatement.executeUpdate();

            connection.commit();

            primaryKeyResultSet = preparedStatement.getGeneratedKeys();
            if (primaryKeyResultSet.next()) {
                int generatedMessageId = (int) primaryKeyResultSet.getLong(COLUMN_MESSAGE_ID_INDEX);
                createdMessage = new Message(generatedMessageId, message.getPosted_by(),
                    message.getMessage_text(), message.getTime_posted_epoch());
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            LOGGER.error("SQLState: {}, ErrorCode: {}. Error occurred during message creation. Transaction rolled back - MessageDAO.createMessage(Message message) method.",
                        sqlState, errorCode, exception);

            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollBackException) {
                String rollBackSqlState = rollBackException.getSQLState();
                int rollBackErrorCode = rollBackException.getErrorCode();
                LOGGER.error("SQLState: {}, ErrorCode: {}. Error occurred during rollback transaction for message creation - MessageDAO.createMessage(Message message) method.", 
                    rollBackSqlState, rollBackErrorCode, rollBackException);
                throw new DatabaseException("Transaction failed, rolled back, original error: " + exception.getMessage(), exception);
            }
            throw new DatabaseException("Failed to create message in the database - MessageDAO.createMessage(Message message) method.", exception);
        } finally {
            closeResources(connection, preparedStatement, primaryKeyResultSet);
        }
        
        if (createdMessage != null) {
            LOGGER.info("Successfully created message with id: {} posted by user id: {} - MessageDAO.createMessage(Message message) method", 
                createdMessage.getMessage_id(), createdMessage.getPosted_by());
            return Optional.of(createdMessage);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Retrieves all messages from the database.
     * 
     * This method executes a query to fetch all messages from the database.
     * It returns a list of messages if successful, or an empty list if no messages are found.
     * 
     * @return An Optional containing a list of all messages in the database.
     * @throws DatabaseException If an error occurs during the database operation.
     */
    public Optional<List<Message>> getAllMessages() {
        LOGGER.info("Received request to get the list of all messages - MessageDAO.getAllMessages() method.");
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<Message> messages = new ArrayList<>();
    
        try {
            connection = ConnectionUtil.getConnection();
            checkConnection(connection);
            
            String sql = "SELECT * FROM message;";
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            
            while (resultSet.next()) {
                Message message = new Message(
                    resultSet.getInt("message_id"),
                    resultSet.getInt("posted_by"),
                    resultSet.getString("message_text"),
                    resultSet.getLong("time_posted_epoch")
                );
                messages.add(message);
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            String message = String.format("SQLState: %s, ErrorCode: %d. Error occurred while getting the list of all messages from the database. SQL Query: %s",
                    sqlState, errorCode, "SELECT * FROM message;");

            LOGGER.error(message, exception);
            throw new DatabaseException(message, exception);
        } finally {
            closeResources(connection, preparedStatement, resultSet);
        }
    
        LOGGER.info("Successfully retrieved {} messages from the database - MessageDAO.getAllMessages() method.", messages.size());
        return Optional.of(messages);
    }

    /**
     * Retrieves a message by its message ID.
     * 
     * @param messageId The ID of the message to retrieve.
     * @return An Optional containing the Message if found, or Optional.empty() if no message is found.
     * @throws DatabaseException If there is an error during the database operation.
     */
    public Optional<Message> getMessageById(int messageId) {
        LOGGER.info("Received request to get message by provided ID: {} - MessageDAO.getMessageById(int messageId) method.", messageId);
        if (messageId <= 0) {
            LOGGER.warn("Invalid message ID: {} - MessageDAO.getMessageById(int messageId) method.", messageId);
            return Optional.empty();
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = ConnectionUtil.getConnection();
            checkConnection(connection);
            
            String sql = "SELECT * FROM message WHERE message_id = ?;";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, messageId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int postedBy = resultSet.getInt("posted_by");
                String messageText = resultSet.getString("message_text");
                long timePostedEpoch = resultSet.getLong("time_posted_epoch");
                Message message = new Message(messageId, postedBy, messageText, timePostedEpoch);
                
                LOGGER.info("Successfully got message with ID: {} posted by user ID: {} - MessageDAO.getMessageById(int messageId) method.", 
                    messageId, postedBy);
                return Optional.of(message);
            } else {
                LOGGER.warn("Failed to find message with ID: {} - MessageDAO.getMessageById(int messageId) method, returning Optional.empty()", messageId);
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            String message = String.format("SQLState: %s, ErrorCode: %d. Error occurred while getting message with ID: %d. SQL Query: %s",
                    sqlState, errorCode, messageId, "SELECT * FROM message WHERE message_id = ?");

            LOGGER.error(message, exception);
            throw new DatabaseException("Failed to get message with provided ID from database - MessageDAO.getMessageById(int messageId) method.", exception);
        } finally {
            closeResources(connection, preparedStatement, resultSet);
        }
        
        return Optional.empty();
    }

    /**
     * Deletes a message from the database by its message ID.
     * 
     * This method attempts to delete a message with the specified ID from the database.
     * 
     * @param messageId The ID of the message to delete.
     * @return true if the message was successfully deleted, false otherwise.
     * @throws DatabaseException If there is an error during the database operation, including during transaction rollback.
     */
    public boolean deleteMessageById(int messageId) {
        LOGGER.info("Received request to delete message with ID: {} - MessageDAO.deleteMessageById(int messageId) method.", messageId);
        if (messageId <= 0) {
            LOGGER.warn("Invalid message ID: {} - MessageDAO.deleteMessageById(int messageId) method.", messageId);
            return false;
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = ConnectionUtil.getConnection();
            checkConnection(connection);
            
            setConnectionAutoCommitFalse(connection);

            String sql = "DELETE FROM message WHERE message_id = ?;";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, messageId);
            int rowsAffected = preparedStatement.executeUpdate();

            connection.commit();
            
            boolean deleted = rowsAffected > 0;
            if (deleted) {
                LOGGER.info("Successfully deleted message with ID: {} - MessageDAO.deleteMessageById(int messageId) method.", messageId);
            } else {
                LOGGER.warn("No message found with ID: {} to delete - MessageDAO.deleteMessageById(int messageId) method.", messageId);
            }
            
            return deleted;
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            LOGGER.error("SQLState: {}, ErrorCode: {}. Error occurred during message deletion. Transaction rolled back - MessageDAO.deleteMessageById(int messageId) method.",
                        sqlState, errorCode, exception);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollBackException) {
                String rollBackSqlState = rollBackException.getSQLState();
                int rollBackErrorCode = rollBackException.getErrorCode();
                LOGGER.error("SQLState: {}, ErrorCode: {}. Error occurred during rollback transaction for message deletion - MessageDAO.deleteMessageById(int messageId) method.", 
                    rollBackSqlState, rollBackErrorCode, rollBackException);
                throw new DatabaseException("Transaction failed, rolled back, original error: " + exception.getMessage(), exception);
            } 
            throw new DatabaseException("Failed to delete message from the database - MessageDAO.deleteMessageById(int messageId) method.", exception);
        } finally {
            closeResources(connection, preparedStatement, null);
        }
    }

    /**
     * Updates a message in the database with new text.
     * 
     * This method attempts to update the text of a message in the database.
     * 
     * @param message The message object containing the ID, posted_by, and time_posted_epoch of the message to update.
     * @param updatedMessageText The new text to set for the message.
     * @return An Optional containing the updated Message if successful, or Optional.empty() if the message could not be updated.
     * @throws DatabaseException If there is an error during the database operation, including during transaction rollback.
     */
    public Optional<Message> updateMessageById(Message message, String updatedMessageText) {
        LOGGER.info("Received request to update message with ID: {} - MessageDAO.updateMessageById(Message message, String updatedMessageText) method.", 
            message.getMessage_id());
        
        Connection connection = null;
        PreparedStatement preparedStatement = null;
    
        try {
            connection = ConnectionUtil.getConnection();
            checkConnection(connection);
            
            setConnectionAutoCommitFalse(connection);
            
            String sql = "UPDATE message SET message_text = ? WHERE message_id = ?;";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, updatedMessageText);
            preparedStatement.setInt(2, message.getMessage_id());
            
            int rowsUpdated = preparedStatement.executeUpdate();

            connection.commit();
    
            if (rowsUpdated == 0) {
                LOGGER.warn("No message found with ID: {} to update - MessageDAO.updateMessageById(Message message, String updatedMessageText) method.", 
                    message.getMessage_id());
                return Optional.empty();
            }
    
            Message updatedMessage = new Message(
                message.getMessage_id(), 
                message.getPosted_by(),
                updatedMessageText, 
                message.getTime_posted_epoch()
            );
            
            LOGGER.info("Successfully updated message with ID: {} - MessageDAO.updateMessageById(Message message, String updatedMessageText) method.", 
                message.getMessage_id());
            return Optional.of(updatedMessage);
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            LOGGER.error("SQLState: {}, ErrorCode: {}. Error occurred during message update. Transaction rolled back - MessageDAO.updateMessageById(Message message, String updatedMessageText) method.",
                        sqlState, errorCode, exception);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollBackException) {
                String rollBackSqlState = rollBackException.getSQLState();
                int rollBackErrorCode = rollBackException.getErrorCode();
                LOGGER.error("SQLState: {}, ErrorCode: {}. Error occurred during rollback transaction for message update - MessageDAO.updateMessageById(Message message, String updatedMessageText) method.", 
                    rollBackSqlState, rollBackErrorCode, rollBackException);
                throw new DatabaseException("Transaction failed, rolled back, original error: " + exception.getMessage(), exception);
            } 
            throw new DatabaseException("Failed to update message in the database - MessageDAO.updateMessageById(Message message, String updatedMessageText) method.", exception);
        } finally {
            closeResources(connection, preparedStatement, null);
        }
    }

    /**
     * Retrieves all messages from a specific user by their account ID.
     * 
     * This method executes a query to fetch all messages posted by a specific user.
     * It returns a list of messages if successful, or an empty list if no messages are found.
     * 
     * @param accountId The ID of the user whose messages are to be retrieved.
     * @return An Optional containing a list of messages posted by the specified user.
     * @throws DatabaseException If an error occurs during the database operation.
     */
    public Optional<List<Message>> getAllMessagesByUser(int accountId) {
        LOGGER.info("Received request to get all messages from user with ID: {} - MessageDAO.getAllMessagesByUser(int accountId) method.", accountId);
        if (accountId <= 0) {
            LOGGER.warn("Invalid account ID: {} - MessageDAO.getAllMessagesByUser(int accountId) method.", accountId);
            return Optional.of(new ArrayList<>());
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<Message> messages = new ArrayList<>();
    
        try {
            connection = ConnectionUtil.getConnection();
            checkConnection(connection);
            
            String sql = "SELECT * FROM message WHERE posted_by = ?;";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, accountId);
            resultSet = preparedStatement.executeQuery();
            
            while (resultSet.next()) {
                Message message = new Message(
                    resultSet.getInt("message_id"),
                    resultSet.getInt("posted_by"),
                    resultSet.getString("message_text"),
                    resultSet.getLong("time_posted_epoch")
                );
                messages.add(message);
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            String message = String.format("SQLState: %s, ErrorCode: %d. Error occurred while getting messages for user with ID: %d. SQL Query: %s",
                    sqlState, errorCode, accountId, "SELECT * FROM message WHERE posted_by = ?");

            LOGGER.error(message, exception);
            throw new DatabaseException(message, exception);
        } finally {
            closeResources(connection, preparedStatement, resultSet);
        }
    
        LOGGER.info("Successfully retrieved {} messages for user with ID: {} - MessageDAO.getAllMessagesByUser(int accountId) method.", 
            messages.size(), accountId);
        return Optional.of(messages);
    }

    /**
     * Closes the database resources such as Connection, PreparedStatement, and ResultSet.
     * 
     * @param connection The database connection to be closed.
     * @param preparedStatement The prepared statement to be closed.
     * @param resultSet The result set to be closed.
     * @throws DatabaseException If an error occurs during the closing of the resources.
     */
    private void closeResources(Connection connection, PreparedStatement preparedStatement, ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
            LOGGER.info("Resources connection, preparedStatement and ResultSet closed; connection autocommit enabled.");
        } catch (SQLException closeException) {
            String sqlState = closeException.getSQLState();
            int errorCode = closeException.getErrorCode();
            String message = String.format("SQLState: %s, ErrorCode: %d. Failed to close resources - MessageDAO.closeResources(Connection connection, PreparedStatement preparedStatement, ResultSet resultSet).",
                    sqlState, errorCode);

            LOGGER.error(message, closeException);
            throw new DatabaseException("Error while closing resources - MessageDAO.closeResources", closeException);
        }
    }

    /**
     * Checks the database connection and logs relevant information.
     * 
     * @param connection The database connection to be checked.
     * @throws DatabaseException If an error occurs while checking the connection.
     */
    private void checkConnection(Connection connection) {
        try {
            if (connection != null) {
                boolean autoCommit = connection.getAutoCommit();
                LOGGER.info("Database connection established. Auto-commit: {} - MessageDAO.checkConnection(Connection connection)", autoCommit);
            } else {
                LOGGER.error("Failed to establish database connection - MessageDAO.checkConnection(Connection connection).");
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            String message = String.format("SQLState: %s, ErrorCode: %d. Error while establishing database connection.",
                    sqlState, errorCode);

            LOGGER.error(message, exception);
            throw new DatabaseException("Error while establishing database connection - MessageDAO.checkConnection", exception);
        }
    }

    /**
     * Sets the connection's auto-commit mode to false to ensure transaction management.
     * 
     * @param connection The connection object whose auto-commit mode is to be set.
     * @throws DatabaseException If an error occurs while setting the auto-commit to false.
     */
    private void setConnectionAutoCommitFalse(Connection connection) {
        try {
            boolean autoCommit = connection.getAutoCommit();
            if (autoCommit) {
                connection.setAutoCommit(false);
                LOGGER.info("Connection auto-commit set to: false - MessageDAO.setConnectionAutoCommitFalse(Connection connection)");
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            String message = String.format("SQLState: %s, ErrorCode: %d. Error while setting connection auto-commit to false.",
                    sqlState, errorCode);
            LOGGER.error(message, exception);
            throw new DatabaseException("Error while setting connection auto-commit to false - MessageDAO.setConnectionAutoCommitFalse", exception);
        }
    }
}

