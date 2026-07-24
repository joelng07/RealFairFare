package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Launches FairFare's Java Core desktop interface. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new FairFareFrame().setVisible(true);
        });
    }
}
