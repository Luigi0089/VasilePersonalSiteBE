package it.vasilepersonalsite.exception;

public class PasswordErrataException extends RuntimeException {

    public PasswordErrataException() {
        super("Password errata");
    }

  public PasswordErrataException(String message) {
    super(message);
  }
}
