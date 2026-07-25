package app;

import javafx.application.Application;

/** JavaFX entry point for the FairFare desktop application. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        Application.launch(FairFareApplication.class, args);
    }
}
