package control;

import fd.AbstractModel;
import gui.IView;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * <code>AbstractController</code> is the abstract super class for all
 * possible MVC controller classes.
 * <p>
 * It provides two <code>ArrayList</code>
 * objects, which are used to keep track of the registered models and views.
 * <p>
 * Upon registering a model, the controller also registers itself as a
 * property change listener on the model. Thus, the <code>propertyChange</code>
 * method is called whenever a model changes. Then, the controller will pass
 * this event on to the appropriate (registered) view.
 *
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.0
 */
public abstract class AbstractController implements PropertyChangeListener {

    /**
     * The registered MVC views.
     */
    private ArrayList<IView> registeredViews;
    /**
     * The registered MVC models.
     */
    private ArrayList<AbstractModel> registeredModels;

    /**
     * Sole constructor, initializing <code>registeredViews</code> and
     * <code>registeredModels</code>.
     */
    public AbstractController() {
        registeredViews = new ArrayList<IView>();
        registeredModels = new ArrayList<AbstractModel>();
    }

    /**
     * Registers a new model.
     *
     * @param model a class extending the <code>AbstractModel</code> class.
     */
    public void addModel(AbstractModel model) {
        registeredModels.add(model);
        model.addPropertyChangeListener(this);
    }

    /**
     * Removes a model from the list of objects, that are being kept track of.
     * 
     * @param model the model to remove
     */
    public void removeModel(AbstractModel model) {
        registeredModels.remove(model);
        model.removePropertyChangeListener(this);
    }

    /**
     * Registers a new view.
     *
     * @param view a class implementing the <code>IView</code> interface.
     */
    public void addView(IView view) {
        registeredViews.add(view);
    }

    /**
     * Removes a view from the list of objects, that are being kept track of.
     *
     * @param view the view to remove
     */
    public void removeView(IView view) {
        registeredViews.remove(view);
    }

    /**
     * Oberserves property changes from registered models and propagates them 
     * on to all the views.
     *
     * @param evt an event of changing a model's state.
     */
    public void propertyChange(PropertyChangeEvent evt) {

        for (IView view : registeredViews) {
            view.modelPropertyChange(evt);
        }
    }

    /**
     * This is a convenience method that subclasses can call upon
     * to fire property changes back to the models. This method
     * uses reflection to inspect each of the model classes
     * to determine whether it is the owner of the property
     * in question. If it isn't, a <code>NoSuchMethodException</code> is thrown,
     * which the method ignores.
     *
     * @param propertyName The name of the property.
     * @param newValue An object that represents the new value
     * of the property.
     */
    protected void setModelProperty(String propertyName, Object newValue) {
        for (AbstractModel model : registeredModels) {
            try {

                Method method = model.getClass().
                        getMethod("set" + propertyName, new Class[]{
                            newValue.getClass()
                        });
                method.invoke(model, newValue);

            } catch (Exception ex) {
                // IGNORE NoSuchMethodException.
                /*Logger.getLogger(
                AbstractController.class.getName()).log(
                Level.SEVERE, null, ex);
                System.err.println(ex.getMessage());*/
            }
        }
    }
}

