package Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.UnauthorizedResponse;
import Model.Account;
import Model.Message;
import Service.AccountService;
import Service.MessageService;
import java.util.List;
import java.util.Optional;

/**
 * This class will create a Javalin API with eight functional endpoints and an example endpoint when the startAPI method is called.
 */
public class SocialMediaController {

    /**
     * Logger instance for logging information, warnings, and errors in the SocialMediaController class.
     * Utilizes SLF4J to log relevant actions, including method entries, business decisions, and error handling.
     */
    private static final Logger logger = LoggerFactory.getLogger(SocialMediaController.class);
    private AccountService accountService;
    private MessageService messageService;
    
    /**
     * No-argument constructor to support bean instantiation for Spring dependency injection.
     */
    public SocialMediaController() {
        this.accountService = new AccountService();
        this.messageService = new MessageService();
    }

    /**
     * Javalin application creation and definition of endpoints.
     * 
     * @return a Javalin app object which defines the behavior of the Javalin controller.
     */
    public Javalin startAPI() {
        logger.info("Initializing Social Media Blog API");
        Javalin app = Javalin.create(config -> {
        });
        
        configureExceptions(app);
        
        app.get("example-endpoint", this::exampleHandler);
        app.post("/register", this::postUserHandler);
        app.post("/login", this::postLoginHandler);
        app.post("/messages", this::postMessageHandler);
        app.get("/accounts", this::getAccountsHandler);
        app.get("/messages", this::getMessageHandler);
        app.get("/messages/{message_id}", this::getMessageByIdHandler);
        app.delete("/messages/{message_id}", this::deleteMessageByIdHandler);
        app.patch("/messages/{message_id}", this::patchMessageByIdHandler);
        app.get("/accounts/{account_id}/messages", this::getAllMessagesByUserHandler);
        
        logger.info("API initialized and ready to accept requests");
        return app;
    }

    /**
     * Configure exception handling for the application.
     * 
     * @param app The Javalin application instance.
     */
    private void configureExceptions(Javalin app) {
        app.exception(JsonProcessingException.class, (e, context) -> {
            logger.error("JsonProcessingException: {}", e.getMessage(), e);
            context.status(HttpStatus.BAD_REQUEST);
        });
        
        app.exception(NumberFormatException.class, (e, context) -> {
            logger.error("NumberFormatException - invalid ID format: {}", e.getMessage(), e);
            context.status(HttpStatus.BAD_REQUEST);
        });
        
        app.exception(BadRequestResponse.class, (e, context) -> {
            logger.error("Bad Request: {}", e.getMessage());
            context.status(HttpStatus.BAD_REQUEST);
        });
        
        app.exception(UnauthorizedResponse.class, (e, context) -> {
            logger.error("Unauthorized: {}", e.getMessage());
            context.status(HttpStatus.UNAUTHORIZED);
        });
        
        app.exception(NotFoundResponse.class, (e, context) -> {
            logger.error("Not Found: {}", e.getMessage());
            context.status(HttpStatus.NOT_FOUND);
            context.result("");
        });
        
        app.exception(Exception.class, (e, context) -> {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
        });
    }

    /**
     * Handler to register a new user at POST /register endpoint.
     * 
     * @param context The Javalin Context object provided by the Javalin app. It allows access to the HTTP request 
     *                and response, and contains the registered user account to be returned as a response.
     */
    private void postUserHandler(Context context) {
        logger.info("Received request to register a new user.");
        Account account = context.bodyAsClass(Account.class);
        Optional<Account> addedAccountOptional = accountService.createAccount(account);
        
        if (addedAccountOptional.isEmpty()) {
            throw new BadRequestResponse("User registration failed.");
        }
        
        Account addedAccount = addedAccountOptional.get();
        logger.info("Successfully registered user: {} with ID: {}.", addedAccount.getUsername(), 
            addedAccount.getAccount_id());
        context.json(addedAccount);
    }

    /**
     * Handler to login a user at POST /login endpoint.
     * 
     * @param context The Javalin Context object provided by the Javalin app. It allows access to the HTTP request 
     *                and response, and contains the logged-in user account to be returned as a response.
     */
    private void postLoginHandler(Context context) {
        logger.info("Received request to login a user.");
        Account account = context.bodyAsClass(Account.class);
        Account loginAccount = accountService.loginAccount(account).orElseThrow(() ->
        new UnauthorizedResponse("Login failed."));
        
        logger.info("Successfully logged in user: {} with ID: {}.", loginAccount.getUsername(), 
            loginAccount.getAccount_id());
        context.json(loginAccount);
    }

    /**
     * Handler to post a message at POST /messages endpoint.
     * 
     * @param context The Javalin Context object provided by the Javalin app. It allows access to the HTTP request 
     *                and response, and contains the posted message to be returned as a response.
     */
    private void postMessageHandler(Context context) {
        logger.info("Received a request to post a message.");
        Message message = context.bodyAsClass(Message.class);
        Optional<Message> postedMessageOptional = messageService.createMessage(message);
        
        Message postedMessage = postedMessageOptional.orElseThrow(() -> new BadRequestResponse("Message creation failed."));
        Optional<Account> messagePosterOptional = accountService.getUserById(postedMessage.getPosted_by());
        Account messagePoster = messagePosterOptional.get();
        logger.info("Successfully posted message with message ID: {} by user: {} with user ID: {}",
            postedMessage.getMessage_id(), messagePoster.getUsername(), messagePoster.getAccount_id());
        context.json(postedMessage);
    }

    /**
     * Handler to get a list of all messages at GET /messages endpoint.
     * 
     * @param context The Javalin Context object provided by the Javalin app. It allows access to the HTTP request 
     *                and response, and contains the list of all messages to be returned as a response.
     */
    private void getMessageHandler(Context context) {
        logger.info("Current directory: {}", System.getProperty("user.dir"));
        logger.info("Received request to get the list of all messages.");
        Optional<List<Message>> messagesOptional = messageService.getAllMessages();
        logger.info("Successfully got the list of all messages.");
        List<Message> messages = messagesOptional.get();
        context.json(messages);
    }

    /**
     * Handler to get a message by providing the message ID at GET /messages/{message_id} endpoint.
     * 
     * @param context The Javalin Context object provided by the Javalin app. It allows access to the HTTP request 
     *                and response, and contains the message with the specified ID to be returned as a response.
     */
    private void getMessageByIdHandler(Context context) {
        logger.info("Received request to get the message with ID: {}", context.pathParam("message_id"));
        int messageId = Integer.parseInt(context.pathParam("message_id"));
        Optional<Message> messageOptional = messageService.getMessageById(messageId);
        
        if (messageOptional.isEmpty()) {
            context.result("");
        } else {
            logger.info("Successfully got the message with ID: {}", messageId);
            Message message = messageOptional.get();
            context.json(message);
        }
    }

    /**
     * Handler to delete messages by providing the message ID at DELETE /messages/{message_id} endpoint.
     * 
     * @param context The Javalin Context object provided by the Javalin app. It allows access to the HTTP request 
     *                and response, and contains the deleted message to be returned as a response.
     */
    private void deleteMessageByIdHandler(Context context) {
        logger.info("Received request to delete the message with ID: {}", context.pathParam("message_id"));
        int messageId = Integer.parseInt(context.pathParam("message_id"));
        Optional<Message> deleteMessageOptional = messageService.getMessageById(messageId);
        Message deleteMessage = deleteMessageOptional.orElse(null);
        if (deleteMessage == null) {
            context.result("");
        } else {
            messageService.deleteMessageById(messageId);
            logger.info("Deleted message with ID: {}", messageId);
            context.json(deleteMessage);
        }
    }

    /**
     * Handler to update messages at PATCH /messages/{message_id} endpoint.
     * 
     * @param context The Javalin Context object provided by the Javalin app. It allows access to the HTTP request 
     *                and response, and contains the updated message to be returned as a response.
     */
    private void patchMessageByIdHandler(Context context) {
        logger.info("Received request to update the message with ID: {}", context.pathParam("message_id"));
        int messageId = Integer.parseInt(context.pathParam("message_id"));
        
        Optional<Message> existingMessageOptional = messageService.getMessageById(messageId);

        Message existingMessage = existingMessageOptional.orElseThrow(() -> 
            new BadRequestResponse("Update message failed - message with ID: " + messageId + "not found."));


        JsonNode requestBody = context.bodyAsClass(JsonNode.class);
        String newMessageText = requestBody.get("message_text").asText();
        
        Optional<Message> updatedMessageOptional = messageService.updateMessageById(messageId, newMessageText);
        
        Message updatedMessage = updatedMessageOptional.orElseThrow(() ->
            new BadRequestResponse("Message update failed: new message text is empty or greater than 255 characters."));
        
        String oldMessageText = existingMessage.getMessage_text();
        logger.info("Successfully updated previous message: {} with ID: {} with new message: {}.", 
            oldMessageText, messageId, newMessageText);
        context.json(updatedMessage);
    }

    /**
     * Handler to get all messages posted by a particular user at GET /accounts/{account_id}/messages endpoint.
     * 
     * @param context The Javalin Context object provided by the Javalin app. It allows access to the HTTP request 
     *                and response, and contains the list of messages posted by the user to be returned as a response.
     */
    private void getAllMessagesByUserHandler(Context context) {
        logger.info("Received request to get the list of all messages by user with ID: {}",
            context.pathParam("account_id"));
        int accountId = Integer.parseInt(context.pathParam("account_id"));
        Optional<List<Message>> messagesOptional = messageService.getAllMessagesByUser(accountId);
        
        if (messagesOptional.isEmpty()) {
            context.result("");
        } else {
            logger.info("Successfully got the list of all messages by user with ID: {}", context.pathParam("account_id"));
            List<Message> messages = messagesOptional.get();
            context.json(messages);
        }
    }

    /**
     * Handler to get a list of all accounts at GET /accounts endpoint.
     * 
     * @param context The Javalin Context object provided by the Javalin app. It allows access to the HTTP request 
     *                and response, and contains the list of all accounts to be returned as a response.
     */
    private void getAccountsHandler(Context context) {
        logger.info("Received request to get the list of all users.");
        Optional<List<Account>> accountsOptional = accountService.getAllAccounts();
        if (accountsOptional.isEmpty()) {
            context.result("");
        } else {
            logger.info("Successfully got the list of all accounts.");
            List<Account> accounts = accountsOptional.get();
            context.json(accounts);
        }
    }

    /**
     * Example handler for an example endpoint.
     * 
     * @param context The Javalin Context object provided by the Javalin app. It allows access to the HTTP request 
     *                and response, and contains the list of all accounts to be returned as a response.
     */
    private void exampleHandler(Context context) {
        context.json("sample text");
    }
    
}
