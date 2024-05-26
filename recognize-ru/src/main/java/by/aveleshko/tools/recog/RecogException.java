package by.aveleshko.tools.recog;

class RecogException extends RuntimeException {
    public RecogException(String errorMessage) {
        super(errorMessage);
    }
    public RecogException(String errorMessage, Throwable error) {
        super(errorMessage, error);
    }
}
