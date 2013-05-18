package control;

import dbtools.ConnectionManager;
import gui.FDISView;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 *
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.1
 */
public class FDISApp extends SingleFrameApplication {

    /** The global <code>logger</code> object. */
    private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    /**
     * A MVC controller, that propagates GUI events to the models and model
     * changes to the according views, respectively.
     */
    private static DefaultController controller;

    /**
     * At startup create and show the main frame of the application.
     */
    @Override
    protected void startup() {
        show(new FDISView(this, controller));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override
    protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of FDISApp
     */
    public static FDISApp getApplication() {
        return Application.getInstance(FDISApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        initialize();
        launch(FDISApp.class, args);
    }

    /**
     * Initializes the MVC controller and adds a shutdown hook to make sure that
     * open database connections will be closed even if the application ends
     * unexpectedly.
     */
    public static void initialize() {
        String fs = System.getProperty("file.separator");
        String dir = System.getProperty("user.dir");

        /*
         * Register a file handler in order to write a log file to the hard
         * disk.
         */
        Handler fh = null;
        try {
            fh = new FileHandler(dir + fs + "fdis.log");
        } catch (IOException ex) {
            logger.logp(Level.SEVERE, FDISApp.class.getName(), "initialize",
                    "Couldn't write log file.", ex);
        } catch (SecurityException ex) {
            logger.logp(Level.SEVERE, FDISApp.class.getName(), "initialize",
                    "Not allowed to write log file.", ex);
        }
        logger.addHandler(fh);

        controller = new DefaultController();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            public void run() {
                ConnectionManager.closeConnection();
            }
        }));
    }
}
