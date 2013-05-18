package gui;

import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;

/**
 * <code>FDTableModel</code> extends the <code>AbstractTableModel</code> 
 * class. It is specialized for the needs of functional dependency data, for
 * instance it provides functionality for adding left- and right-hand side
 * attributes.
 *
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.1
 */
public class FDTableModel extends AbstractTableModel {

    /** Compiler-generated serial version identifier. */
    private static final long serialVersionUID = 1233940151014021874L;
    /** The column names. */
    private Vector<String> columnNames;
    /** The table data. */
    private Vector<Vector<? extends Object>> rowData;

    /**
     * Standard constructor.
     */
    protected FDTableModel() {
        columnNames = new Vector<String>();
        columnNames.addElement("LHS");
        columnNames.addElement("RHS");
        columnNames.addElement("ID");
        columnNames.addElement("Key");

        rowData = new Vector<Vector<? extends Object>>();
    }

    /**
     * Adds a column to the model.
     *
     * @param col a column name
     */
    protected void addColumn(String col) {
        columnNames.addElement(col);
    }

    /**
     * Removes all columns from the model.
     */
    protected void removeAllColumns() {
        columnNames.clear();
    }

    /**
     * Removes all data from the model.
     */
    protected void removeAllData() {
        rowData.clear();
    }

    /**
     * Adds a row, specifying only the left-hand side of a
     * functional dependency.
     *
     * @param lhs the left-hand side attributes
     */
    protected void addRow(Set<String> lhs) {
        Vector<String> row = new Vector<String>();
        row.addElement(lhs.toString());
        row.addElement("KEY");

        rowData.addElement(row);
    }

    /**
     * Adds a row, specifying both the left-hand and right-hand side of a
     * functional dependency.
     *
     * @param lhs the left-hand side attributes
     * @param rhs the right-hand side attributes
     */
    protected void addRow(Set<String> lhs, Set<String> rhs) {
        Vector<String> row = new Vector<String>();
        row.addElement(lhs.toString());
        row.addElement(rhs.toString());

        rowData.addElement(row);
    }

    /**
     * Adds a row, specifying both the left-hand and right-hand side of a
     * functional dependency, as well as its ID and whether it is implemented
     * by a key constraint.
     *
     * @param lhs the left-hand side attributes
     * @param rhs the right-hand side attributes
     * @param id the ID
     * @param key is it implemented by a key constraint
     */
    protected void addRow(SortedSet<String> lhs, SortedSet<String> rhs, int id,
            boolean key) {
        Vector<Object> row = new Vector<Object>();
        row.addElement(lhs.toString());
        row.addElement(rhs.toString());
        row.addElement(id);
        row.addElement(key);

        rowData.addElement(row);
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public int getRowCount() {
        return rowData.size();
    }

    @Override
    public String getColumnName(int col) {
        return columnNames.get(col);
    }

    @Override
    public Object getValueAt(int row, int col) {
        return rowData.get(row).get(col);
    }

    /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.  If we didn't implement this method,
     * then the last column would contain text ("true"/"false"),
     * rather than a check box.
     * Copyright for this method by Sun Microsystems.
     */
    @Override
    public Class<? extends Object> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }
}