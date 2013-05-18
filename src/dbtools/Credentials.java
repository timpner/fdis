package dbtools;

import control.DefaultController;
import fd.AbstractModel;
import java.io.Serializable;
import java.net.URL;

/**
 * <code>Credentials</code> represent user credentials for a database 
 * connection, including username, URL and JDBC driver.
 *
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.0
 */
public class Credentials extends AbstractModel implements Serializable {

    /**
     * Compiler-generated serial version identifier.
     */
    private static final long serialVersionUID = -28414086365987497L;
    /**
     * The {@link URL} of the database server.
     */
    private String url;
    /**
     * The JDBC driver.
     */
    private String driver;
    /**
     * The username of the database server.
     */
    private String user;
    /**
     * The password of the database server.
     */
    private String password;

    /**
     * Class constructor. All fields will be set to an empty string.
     */
    public Credentials() {
        setUrl("");
        setDriver("");
        setUser("");
        setPassword("");
    }

    /**
     * Class constructor specifying all fields.
     */
    public Credentials(String url, String driver, String user, String password) {
        setUrl(url);
        setDriver(driver);
        setUser(user);
        setPassword(password);
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the used URL to the database server. Fires an property change event,
     * so that the model changes are propagated to the according view.
     *
     * @param url the URL
     */
    public void setUrl(String url) {
        String oldUrl = this.url;
        this.url = url;

        firePropertyChange(
                DefaultController.ELEMENT_URL_PROPERTY,
                oldUrl, url);
    }

    /**
     * @return the driver
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Sets the used JDBC driver. Fires an property change event,
     * so that the model changes are propagated to the according view.
     * 
     * @param driver the JDBC driver
     */
    public void setDriver(String driver) {
        String oldDriver = this.driver;
        this.driver = driver;

        firePropertyChange(
                DefaultController.ELEMENT_DRIVER_PROPERTY,
                oldDriver, driver);
    }

    /**
     * @return the username
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the username to the database server. Fires an property change event,
     * so that the model changes are propagated to the according view.
     * 
     * @param user the username
     */
    public void setUser(String user) {
        String oldUser = this.user;
        this.user = user;

        firePropertyChange(
                DefaultController.ELEMENT_USER_PROPERTY,
                oldUser, user);
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password to the database server. Fires an property change event,
     * so that the model changes are propagated to the according view.
     *
     * @param password the password
     */
    public void setPassword(String password) {

        String oldPassword = this.password;
        this.password = password;

        firePropertyChange(
                DefaultController.ELEMENT_PASSWORD_PROPERTY,
                oldPassword, password);
    }
}
