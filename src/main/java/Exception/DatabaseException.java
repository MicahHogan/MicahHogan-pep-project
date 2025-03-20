package Exception;

public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable exception) {
        super(message, exception);
    }

}
