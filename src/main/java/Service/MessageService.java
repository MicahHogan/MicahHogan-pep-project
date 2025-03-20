package Service;

import Model.Message;
import DAO.MessageDAO;
import DAO.AccountDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

/**
 * Service class that handles business logic for Message-related operations.
 * This class serves as an intermediary between the controller layer and the data access layer,
 * implementing validation and business rules for message management in the social media application.
 * It ensures that messages comply with business rules before persistence operations are performed.
 */
public class MessageService {

    /**
     * Logger instance for logging information, warnings, and errors in the SocialMediaController class.
     * Utilizes SLF4J to log relevant actions, including method entries, business decisions, and error handling.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class); // Logger for MessageService

    /**
     * Data Access Object (DAO) for handling message-related database operations.
     */
    private MessageDAO messageDAO;

    /**
     * Data Access Object (DAO) for handling account-related database operations.
     */
    private AccountDAO accountDAO;

    /**
     * Default constructor that initializes new MessageDAO and AccountDAO instances.
     * Creates the necessary data access components for message operations.
     */
    public MessageService() {
        messageDAO =  new MessageDAO();
        accountDAO = new AccountDAO();
    }

    /**
     * Constructor that accepts pre-configured DAO instances.
     * Useful for dependency injection, particularly in testing scenarios where
     * mock objects might be used instead of actual database connections.
     * 
     * @param messageDAO The MessageDAO instance to be used for message operations
     * @param accountDAO The AccountDAO instance to be used for account validation
     */
    public MessageService(MessageDAO messageDAO, AccountDAO accountDAO) {
        this.messageDAO = messageDAO;
        this.accountDAO = accountDAO;
    }

    /**
     * Creates a new message after performing validation checks.
     * Message creation will fail if:
     * - The account that posted the message doesn't exist
     * - The message text is blank
     * - The message text exceeds 255 characters
     * 
     * @param message The Message object containing the details of the message to be created
     * @return An Optional containing the created Message if successful, or an empty Optional if validation fails
     */
    public Optional<Message> createMessage(Message message) {
        LOGGER.info("Received request to add new message to the database - MessageService.createMessage(Message message) method");

        if (accountDAO.getAccountById(message.getPosted_by()).isEmpty()) {
            LOGGER.warn("Account with ID {} does not exist. Message creation failed - MessageService.createMessage(Message message) method", message.getPosted_by());
            return Optional.empty();
        }

        if (message.getMessage_text().isBlank()) {
            LOGGER.warn("Message text is blank. Message creation failed - MessageService.createMessage(Message message) method");
            return Optional.empty();
        }

        if (message.getMessage_text().length() > 255) {
            LOGGER.warn("Message text exceeds 255 characters. Message creation failed - MessageService.createMessage(Message message) method");
            return Optional.empty();
        }

        Optional<Message> createdMessageOptional = messageDAO.createMessage(message);
        if (createdMessageOptional.isEmpty()) {
            LOGGER.warn("Message creation failed in DAO layer for message: {} - MessageService.createMessage(Message message) method", message);
            return Optional.empty();
        }

        LOGGER.info("Successfully created message with ID: {} posted by user with ID: {} - MessageService.createMessage(Message message) method",
                createdMessageOptional.get().getMessage_id(), message.getPosted_by());
        return createdMessageOptional;
    }

    /**
     * Retrieves all messages in the system.
     * 
     * @return An Optional containing a List of all Messages if successful, or an empty Optional if retrieval fails
     */
    public Optional<List<Message>> getAllMessages() {
        LOGGER.info("Received request to retrieve all messages - MessageService.getAllMessages() method");
        Optional<List<Message>> messages = messageDAO.getAllMessages();

        if (messages.isEmpty()) {
            LOGGER.warn("No messages found - MessageService.getAllMessages() method");
        }

        LOGGER.info("Exiting getAllMessages method");
        return messages;
    }

    /**
     * Retrieves a message by its unique identifier.
     * 
     * @param messageId The ID of the message to retrieve
     * @return An Optional containing the Message if found, or an empty Optional if no message exists with the given ID
     */
    public Optional<Message> getMessageById(int messageId) {
        LOGGER.info("Received request to retrieve message with ID: {} - MessageService.getMessageById() method", messageId);
        Optional<Message> message = messageDAO.getMessageById(messageId);

        if (message.isEmpty()) {
            LOGGER.warn("No message found with ID: {} - MessageService.getMessageById() method", messageId);
        }

        LOGGER.info("Exiting getMessageById method");
        return message;
    }

    /**
     * Deletes a message by its unique identifier.
     * 
     * @param messageId The ID of the message to delete
     */
    public void deleteMessageById(int messageId) {
        LOGGER.info("Received request to delete message with ID: {} - MessageService.deleteMessageById() method", messageId);

        messageDAO.deleteMessageById(messageId);

        LOGGER.info("Successfully deleted message with ID: {} - MessageService.deleteMessageById() method", messageId);
    }

    /**
     * Updates the text of an existing message after performing validation checks.
     * Message update will fail if:
     * - The message to update doesn't exist
     * - The updated message text is blank (after trimming)
     * - The updated message text exceeds 255 characters
     * 
     * @param messageId The ID of the message to update
     * @param updatedMessage The new text for the message
     * @return An Optional containing the updated Message if successful, or an empty Optional if validation fails
     */
    public Optional<Message> updateMessageById(int messageId, String updatedMessage) {
        LOGGER.info("Received request to update message with ID: {} - MessageService.updateMessageById() method", messageId);

        if (updatedMessage.trim().isBlank()) {
            LOGGER.warn("Updated message text is blank after trimming. Update failed - MessageService.updateMessageById() method");
            return Optional.empty();
        }

        if (updatedMessage.length() > 255) {
            LOGGER.warn("Updated message text exceeds 255 characters. Update failed - MessageService.updateMessageById() method");
            return Optional.empty();
        }

        Optional<Message> existingMessageOptional = messageDAO.getMessageById(messageId);

        if (existingMessageOptional.isEmpty()) {
            LOGGER.warn("No message found with ID: {}. Update failed - MessageService.updateMessageById() method", messageId);
            return Optional.empty();
        }

        Message existingMessage = existingMessageOptional.get();
        existingMessage.setMessage_text(updatedMessage);
        Optional<Message> updatedMessageOptional = messageDAO.updateMessageById(existingMessage, updatedMessage);

        if (updatedMessageOptional.isEmpty()) {
            LOGGER.warn("Failed to update message with ID: {} - MessageService.updateMessageById() method", messageId);
            return Optional.empty();
        }

        LOGGER.info("Successfully updated message with ID: {} - MessageService.updateMessageById() method", messageId);
        return updatedMessageOptional;
    }

    /**
     * Retrieves all messages posted by a specific user.
     * 
     * @param accountId The ID of the account whose messages should be retrieved
     * @return An Optional containing a List of Messages posted by the specified user if successful,
     *         or an empty Optional if retrieval fails or if the user has no messages
     */
    public Optional<List<Message>> getAllMessagesByUser(int accountId) {
        LOGGER.info("Received request to retrieve all messages for user with ID: {} - MessageService.getAllMessagesByUser() method", accountId);
        Optional<List<Message>> messages = messageDAO.getAllMessagesByUser(accountId);

        if (messages.isEmpty()) {
            LOGGER.warn("No messages found for user with ID: {} - MessageService.getAllMessagesByUser() method", accountId);
        }
        
        LOGGER.info("Exiting getAllMessagesByUser method");
        return messages;
    }
}

