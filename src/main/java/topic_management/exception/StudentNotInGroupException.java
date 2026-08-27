package topic_management.exception;

public class StudentNotInGroupException extends RuntimeException {
    public StudentNotInGroupException(String message) {
        super(message);
    }
}
