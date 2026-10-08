package persistence;

/** Signals that FairFare could not read or write its configured database. */
public final class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) { super(message, cause); }
}
