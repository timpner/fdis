package gui;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;

/**
 * <code>SchemaTreeCellRenderer</code> provides tree node icons, corresponding
 * to a database schema. The root node icon depicts a database, and all other
 * non-leaf node icons depict database tables.
 *
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.0
 */
public class SchemaTreeCellRenderer extends DefaultTreeCellRenderer {

    /** Compiler-generated serial version identifier. */
    private static final long serialVersionUID = 632309934518849826L;
    /** The icon used for non-root, non-leaf nodes. */
    private ImageIcon icon = createImageIcon("tableIcon");
    /** The icon used for the root node. */
    private ImageIcon rootIcon = createImageIcon("databaseIcon");
    /** The global <code>logger</code> object. */
    private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
            Object value, boolean sel, boolean expanded, boolean isLeaf,
            int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded,
                isLeaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

        if (node.isRoot()) {
            if (rootIcon != null) {
                setIcon(rootIcon);
            }

        } else if (node.isLeaf()) {
            // use default icon
        } else {
            if (icon != null) {
                setIcon(icon);
            }
        }
        return this;
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     *
     * @param iconID the ID of the icon file.
     * @return the icon
     */
    protected ImageIcon createImageIcon(String iconID) {
        //String fs = System.getProperty("file.separator");

        ApplicationContext appContext = Application.getInstance().getContext();
        ResourceMap resourceMap = appContext.getResourceMap(FDISView.class);
        ImageIcon image = (ImageIcon) resourceMap.getIcon(iconID);

        // java.net.URL imgURL = SchemaTreeCellRenderer.class.getResource(
        //        "resources" + fs + path);
        if (image != null) {
            return image;//new ImageIcon(imgURL);
        } else {
            logger.logp(Level.SEVERE, SchemaTreeCellRenderer.class.getName(),
                    "createImageIcon",
                    "Couldn't find file " + iconID);
            return null;
        }
    }
}
