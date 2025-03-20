package DAO;

import Model.Account;
import Util.ConnectionUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import Exception.DatabaseException;

/**
 * Data Access Object (DAO) for handling account-related database operations.
 * This class provides methods for creating and retrieving accounts from the database.
 * It also includes transaction management and error handling for database operations.
 */
public class AccountDAO {

    /**
     * Logger instance for logging information, warnings, and errors in the SocialMediaController class.
     * Utilizes SLF4J to log relevant actions, including method entries, business decisions, and error handling.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountDAO.class);

    /**
     * Constant to refer to the index of the account ID column in the result set.
     * This value is used when extracting the account ID from the generated keys after an account is created.
     */
    private static final int COLUMN_ACCOUNT_ID_INDEX = 1;

    /**
     * No-argument constructor for AccountDAO.
     * This constructor allows Spring or other frameworks to instantiate the class without passing arguments.
     */
    public AccountDAO() {}

    /**
     * Creates a new account in the database.
     * 
     * This method attempts to insert a new user into the database with the provided account details (username and password).
     * If the account is successfully created, the new account with the generated account ID is returned.
     * 
     * @param account The account object containing the user's information (username and password).
     * @return An Optional containing the newly created Account if successful, or Optional.empty() if the account could not be created.
     * @throws DatabaseException If there is an error during the database operation, including during transaction rollback.
     */
    public Optional<Account> createAccount(Account account) {
        LOGGER.info("Received request to add new user to the database - AccountDAO.createAccount(Account account) method");
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Account newAccount = null;
        ResultSet primaryKeyResultSet = null;

        try {
            connection = ConnectionUtil.getConnection();
            checkConnection(connection);
            
            setConnectionAutoCommitFalse(connection);

            String sql = "INSERT INTO account (username, password) VALUES (?, ?);";
            preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, account.getUsername());
            preparedStatement.setString(2, account.getPassword());
            preparedStatement.executeUpdate();

            connection.commit();

            primaryKeyResultSet = preparedStatement.getGeneratedKeys();
            if (primaryKeyResultSet.next()) {
                int generatedAccountId = (int) primaryKeyResultSet.getLong(COLUMN_ACCOUNT_ID_INDEX);
                newAccount = new Account(generatedAccountId, account.getUsername(), account.getPassword());
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            LOGGER.error("SQLState: {}, ErrorCode: {}. Error occurred during user account creation. Transaction rolled back - AccountDAO.createAccount(Account account) method.",
                        sqlState, errorCode, exception);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollBackException) {
                String rollBackSqlState = exception.getSQLState();
                int rollBackErrorCode = exception.getErrorCode();
                LOGGER.error("SQLState: {}, ErrorCode: {}. Error occurred during rollback transaction for user account creation - AccountDAO.createAccount(Account account) method.", 
                    rollBackSqlState, rollBackErrorCode, rollBackException);
                throw new DatabaseException("Transaction failed, rolled back, original error: " + exception.getMessage(), exception);
            } 
            throw new DatabaseException("Failed to create user account to the database - AccountDAO.createAccount(Account account) method.", exception);
        } finally {
            closeResources(connection, preparedStatement, primaryKeyResultSet);
        }
        
        if (newAccount != null) {
            LOGGER.info("Successfully created user: {} with id: {} - AccountDAO.createAccount(Account account) method", newAccount.getUsername(),
                newAccount.getAccount_id());
            return Optional.of(newAccount);
        }
        return Optional.empty();
        
    }

    /**
     * Retrieves all accounts from the database.
     * 
     * This method executes a query to fetch all user accounts from the database.
     * It returns a list of accounts if successful, or an empty list if no accounts are found.
     * 
     * @return An Optional containing a list of all accounts in the database, or Optional.empty() if no accounts are found.
     * @throws DatabaseException If an error occurs during the database operation.
     */
    public Optional<List<Account>> getAllAccounts() {
        LOGGER.info("Received request to get the list of all users - AccountDAO.getAllAccounts() method.");
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<Account> accounts = new ArrayList<>();
    
        try {
            connection = ConnectionUtil.getConnection();

            if (connection != null) {
                LOGGER.info("Database connection established. Auto-commit: {}", connection.getAutoCommit());
            } else {
                LOGGER.error("Failed to establish database connection.");
                return Optional.of(accounts);
            }
            
            String sql = "SELECT * FROM account;";
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
            
            while (resultSet.next()) {
                Account account = new Account(
                    resultSet.getInt("account_id"),
                    resultSet.getString("username"),
                    resultSet.getString("password")
                );
                accounts.add(account);
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            String message = String.format("SQLState: %s, ErrorCode: %d. Error occurred while getting the list of all users from the database. SQL Query: %s",
                    sqlState, errorCode, "SELECT * FROM account;");

            LOGGER.error(message, exception);
            throw new DatabaseException(message, exception);
        } finally {
            closeResources(connection, preparedStatement, resultSet);
        }
    
        LOGGER.info("Successfully got list of all users from the database - AccountDAO.getAllAccounts() method.");
        return Optional.of(accounts);
    }

    /**
     * Retrieves an account by its account ID.
     * 
     * @param accountId The ID of the account to retrieve.
     * @return An Optional containing the Account if found, or Optional.empty() if no account is found.
     * @throws DatabaseException If there is an error during the database operation.
     */
    public Optional<Account> getAccountById(int accountId) {
        LOGGER.info("Received request to get user by provided ID: {} - AccountDAO.getAccountById(int accountId) method.", accountId);
        if (accountId <= 0) {
            LOGGER.warn("Invalid account ID: {} - AccountDAO.getAccountById(int accountId) method.", accountId);
            return Optional.empty();
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = ConnectionUtil.getConnection();
            String sql = "SELECT * FROM account WHERE account_id = ?;";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, accountId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                Account account = new Account(accountId, username, password);
                LOGGER.info("Successfully got user: {} with ID: {} - AccountDAO.getAccountById(int accountId) method.", username, accountId);
                return Optional.of(account);
            } else {
                LOGGER.warn("Failed to find account with ID: {} - AccountDAO.getAccountById(int accountId) method, returning Optional.empty()", accountId);
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            String message = String.format("SQLState: %s, ErrorCode: %d. Error occurred while getting account with ID: %d. SQL Query: %s",
                    sqlState, errorCode, accountId, "SELECT * FROM account WHERE account_id = ?");

            LOGGER.error(message, exception);
            throw new DatabaseException("Failed to get account with provided ID from database - AccountDAO.getAccountById(int accountId) method.", exception);
        } finally {
            closeResources(connection, preparedStatement, resultSet);
        }
        return Optional.empty();
    }


    /**
     * Retrieves an account by its username and password.
     * 
     * @param account The account object containing the username and password.
     * @return An Optional containing the found Account if successful, or Optional.empty() if no account is found.
     * @throws DatabaseException If there is an error during the database operation.
     */
    public Optional<Account> getAccountByUsernameAndPassword(Account account) {
        LOGGER.info("Received request to get user: {} - AccountDAO.getAccountByUsernameAndPassword(Account account)", 
            account.getUsername());
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = ConnectionUtil.getConnection();
            String sql = "SELECT * FROM account WHERE username = ? AND password = ?;";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, account.getUsername());
            preparedStatement.setString(2, account.getPassword());
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int account_id = resultSet.getInt("account_id");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                Account foundAccount = new Account(account_id, username, password);
                return Optional.of(foundAccount);  // Return the account wrapped in Optional
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            String message = String.format("SQLState: %s, ErrorCode: %d. Error occurred while retrieving account for user: %s. SQL Query: %s",
                    sqlState, errorCode, account.getUsername(), "SELECT * FROM account WHERE username = ? AND password = ?");

            LOGGER.error(message, exception);
            throw new DatabaseException("Failed to get account with provided username: " + account.getUsername() + " and password", exception);
        } finally {
            closeResources(connection, preparedStatement, resultSet);
        }

        return Optional.empty();
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
            String message = String.format("SQLState: %s, ErrorCode: %d. Failed to close resources - AccountDAO.closeResources(Connection connection, PreparedStaement preparedStatement, ResultSet resultSet).",
                    sqlState, errorCode);

            LOGGER.error(message, closeException);
            throw new DatabaseException("Error while closing resources - AccountDAO.closeResources", closeException);
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
            LOGGER.info("Database connection established. Auto-commit: {} - AccountDAO.checkConnection(Connection connection)", autoCommit);
        } else {
            LOGGER.error("Failed to establish database connection - AccountDAO.checkConnection(Connection connection).");
        }
    } catch (SQLException exception) {
        String sqlState = exception.getSQLState();
        int errorCode = exception.getErrorCode();
        String message = String.format("SQLState: %s, ErrorCode: %d. Error while establishing database connection.",
                sqlState, errorCode);

        LOGGER.error(message, exception);
        throw new DatabaseException("Error while establishing database connection - AccountDAO.checkConnection", exception);
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
                LOGGER.info("Connection auto-commit set to: false - AccountDA0.setConnectionAutoCommitFalse(Connection connection)");
            }
        } catch (SQLException exception) {
            String sqlState = exception.getSQLState();
            int errorCode = exception.getErrorCode();
            String message = String.format("SQLState: %s, ErrorCode: %d. Error while setting connection auto-commit to false.",
                    sqlState, errorCode);
            LOGGER.error(message, exception);
            throw new DatabaseException("Error while setting connection auto-commit to false - AccountDAO.setConnectionAutoCommitFalse", exception);
        }
    }
}
