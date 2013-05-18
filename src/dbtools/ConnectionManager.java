package dbtools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * <code>ConnectionManager</code> class is responsible for providing a
 * database connection for other classes to use.
 * <p>
 * With the specified credentials a single <code>Connection</code> object
 * is created, that can be used by other classes for database operations. A
 * single instance is sufficient, as the information system neither supports
 * multi-user operations, nor are concurrent database accesses neccessary.
 * <p>
 * In addition, the <code>ConnectionManager</code> provides methods for
 * serializing and deserializing credentials for a more comfortable user
 * experience.
 *
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.0
 */
public class ConnectionManager {

    /**
     * The credentials for the database connection.
     */
    private static Credentials _cred;
    /**
     * The only {@link Connection} instance, according to the Singleton pattern.
     */
    private static Connection _theConnection;
    /** The global <code>logger</code> object. */
    private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * Sole constructor.
     */
    public ConnectionManager() {
    }

    /**
     * Opens and returns a database connection. As there is no multi-user
     * support, only one single instance of a connection is needed. According
     * to the Singleton pattern, a new connection is only opened if there was no 
     * open connection. Otherwise the <code>_theConnection</code> is returned.
     *
     * @return the database connection
     */
    public static Connection getConnection() {
        if (_theConnection == null) {
            try {
                /* Registers the JDBC _driver for the database. */
                Class.forName(getCred().getDriver());
            } catch (java.lang.ClassNotFoundException e) {
                logger.logp(Level.SEVERE, ConnectionManager.class.getName(),
                        "getConnection",
                        "Couldn't load the JDBC driver.", e);
            }

            try {
                /* Establishes a connection to the database. */
                _theConnection = DriverManager.getConnection(
                        getCred().getUrl(),
                        getCred().getUser(),
                        getCred().getPassword());

                if (!_theConnection.isClosed()) {
                    /*System.out.println("Successfully connected to " +
                    "database server!");*/
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "SQL-Exception: Couldn't connect to database server.",
                        "Error Message", JOptionPane.ERROR_MESSAGE);
                logger.logp(Level.SEVERE, ConnectionManager.class.getName(),
                        "getConnection",
                        "Couldn't connect to database server.", e);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Verbindung zu Datenbank-Server konnte nicht " +
                        "hergestellt werden.",
                        "Error Message", JOptionPane.ERROR_MESSAGE);
                logger.logp(Level.SEVERE, ConnectionManager.class.getName(),
                        "getConnection",
                        "Couldn't connect to database server.", ex);
            }
        }
        return _theConnection;
    }

    /**
     * Closes the connection to a database server. If there is no open
     * connection upon being called, the <code>closeConnection</code> method
     * will do no harm.
     */
    public static void closeConnection() {
        try {
            if (_theConnection != null) {
                _theConnection.close();
                _theConnection = null;
            //System.out.println("Database connection successfully closed.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Keine Verbindung zum Datenbank-Server zu schließen.",
                    "Error Message", JOptionPane.ERROR_MESSAGE);
            logger.logp(Level.SEVERE, ConnectionManager.class.getName(),
                    "closeConnection",
                    "Couldn't close the connection.", e);
        }
    }

    /**
     * Serializes current <code>Credentials</code> object. For each stored
     * object a .ser file will be created in subdirectory "serialize" within
     * the user directory.
     *
     * @param filename the name of the .ser file.
     */
    public static void saveCred(String filename) {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;

        String fs = System.getProperty("file.separator");
        String dir = System.getProperty("user.dir");

        try {
            fos = new FileOutputStream(dir + fs + "serialize" +
                    fs + filename + ".ser");
            out = new ObjectOutputStream(fos);
            out.writeObject(getCred());
            out.close();
        } catch (IOException ex) {
            logger.logp(Level.SEVERE, ConnectionManager.class.getName(),
                    "saveCred",
                    "Couldn't write the credentials to the hard disk.", ex);
        }
    }

    /**
     * Loads a serialized <code>Credentials</code> object from the user
     * directory.
     * 
     * @param filename the name of the .ser file to load
     */
    public static void loadCred(String filename) {
        String fs = System.getProperty("file.separator");
        String dir = System.getProperty("user.dir");

        FileInputStream fis = null;
        ObjectInputStream in = null;
        Credentials cred = null;
        try {
            fis = new FileInputStream(dir + fs + "serialize" +
                    fs + filename + ".ser");
            in = new ObjectInputStream(fis);
            cred = (Credentials) in.readObject();
            in.close();
            setCred(cred);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            logger.logp(Level.SEVERE, ConnectionManager.class.getName(),
                    "loadCred",
                    "Couldn't read credentials from hard disk.", ex);
        }
    }

    /**
     * Returns the user credentials for the currently used connection.
     *
     * @return the connection credentials
     */
    public static Credentials getCred() {
        return _cred;
    }

    /**
     * Sets the credentials, that are used for opening a database connection.
     *
     * @param aCred the connection credentials
     */
    public static void setCred(Credentials aCred) {
        _cred = aCred;
    }
}
