package backend.academy.linktracker.bot.exception;

public class InvalidLinkUpdateException extends RuntimeException {

    public InvalidLinkUpdateException(String message) {
        super(message);
    }
}
