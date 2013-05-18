package gui;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import org.jdesktop.swingx.JXTable;

/**
 * <code>FDTable</code> extends the <code>JXTable</code> super class from the
 * <a href="https://swingx.dev.java.net/">SwingX</a> project and provides
 * further convenience methods for highlighting rows and cells.
 *
 * <code>JXTable</code> itself extends <code>JTable</code> with several
 * improvements, like hiding columns and better sorting support.
 * 
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.0
 */
public class FDTable extends JXTable {

    /** Compiler-generated serial version identifier. */
    private static final long serialVersionUID = -535643396889655119L;
    /** Maps rows to their background color. */
    private Map<Integer, Color> rowColor;
    /** Maps columns to their background color. */
    private Map<Integer, Color> columnColor;
    /** Cell background color. */
    private Color cellColor;
    /** Standard background color. */
    private Color defaultColor;

    /**
     * Class constructor specifying the table model with the data.
     * 
     * @param model a table model
     */
    public FDTable(TableModel model) {
        super(model);

        rowColor = new HashMap<Integer, Color>();
        columnColor = new HashMap<Integer, Color>();
    }

    /**
     * Standard class constructor.
     */
    FDTable() {
        super();
        rowColor = new HashMap<Integer, Color>();
        columnColor = new HashMap<Integer, Color>();
    }

    /**
     * Removes all color highlightings.
     */
    public void clearAllColor() {
        rowColor.clear();
        columnColor.clear();
    }

    /**
     * Highlights a row with a given background color.
     *
     * @param row the row to highlight
     * @param c the background color
     */
    public void setRowColor(int row, Color c) {
        rowColor.put(new Integer(row), c);
    }

    /**
     * Highlights a column with a given background color.
     *
     * @param column the column to highlight
     * @param c the background color
     */
    public void setColumnColor(int column, Color c) {
        columnColor.put(new Integer(column), c);
    }

    /**
     * Sets the standard cell background color.
     *
     * @param c the background color
     */
    public void setCellColor(Color c) {
        cellColor = c;
    }

    @Override
    public Class<? extends Object> getColumnClass(int column) {
        return getValueAt(0, column).getClass();
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row,
            int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (defaultColor == null) {
            defaultColor = c.getBackground();
        }

        // Color order is as follows:
        // rowSelection, checkBox toggle for row color, column color, cell color

        if (!isRowSelected(row)) {
            Color color = (Color) rowColor.get(new Integer(row));
            if (color == null || Boolean.FALSE.equals(getModel().getValueAt(
                    row, 0))) {
                color = (Color) columnColor.get(new Integer(column));
            }
            if (color == null) {
                // cell color only if cell has special value, for example
                // purposes, if the cell value begins with a X2
                Object value = getValueAt(row, column);
                if (value != null && value.toString().startsWith("X2")) {
                    color = cellColor;
                }
            }
            if (color != null) {
                c.setBackground(color);
            } else {
                c.setBackground(defaultColor);
            }
        }

        return c;
    }
}
