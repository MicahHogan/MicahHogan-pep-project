package Service;

import Model.Account;
import DAO.AccountDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service class that handles business logic for Account-related operations.
 * This class serves as an intermediary between the controller layer and the data access layer,
 * implementing validation and business rules for account management.
 */
public class AccountService {

    /**
     * Logger instance for logging information, warnings, and errors in the SocialMediaController class.
     * Utilizes SLF4J to log relevant actions, including method entries, business decisions, and error handling.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountService.class); // Logger for AccountService

    /**
     * Data Access Object (DAO) for handling account-related database operations.
     */
    private AccountDAO accountDAO;

    /**
     * Default constructor that initializes a new AccountDAO instance.
     */
    public AccountService() {
        accountDAO = new AccountDAO();
    }

    /**
     * Constructor that accepts a pre-configured AccountDAO instance.
     * Useful for dependency injection, particularly in testing scenarios.
     * 
     * @param accountDAO The AccountDAO instance to be used by this service
     */
    public AccountService(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    /**
     * Creates a new account after performing validation checks.
     * Account creation will fail if:
     * - The account object is null
     * - The username is blank
     * - The password is less than 4 characters long
     * - An account with the same username and password already exists
     * 
     * @param account The Account object containing the details of the account to be created
     * @return An Optional containing the created Account if successful, or an empty Optional if validation fails
     */
    public Optional<Account> createAccount(Account account) {
        LOGGER.info("Received request to create a new account - AccountService.createAccount(Account account) method");

        // Check if the account is null
        if (account == null) {
            LOGGER.warn("Account object is null. Account creation failed - AccountService.createAccount(Account account) method");
            return Optional.empty();
        }

        // Check if username is blank or password is too short
        if (account.getUsername().isBlank()) {
            LOGGER.warn("Username is blank. Account creation failed - AccountService.createAccount(Account account) method");
            return Optional.empty();
        }

        if (account.getPassword().length() < 4) {
            LOGGER.warn("Password is too short. It must be at least 4 characters. Account creation failed - AccountService.createAccount(Account account) method");
            return Optional.empty();
        }

        // Check if an account with the same username and password already exists
        if (accountDAO.getAccountByUsernameAndPassword(account).isPresent()) {
            LOGGER.warn("An account with the same username and password already exists. Account creation failed - AccountService.createAccount(Account account) method");
            return Optional.empty();
        }

        // Attempt to create the account
        Optional<Account> createdAccountOptional = accountDAO.createAccount(account);
        if (createdAccountOptional.isEmpty()) {
            LOGGER.warn("Account creation failed in DAO layer for account: {} - AccountService.createAccount(Account account) method", account.getUsername());
        }

        LOGGER.info("Successfully created account with username: {} - AccountService.createAccount(Account account) method", account.getUsername());
        return createdAccountOptional;
    }

    /**
     * Attempts to authenticate an account using the provided credentials.
     * 
     * @param account The Account object containing the username and password to authenticate
     * @return An Optional containing the authenticated Account if successful, or an empty Optional if authentication fails
     */
    public Optional<Account> loginAccount(Account account) {
        LOGGER.info("Received request to authenticate account - AccountService.loginAccount(Account account) method");

        // Check if the account is null
        if (account == null) {
            LOGGER.warn("Account object is null. Authentication failed - AccountService.loginAccount(Account account) method");
            return Optional.empty();
        }

        Optional<Account> loggedInAccountOptional = accountDAO.getAccountByUsernameAndPassword(account);
        
        if (loggedInAccountOptional.isEmpty()) {
            LOGGER.warn("Authentication failed for account with username: {} - AccountService.loginAccount(Account account) method", account.getUsername());
        } else {
            LOGGER.info("Successfully authenticated account with username: {} - AccountService.loginAccount(Account account) method", account.getUsername());
        }
        
        return loggedInAccountOptional;
    }

    /**
     * Retrieves an account by its unique identifier.
     * 
     * @param accountId The ID of the account to retrieve
     * @return An Optional containing the Account if found, or an empty Optional if no account exists with the given ID
     */
    public Optional<Account> getUserById(int accountId) {
        LOGGER.info("Received request to retrieve account with ID: {} - AccountService.getUserById() method", accountId);
        Optional<Account> account = accountDAO.getAccountById(accountId);
        if (account.isEmpty()) {
            LOGGER.warn("No account found with ID: {} - AccountService.getUserById() method", accountId);
        }
        LOGGER.info("Exiting getUserById method");
        return account;
    }

    /**
     * Retrieves all accounts in the system.
     * 
     * @return An Optional containing a List of all Accounts if successful, or an empty Optional if retrieval fails
     */
    public Optional<List<Account>> getAllAccounts() {
        LOGGER.info("Received request to retrieve all accounts - AccountService.getAllAccounts() method");
        Optional<List<Account>> accounts = accountDAO.getAllAccounts();
        if (accounts.isEmpty()) {
            LOGGER.warn("No accounts found - AccountService.getAllAccounts() method");
        }
        LOGGER.info("Exiting getAllAccounts method");
        return accounts;
    }
}

