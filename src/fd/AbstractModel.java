package fd;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * <code>AbstractModel</code> is the abstract super class for all MVC model
 * classes. It uses the <code>javax.beans.PropertyChangeSupport</code> class
 * to register, deregister, and notify interested listeners of changes to
 * the model.
 *
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.0
 */
public abstract class AbstractModel {

    /** The beans class for (de-)registering listeners. */
    protected PropertyChangeSupport propertyChangeSupport;

    /**
     * Sole constructor.
     */
    public AbstractModel() {
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Adds a <code>PropertyChangeListener</code> to the listener list.
     * The listener
     * is registered for all properties. The same listener object may be
     * added more than once, and will be called as many times as it is added.
     * If listener is null, no exception is thrown and no action is taken.
     *
     * @see java.beans.PropertyChangeSupport#addPropertyChangeListener(
     *                                                  PropertyChangeListener)
     * @param listener The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a <code>PropertyChangeListener</code> from the listener list.
     * This removes a <code>PropertyChangeListener</code> that was registered
     * for all properties. If listener was added more than once to the
     * same event source, it will be notified one less time after being
     * removed. If listener is null, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener(
     *                                                  PropertyChangeListener)
     * @param listener The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Report a bound property update to any registered listeners. No event 
     * is fired if old and new are equal and non-null.
     *
     * @see java.beans.PropertyChangeSupport#firePropertyChange(String,
     *                                                         Object, Object)
     * @param propertyName
     *                   The programmatic name of the property that was changed.
     * @param oldValue The old value of the property.
     * @param newValue The new value of the property.
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
            Object newValue) {
        propertyChangeSupport.firePropertyChange(
                propertyName, oldValue, newValue);
    }
}
