package gui;

import java.beans.PropertyChangeEvent;

/**
 * <code>IView</code> is the interface that all views have to implement in
 * order to register for listening on model state changes.
 *
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.0
 */
public interface IView {

    /**
     * The <code>modelPropertyChange</code> method is called by the
     * controller whenever the model reports a state change.
     * 
     * @param evt an event created when a property changes.
     */
    public void modelPropertyChange(final PropertyChangeEvent evt);
}
