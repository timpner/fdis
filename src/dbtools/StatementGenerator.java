package dbtools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import fd.FDep;
import fd.Relation;
import fd.SynRelation;

/**
 * Provides methods to generate SQL-Statements to implement changes to
 * PostgreSQL database.
 * 
 * @author Matthias Meine <m.meine@tu-bs.de>
 * 
 */
public class StatementGenerator implements IStatement {

    private final String nl = System.getProperty("line.separator");
    /** The global <code>logger</code> object. */
    private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * Generates the name of the Triggerfunction for the database
     *
     * @param id
     *            the id of the fd in the catalog
     * @param rname
     *            the relation name
     * @return the name of the triggerfunction
     */
    public String generateTFunctionName(int id, String rname) {
        String fname = "\"check";
        fname += rname + "FA" + id + "\"()";
        return fname;
    }

    /**
     * Generates the name of the Trigger for the database
     *
     * @param id
     *            the id of the fd in the catalog
     * @param rname
     *            the relation name
     * @return the name for the trigger
     */
    public String generateTriggerName(int id, String rname) {
        String triggerName = "\"Trigger";
        triggerName += rname + "FA" + id + "\"";

        return triggerName;
    }

    /**
     * Generates the SQL-Statement to implement the Triggerfunction
     *
     * @param fd
     *            the fd that shall be implemented by the function
     * @param rname
     *            the relation name
     * @param fname
     *            the trigger function name
     * @return the SQL-Statement to implement the function
     */
    public String generateTFunction(FDep fd, String rname, String fname) {
        // Name wird hier so generiert, wie er im statement benötigt wird
        // dies utnerscheidet sich davon, wie er in der DB gespeichert werden
        // muss

        StringBuffer sb = new StringBuffer();
        sb.append("CREATE OR REPLACE FUNCTION " + fname + " RETURNS trigger AS" + nl);
        sb.append("$BODY$DECLARE" + nl);
        sb.append("anzahl integer;" + nl);
        sb.append("BEGIN" + nl);
        sb.append("SELECT INTO anzahl count(*) FROM \"" + rname + "\" t1, \"" + rname + "\" t2" + nl);
        sb.append("WHERE ");
        Iterator<String> lhs = fd.getLeftSide().iterator();
        while (lhs.hasNext()) {
            String nextLHS = lhs.next();
            sb.append("t1.\"" + nextLHS + "\"=t2.\"" + nextLHS + "\"");
            sb.append(" AND ");
        }
        Iterator<String> rhs = fd.getRightSide().iterator();
        sb.append("(");
        while (rhs.hasNext()) {
            String nextRHS = rhs.next();
            sb.append("t1.\"" + nextRHS + "\"!=t2.\"" + nextRHS + "\"");
            if (rhs.hasNext()) {
                sb.append(" OR ");
            } else {
                sb.append(");" + nl);
            }
        }
        sb.append("IF anzahl > 0 THEN" + nl);
        sb.append("RAISE EXCEPTION 'Fehler bei FA';" + nl);
        sb.append("END IF;" + nl);
        sb.append("RETURN NULL;" + nl);
        sb.append("END;$BODY$" + nl);
        sb.append("LANGUAGE 'plpgsql'");

        return sb.toString();
    }

    /**
     * Generates the SQL-Statement to implement the trigger
     *
     * @param id
     *            the id of the fd in the database
     * @param rname
     *            the relation name
     * @param fname
     *            the name of the triggerfunction
     * @return the SQL-Statement to implement the trigger
     */
    public String generateTrigger(int id, String rname, String fname) {
        StringBuffer sb = new StringBuffer();
        sb.append("CREATE TRIGGER \"Trigger" + rname + "FA" + id + "\"" + nl);
        sb.append("AFTER INSERT OR UPDATE ON \"" + rname + "\"" + nl);
        sb.append("FOR EACH STATEMENT EXECUTE PROCEDURE public." + fname + ";");

        return sb.toString();
    }

    /**
     * Generates the SQL-Statement to create a table specified by the given
     * typeMap
     *
     * @param typeMap
     *            a Map that maps attributes to datatypes
     * @param srel
     *            the SynRelation to create
     * @return the SQL-Statment to create the table
     */
    public String generateCreateTable(Map<String, String> typeMap,
            SynRelation srel) {
        StringBuffer sb = new StringBuffer();
        sb.append("CREATE TABLE \"" + srel.getRelationName() + "\"" + nl);
        sb.append("(" + nl);
        Set<String> attributeSet = srel.getColumns();// = typeMap.keySet();

        for (String attribute : attributeSet) {
            sb.append("\"" + attribute + "\" " + typeMap.get(attribute) + "," + nl);
        }
        // use originFD as primary key.
        // if no originFD is set yet, use a key candidate to set originFD
        if (srel.getOriginFD().getLeftSide().isEmpty()) {
            for (FDep fa : srel.getFdeps()) {
                if (fa.getIsKey()) {
                    srel.setOriginFD(fa);
                    break;
                } else {
                    for (Set<String> x : srel.determineKeys(false)) {
                        FDep newFD = new FDep();
                        newFD.addLeftSide(x);
                        newFD.addRightSide(srel.getColumns());
                        srel.setOriginFD(newFD);
                        break;
                    }
                }
            }
        }
        sb.append("CONSTRAINT \"" + srel.getRelationName() + "_pkey\" PRIMARY KEY (\"");
        Iterator<String> it1 = srel.getOriginFD().getLeftSide().iterator();
        while (it1.hasNext()) {
            sb.append(it1.next());
            if (it1.hasNext()) {
                sb.append("\",\"");
            } else {
                sb.append("\")" + nl);
            }
        }

        sb.append(")");

        return sb.toString();
    }

    /**
     * Generates the SQL-Statement to implement a foreign key
     *
     * @param referencedTable
     *            the referenced Table
     * @param referencingTable
     *            the referencing Table
     * @param refMap
     *            a Map containing referencing attributes as keys and referenced
     *            attributes as values
     * @param onUpdate
     *            the action to be performed on Update
     * @param onDelete
     *            the action to be performed on Delete
     * @return the SQL-Statement to implement the foreign key
     */
    public String generateAddConstraint(String referencedTable,
            String referencingTable, Map<String, String> refMap,
            String onUpdate, String onDelete) {

        StringBuffer sb = new StringBuffer();
        sb.append("ALTER TABLE \"" + referencingTable + "\" " + nl);
        sb.append("ADD CONSTRAINT \"" + referencingTable + "_fkey_" + refMap.keySet().iterator().next() + "\"");
        sb.append(" FOREIGN KEY (");
        Iterator<String> it1 = refMap.keySet().iterator();
        while (it1.hasNext()) {
            sb.append("\"" + it1.next() + "\"");
            if (it1.hasNext()) {
                sb.append(",");
            } else {
                sb.append(")" + nl);
            }
        }
        sb.append("REFERENCES \"" + referencedTable + "\" (");
        Iterator<String> it2 = refMap.values().iterator();
        while (it2.hasNext()) {
            sb.append("\"" + it2.next() + "\"");
            if (it2.hasNext()) {
                sb.append(",");
            } else {
                sb.append(")" + nl);
            }
        }
        sb.append("ON UPDATE " + onUpdate + " ON DELETE " + onDelete);
        return sb.toString();
    }

    /**
     * Generates a unique name for a foreign key
     * @param con a database connection
     * @param one_element an arbitrary attribute
     * @param rel name of the referencing relation
     * @return a unique name for a foreign key
     */
    private String getFKname(Connection con, String one_element, String rel) {
        int i = 1;
        boolean newNameRequired = true;

        String isNameAssigned = "SELECT COUNT(tc.constraint_name) " +
                "FROM information_schema.table_constraints tc " +
                "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                "AND tc.table_name = ? " +
                "AND tc.constraint_name = ?";

        String name = null;
        ResultSet rs;

        try {
            PreparedStatement pstmt = con.prepareStatement(isNameAssigned);
            // generate new name and test if it is unique
            while (newNameRequired) {
                name = rel + "_fkey_" + one_element + "_" + i;
                pstmt.setString(1, rel);
                pstmt.setString(2, name);
                rs = pstmt.executeQuery();
                // if it is not unique increase i
                while (rs.next()) {
                    if (rs.getInt(1) > 0) {
                        i++;
                    } else {
                        newNameRequired = false;
                    }
                }
            }
        } catch (SQLException e) {
            logger.logp(Level.SEVERE, StatementGenerator.class.getName(),
                    "getFKname", "Couldn't check whether FK name already exists.",
                    e);
        }
        return name;
    }

    /**
     * Detects all possible foreign keys between new SynRelations and generates
     * SQL-Statements to implement foreign keys between new created SynRelations
     *
     * @param newRelations
     *            a set of all new created SynRelation-objects
     * @param con
     *            the database connection
     * @return a set of SQL-Statements - one statement for each foreign key
     */
    public Set<String> generateIntraFKeys(Set<SynRelation> newRelations,
            Connection con) {
        CatalogManager catMan = new CatalogManager();
        Set<String> statements = new HashSet<String>();
        Set<String> colsToRemove;
        for (Relation rel : newRelations) {
            Set<Set<String>> power_cols = Relation.powerset(rel.getColumns().toArray(new String[0]));

            String stmt = "SELECT DISTINCT kcu.column_name " + "FROM information_schema.table_constraints tc " + "LEFT JOIN information_schema.key_column_usage kcu " + "ON tc.constraint_catalog = kcu.constraint_catalog " + "AND tc.constraint_schema = kcu.constraint_schema " + "AND tc.constraint_name = kcu.constraint_name " + "LEFT JOIN information_schema.referential_constraints rc " + "ON tc.constraint_catalog = rc.constraint_catalog " + "AND tc.constraint_schema = rc.constraint_schema " + "AND tc.constraint_name = rc.constraint_name " + "LEFT JOIN information_schema.constraint_column_usage ccu " + "ON rc.unique_constraint_catalog = ccu.constraint_catalog " + "AND rc.unique_constraint_schema = ccu.constraint_schema " + "AND rc.unique_constraint_name = ccu.constraint_name " + "WHERE tc.table_name = ? " + "AND constraint_type = 'FOREIGN KEY'";

            ResultSet rs = null;
            colsToRemove = new HashSet<String>();
            try {
                PreparedStatement pstmt = con.prepareStatement(stmt);
                pstmt.setString(1, rel.getRelationName());
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    colsToRemove.add(rs.getString(1));
                }
            } catch (SQLException e) {
                logger.logp(Level.SEVERE, StatementGenerator.class.getName(),
                        "generateIntraFKeys", "Couldn't retrieve attributes.",
                        e);
            }

            power_cols.removeAll(colsToRemove);
            List<Set<String>> list = new LinkedList<Set<String>>(power_cols);
            Comparator<Set<String>> c = new SetComparator();
            Collections.sort(list, c);

            // Search in all other newRelations if it contains list
            for (Set<String> potential_foreign_key : list) {
                if (potential_foreign_key.size() != 0) {
                    for (Relation different_rel : newRelations) {
                        if (rel != different_rel) {
                            if (different_rel.getColumns().containsAll(
                                    potential_foreign_key)) {
                                if (different_rel.isKey(potential_foreign_key)) {
                                    catMan.setAsUnique(different_rel.getRelationName(),
                                            potential_foreign_key, con);
                                    StringBuffer fk_atts = new StringBuffer();
                                    Iterator<String> it = potential_foreign_key.iterator();

                                    while (it.hasNext()) {
                                        fk_atts.append("\"" + it.next() + "\"");
                                        if (it.hasNext()) {
                                            fk_atts.append(",");
                                        }
                                    }
                                    String one_element = fk_atts.toString().split(",")[0].replaceAll("\"", "");
                                    String name = getFKname(con, one_element, rel.getRelationName());

                                    StringBuffer sb = new StringBuffer();
                                    sb.append("ALTER TABLE ");
                                    sb.append("\"" + rel + "\" ");
                                    sb.append("ADD CONSTRAINT \"" + name + "\" FOREIGN KEY ");
                                    sb.append("(" + fk_atts + ")" + " REFERENCES \"" + different_rel.getRelationName() + "\" (" + fk_atts.toString() + ");");
                                    statements.add(sb.toString());
                                }
                            }
                        }
                    }
                }
            }
        }

        return statements;
    }
}

/**
 * Compares two sets of Strings according to their cardinality.
 * 
 * @author Matthias Meine
 */
class SetComparator implements Comparator<Set<String>> {

    @Override
    public int compare(Set<String> arg0, Set<String> arg1) {

        return (((Set<String>) arg1).size() - ((Set<String>) arg0).size());
    }
}
