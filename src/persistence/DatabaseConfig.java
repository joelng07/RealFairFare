package persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves FairFare's self-contained SQLite database in the current user's app-data directory. */
public record DatabaseConfig(String url, Path file) {
    public static DatabaseConfig local() {
        String appData = System.getenv("LOCALAPPDATA");
        Path base = appData == null || appData.isBlank()
                ? Path.of(System.getProperty("user.home"), ".fairfare")
                : Path.of(appData, "FairFare");
        try { Files.createDirectories(base); }
        catch (IOException exception) { throw new DatabaseException("Unable to create FairFare's local data folder.", exception); }
        Path database = base.resolve("fairfare.db");
        return new DatabaseConfig("jdbc:sqlite:" + database.toAbsolutePath(), database);
    }
}
