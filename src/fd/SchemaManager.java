package fd;

import control.DefaultController;
import control.DefaultController.NF;
import dbtools.CatalogManager;
import dbtools.ConnectionManager;
import dbtools.Credentials;
import dbtools.IStatement;
import dbtools.StatementGenerator;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * <code>SchemaManager</code> class holds all information about a loaded
 * database and provides the functionality for loading and updating the schema.
 * Furthermore, it manages the currently loaded relation and commits or
 * rolls back changes to it.
 *
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.1
 */
public class SchemaManager {

    /** The currently loaded database schema. */
    private DbSchema schema;
    /** The currently selected relation. */
    private Relation relation;
    /** The global <code>logger</code> object. */
    private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    /**
     * The relations that are synthesized from field <code>relation</code>,
     * in order to achieve 2 or 3 NF.
     */
    private SortedSet<SynRelation> normalizedRelations;
    /** The controller handling all model changes and view updates. */
    private DefaultController controller;
    /** The manager dealing with all catalog related queries. */
    private CatalogManager catMan;

    /**
     * Class constructor specifying the MVC controller.
     */
    public SchemaManager(DefaultController controller) {
        this.controller = controller;

        schema = new DbSchema("none");

        controller.addModel(schema);
    }

    /**
     * Generates a preview for the results of the specified normalization
     * algorithm.
     * 
     * @param form the desired normalform
     */
    public void normalize(NF form) {
        normalizedRelations = relation.normalize(form);

        relation.firePropertyChange(
                DefaultController.ELEMENT_NORMALIZATION_PROPERTY,
                null,
                normalizedRelations);
    }

    /**
     * Commits a previewed normalization. Committing takes place as a
     * transaction, thus possible errors will not lead to a inconsistent state,
     * but to an abort of the transaction.
     * <p>
     * First, the new relations, as they are specified by the normalization
     * algorithm, are created. Second, all contents from the normalized
     * relation are copied into the new ones.
     * <p>
     * Then, potential foreign key (FK)
     * constraints are dealt with: if any relation has foreign keys referencing
     * the normalized relation, these FKs are replaced with FKs referencing one
     * of the new relations. To be more precise, the relation whose primary key
     * corresponds with the foreign key attributes, is referenced.
     * If the normalized relation itself has a foreign key, all new relations 
     * with attributes from that FK are implemented with a corresponding FK.
     * <p>
     * Then, all FD belonging to the normalized relation are deleted from the
     * fd_catalog. Finally, that relation is dropped from the database and
     * the FDs from the new relations are added to the fd_catalog.
     */
    public void commitNormalization() {

        Connection con = ConnectionManager.getConnection();
        // Start transaction.
        try {
            con.setAutoCommit(false);
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "commitNormalization",
                    "Couldn't start transaction.", ex);
        }

        if (reqsFulfilled(con)) {
            /*
             * Fetches types, ranges, and optional NOT NULL constraints for each
             * attribute of the relation, that is to be normalized.
             */
            Map<String, String> typeMap = generateAttTypes(con);

            /*
             * Creates new relations, as specified by the normalization
             * algorithm.
             */
            createTables(con, typeMap);

            /*
             * Copies contents from the old relation into the new ones.
             */
            copyContents(con);

            /*
             * Changes potential foreign keys, that reference the normalized
             * relation, so that they are referencing one of the new relations
             * instead.
             */
            alterIncomingFK(con);

            /*
             * Implements potential foreign keys of the normalized relation as 
             * FKs of the new relations with the same attributes.
             */
            addOutgoingFK(con);

            /*
             * Transfers any existing constraints on the normalized relation
             * to the created relations.
             */
            addChecks(con);

            /*
             * Transfers any existing unique constraints on the normalized
             * relation to the created relations.
             */
            //addUniques(con);

            /*
             * Adds intra-relation foreign keys, if not disabled.
             */
            if (controller.isIntraFkEnabled()) {
                IStatement gen = new StatementGenerator();
                Set<String> statements = gen.generateIntraFKeys(
                        normalizedRelations, con);
                Statement stmt;
                try {
                    stmt = con.createStatement();
                    for (String stat : statements) {
                        stmt.executeUpdate(stat);
                    }
                    stmt.close();
                } catch (SQLException e) {
                    logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                            "commitNormalization",
                            "Couldn't update intra-relation FD.", e);
                }
            }

            /*
             * Removes all FDs belonging to the relation that is to be normalized
             * from the fd_catalog.
             */
            for (FDep fd : relation.getFdeps()) {
                catMan.removeFD(fd.getId(), relation.getRelationName(), con);
            }

            /*
             * Drops the normalized relation.
             */
            dropTable(con);

            /*
             * Adds FDs for each new relation to the fd_catalog.
             */
            for (SynRelation table : normalizedRelations) {
                for (FDep fd : table.getFdeps()) {
                    catMan.addFD(table.getRelationName(), fd, con);
                }
            }
        }

        /*
         * Commit changes. End transaction.
         */
        try {
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException ex) {
            try {
                con.rollback();
            } catch (SQLException ex1) {
                logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                        "commitNormalization",
                        "Couldn't rollback transaction.", ex1);
            }
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "commitNormalization",
                    "Couldn't commit transaction.", ex);
        }

        ConnectionManager.closeConnection();

        /*
         * Reload the database schema in order to refresh the UI.
         */
        reloadSchema();
    }

    /**
     * Checks whether requirements for a normalization are fulfilled, i.e. there
     * are no external triggers or views on the relation.
     *
     * @param con a database connection
     * @return true if all requirements are fulfilled
     */
    private boolean reqsFulfilled(Connection con) {
        String viewsQuery = "SELECT view_name " +
                "FROM information_schema.view_table_usage " +
                "WHERE table_schema NOT IN ('information_schema', 'pg_catalog') " +
                "AND table_name = ?";

        String triggerQuery = "SELECT trigger_name " +
                "FROM information_schema.triggers " +
                "WHERE trigger_schema NOT IN " +
                "('pg_catalog', 'information_schema') " +
                "AND event_object_table = ?";

        String catalogQuery = "SELECT name " +
                "FROM fd_catalog_trigger";

        PreparedStatement pstmt;
        ResultSet rs = null;
        Set<String> views = null;
        Set<String> triggers = null;
        Set<String> allTriggers = null;
        try {
            /*
             * Select all views referencing the relation.
             */
            pstmt = con.prepareStatement(viewsQuery);
            pstmt.setString(1, relation.getRelationName());

            rs = pstmt.executeQuery();
            views = new HashSet<String>();
            while (rs.next()) {
                views.add(rs.getString(1));
            }

            /*
             * Select all triggers on the relation.
             */
            pstmt = con.prepareStatement(triggerQuery);
            pstmt.setString(1, relation.getRelationName());

            rs = pstmt.executeQuery();
            triggers = new HashSet<String>();
            while (rs.next()) {
                triggers.add(rs.getString(1));
            }

            /*
             * Select all triggers in the fd_catalog.
             */
            pstmt = con.prepareStatement(catalogQuery);

            rs = pstmt.executeQuery();
            allTriggers = new HashSet<String>();
            while (rs.next()) {
                allTriggers.add(rs.getString(1));
            }

            rs.close();
            pstmt.close();
        } catch (SQLException ex1) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "reqsFulfilled",
                    "Couldn't retrieve views on the normalized table.", ex1);
        }

        /*
         * Add extra " to the triggers in order to compare them to the fd_catalog
         * triggers.
         */
        Set<String> relTriggers = new HashSet<String>();
        for (String trigger : triggers) {
            trigger = "\"" + trigger + "\"";
            relTriggers.add(trigger);
        }

        /*
         * Remove all triggers that are in the fd_catalog.
         */
        relTriggers.removeAll(allTriggers);

        if (!views.isEmpty()) {
            String ls = System.getProperty("line.separator");
            JOptionPane.showMessageDialog(null,
                    "Could not drop table " +
                    relation.getRelationName() +
                    " because of depending views: " + ls + views,
                    "Normalization Aborted",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        } else if (!relTriggers.isEmpty()) {
            String ls = System.getProperty("line.separator");
            JOptionPane.showMessageDialog(null,
                    "Could not drop table " +
                    relation.getRelationName() +
                    " because of depending triggers: " + ls + relTriggers,
                    "Normalization Aborted",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Fetches types, ranges, and optional NOT NULL constraints for each
     * attribute of the relation, that is to be normalized.
     *
     * @param con a database connection
     * @return a map of attributes and their corresponding types/constraints
     */
    private Map<String, String> generateAttTypes(Connection con) {

        String queryAttr = "SELECT ordinal_position, " +
                "column_name, " +
                "data_type, " +
                "column_default, " +
                "is_nullable, " +
                "character_maximum_length, " +
                "numeric_precision, " +
                "numeric_scale, " +
                "domain_name " +
                "FROM information_schema.columns " +
                "WHERE table_name = '" + relation.getRelationName() + "' " +
                "ORDER BY ordinal_position";

        ResultSet rs = null;
        Statement stmt;
        Map<String, String> typeMap = new HashMap<String, String>();

        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(queryAttr);
            String name = null;
            String type = null;
            String prec = null;
            String scale = null;
            String maxLen = null;
            String def = null;
            String nullable = null;
            String domain = null;

            while (rs.next()) {
                name = rs.getString(2);
                type = rs.getString(3);
                def = rs.getString(4);
                nullable = rs.getString(5);
                maxLen = rs.getString(6);
                prec = rs.getString(7);
                scale = rs.getString(8);
                domain = rs.getString(9);

                /*
                 * If the data type is integer or bigint and the default
                 * column value contains a 'nextval', the actual data type is
                 * 'serial'.
                 */
                if (def != null && (type.equals("integer") ||
                        type.equals("bigint"))) {
                    if (def.contains("nextval")) {
                        type = "serial";
                    }
                /*
                 * If a domain is set, the actual data type equals the
                 * domain.
                 */
                } else if (domain != null) {
                    type = domain;
                }

                StringBuffer sb = new StringBuffer();
                if (maxLen != null) {
                    sb.append(type).append("(").append(maxLen).append(")");
                } else if (type.equals("numeric") && prec != null &&
                        scale != null) {
                    sb.append(type).append("(").append(prec).append(",").
                            append(scale).append(")");
                } else {
                    sb.append(type);
                }
                if (nullable.equals("NO")) {
                    sb.append(" NOT NULL");
                }

                /*
                 * Only pay attention to the default value, if it does not
                 * contain 'nextval', for this indicates that the data type is
                 * an auto-incremental 'serial'.
                 */
                if (def != null) {
                    if (!def.contains("nextval")) {
                        sb.append(" DEFAULT ").append(def);
                    }
                }

                typeMap.put(name, sb.toString());
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "generateAttTypes",
                    "Couldn't get attributes types/constraints.", ex);
        }
        return typeMap;
    }

    /**
     * Generates CREATE TABLE statements for each new relation, using the
     * fetched attribute types (stored in typeMap).
     *
     * @param con a database connection
     * @param typeMap a map of attributes and their types
     */
    private void createTables(Connection con, Map<String, String> typeMap) {

        IStatement gen = new StatementGenerator();
        String createTable = null;
        Statement stmt;
        try {
            stmt = con.createStatement();

            for (SynRelation table : normalizedRelations) {
                createTable = gen.generateCreateTable(typeMap, table);
                stmt.executeUpdate(createTable);
            }

            stmt.close();
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "createTables",
                    "Couldn't create new table.", ex);
        }
    }

    /**
     * Copies contents from the original (normalized) relation to the new
     * (synthesized) ones.
     *
     * @param con a database connection
     */
    private void copyContents(Connection con) {
        /*
         *  The SQL query has the following form:
         *
         * INSERT INTO "VERMIETUNG2_1" SELECT "MName","VName" FROM "VERMIETUNG2"
         *
         * Again, a more elegant PreparedStatement, such as
         *
         * INSERT INTO ? SELECT ? FROM ?
         *
         * does not work because quotation marks are used within the string.
         */
        String copyContents;
        Statement stmt;

        try {
            stmt = con.createStatement();
            /*
             * Generate copy SQL query.
             */
            for (SynRelation table : normalizedRelations) {
                StringBuffer sb = new StringBuffer();
                for (String col : table.getColumns()) {
                    sb.append("\"").append(col).append("\",");
                }
                String cols = sb.substring(0, sb.length() - 1);
                sb = new StringBuffer();
                sb.append("INSERT INTO \"").append(table.getRelationName()).
                        append("\" SELECT DISTINCT ").append(cols).append(" FROM \"").
                        append(table.getOriginName()).append("\"");
                copyContents = sb.toString();

                int n = stmt.executeUpdate(copyContents);
                System.out.println("rows affected: " + n);
            }
            stmt.close();
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "copyContents",
                    "Couldn't copy contents from one relation to another.", ex);
        }
    }

    /**
     * Fetches all foreign key constraints that reference the relation,
     * that is to be normalized. For each foreign key, a new foreign key
     * with the same constraints has to be generated and added to the
     * database. However, this new FK references one of the new
     * (synthesized) relations and not the originial relation anymore.
     *
     * Then, the orginial FKs are dropped.
     *
     * @param con a database connection
     */
    private void alterIncomingFK(Connection con) {

        String getFkName = "SELECT DISTINCT tc.constraint_name " +
                "FROM information_schema.table_constraints tc " +
                "LEFT JOIN information_schema.referential_constraints rc " +
                "ON tc.constraint_catalog = rc.constraint_catalog " +
                "AND tc.constraint_schema = rc.constraint_schema " +
                "AND tc.constraint_name = rc.constraint_name " +
                "LEFT JOIN information_schema.constraint_column_usage ccu " +
                "ON rc.unique_constraint_catalog = ccu.constraint_catalog " +
                "AND rc.unique_constraint_schema = ccu.constraint_schema " +
                "AND rc.unique_constraint_name = ccu.constraint_name " +
                "WHERE ccu.table_name = ? " +
                "AND constraint_type = 'FOREIGN KEY'";

        ResultSet rs = null;
        try {
            /*
             * Gets all foreign keys referencing the normalized relation. Stores
             * their names in a set of Strings.
             */
            PreparedStatement pstmt = con.prepareStatement(getFkName);
            pstmt.setString(1, relation.getRelationName());

            rs = pstmt.executeQuery();

            Set<String> constraints = new HashSet<String>();

            while (rs.next()) {
                constraints.add(rs.getString(1));
            }

            rs.close();
            pstmt.close();

            /*
             * For each foreign key constraint referencing the normalized
             * relation, generate a new one, that references a new relations
             * instead.
             */
            IStatement gen = new StatementGenerator();
            for (String cons : constraints) {
                String getFK = "SELECT DISTINCT " +
                        "tc.table_name, " +
                        "kcu.column_name, " +
                        "kcu2.column_name AS references_field, " +
                        "rc.update_rule AS on_update, " +
                        "rc.delete_rule AS on_delete " +
                        "FROM information_schema.table_constraints tc " +
                        "LEFT JOIN information_schema.key_column_usage kcu " +
                        "ON tc.constraint_catalog = kcu.constraint_catalog " +
                        "AND tc.constraint_schema = kcu.constraint_schema " +
                        "AND tc.constraint_name = kcu.constraint_name " +
                        "LEFT JOIN information_schema.referential_constraints rc " +
                        "ON tc.constraint_catalog = rc.constraint_catalog " +
                        "AND tc.constraint_schema = rc.constraint_schema " +
                        "AND tc.constraint_name = rc.constraint_name " +
                        "LEFT JOIN information_schema.constraint_column_usage ccu " +
                        "ON rc.unique_constraint_catalog = ccu.constraint_catalog " +
                        "AND rc.unique_constraint_schema = ccu.constraint_schema " +
                        "AND rc.unique_constraint_name = ccu.constraint_name " +
                        "INNER JOIN information_schema.key_column_usage kcu2 " +
                        "ON kcu.position_in_unique_constraint = kcu2.ordinal_position " +
                        "AND kcu2.constraint_name = rc.unique_constraint_name " +
                        "WHERE constraint_type = 'FOREIGN KEY' " +
                        "AND tc.constraint_name = ? " +
                        "AND ccu.table_name = ?";

                pstmt = con.prepareStatement(getFK);

                pstmt.setString(1, cons);
                pstmt.setString(2, relation.getRelationName());

                rs = pstmt.executeQuery();

                String referencingTable = null;
                String columnName = null;
                String referencedColumn = null;
                String onUpdate = null;
                String onDelete = null;

                Map<String, String> refMap = new HashMap<String, String>();

                while (rs.next()) {
                    referencingTable = rs.getString(1);
                    columnName = rs.getString(2);
                    referencedColumn = rs.getString(3);
                    onUpdate = rs.getString(4);
                    onDelete = rs.getString(5);

                    refMap.put(columnName, referencedColumn);
                }

                rs.close();
                pstmt.close();

                /**
                 * Determine which of the new relations shall be referenced
                 * instead of the normalized one. This depends on where
                 * the FKs attributes are a primary key.
                 */
                String referencedTable = null;
                Set<String> fkeyAtts = new HashSet<String>(refMap.values());
                for (SynRelation table : normalizedRelations) {
                    if (table.isKey(fkeyAtts)) {
                        referencedTable = table.getName();
                        // Make sure that the FK's attributes are unique.
                        catMan.setAsUnique(referencedTable, fkeyAtts, con);
                    }
                }
                String addConstraint =
                        gen.generateAddConstraint(referencedTable,
                        referencingTable, refMap, onUpdate, onDelete);

                // DELETE CONSTRAINT
                String drop = "ALTER TABLE \"" + referencingTable +
                        "\" DROP CONSTRAINT \"" + cons + "\"";
                Statement st = con.createStatement();
                st.execute(drop);
                System.out.println("removed FK [" + cons + "]");

                // ADD CONSTRAINT
                System.out.println(addConstraint);
                st = con.createStatement();
                st.execute(addConstraint);
                System.out.println("added FK [" + addConstraint + "]");

                st.close();
                pstmt.close();
            }

        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(), "alterFK",
                    "Couldn't change foreign keys referencing the " +
                    "normalized relation.", ex);
        }
    }

    /**
     * If the normalized relation itself has a foreign key, all new relations
     * with attributes from that FK are implemented with a corresponding FK.
     *
     * @param con a database connection
     */
    private void addOutgoingFK(Connection con) {

        String getFkName = "SELECT DISTINCT tc.constraint_name " +
                "FROM information_schema.table_constraints tc " +
                "WHERE constraint_type = 'FOREIGN KEY' " +
                "AND tc.table_name = ?";

        PreparedStatement pstmt;
        ResultSet rs = null;
        try {
            /*
             * Retrieves all foreign keys of the normalized relation and add
             * their names to a set of Strings.
             */
            pstmt = con.prepareStatement(getFkName);

            pstmt.setString(1, relation.getRelationName());

            rs = pstmt.executeQuery();

            Set<String> constraints = new HashSet<String>();
            while (rs.next()) {
                constraints.add(rs.getString(1));
            }

            rs.close();
            pstmt.close();

            /*
             * For each foreign key constraint on the normalized relation,
             * get detailed information about it.
             */
            IStatement gen = new StatementGenerator();
            for (String cons : constraints) {
                String getFK = "SELECT DISTINCT " +
                        "kcu2.table_name AS references_table, " +
                        "kcu.column_name, " +
                        "kcu2.column_name AS references_field, " +
                        "rc.update_rule AS on_update, " +
                        "rc.delete_rule AS on_delete " +
                        "FROM information_schema.table_constraints tc " +
                        "LEFT JOIN information_schema.key_column_usage kcu " +
                        "ON tc.constraint_catalog = kcu.constraint_catalog " +
                        "AND tc.constraint_schema = kcu.constraint_schema " +
                        "AND tc.constraint_name = kcu.constraint_name " +
                        "LEFT JOIN information_schema.referential_constraints rc " +
                        "ON tc.constraint_catalog = rc.constraint_catalog " +
                        "AND tc.constraint_schema = rc.constraint_schema " +
                        "AND tc.constraint_name = rc.constraint_name " +
                        "LEFT JOIN information_schema.constraint_column_usage ccu " +
                        "ON rc.unique_constraint_catalog = ccu.constraint_catalog " +
                        "AND rc.unique_constraint_schema = ccu.constraint_schema " +
                        "AND rc.unique_constraint_name = ccu.constraint_name  " +
                        "INNER JOIN information_schema.key_column_usage kcu2 " +
                        "ON kcu.position_in_unique_constraint = kcu2.ordinal_position " +
                        "AND kcu2.constraint_name = rc.unique_constraint_name " +
                        "WHERE constraint_type = 'FOREIGN KEY' " +
                        "AND tc.table_name = ? " +
                        "AND tc.constraint_name = ?";

                pstmt = con.prepareStatement(getFK);

                pstmt.setString(1, relation.getRelationName());
                pstmt.setString(2, cons);

                rs = pstmt.executeQuery();

                String referencedTable = null;
                String columnName = null;
                String referencedColumn = null;
                String onUpdate = null;
                String onDelete = null;

                Map<String, String> refMap = new HashMap<String, String>();

                while (rs.next()) {
                    referencedTable = rs.getString(1);
                    columnName = rs.getString(2);
                    referencedColumn = rs.getString(3);
                    onUpdate = rs.getString(4);
                    onDelete = rs.getString(5);

                    refMap.put(columnName, referencedColumn);
                }

                /*
                 * If there are new relations sharing attributes with the
                 * normalized relation, and these attributes are part of a
                 * foreign key, generate a new FK and add it to the
                 * corresponding relation.
                 */
                for (SynRelation table : normalizedRelations) {
                    Set<String> fkAtts = new HashSet<String>();
                    for (String referencingAtt : refMap.keySet()) {
                        if (table.getColumns().contains(referencingAtt)) {
                            fkAtts.add(referencingAtt);
                        }
                    }

                    if (fkAtts.size() != 0) {
                        String addConstraint = gen.generateAddConstraint(
                                referencedTable, table.getRelationName(),
                                refMap, onUpdate, onDelete);

                        // ADD CONSTRAINT
                        System.out.println("new FK: " + addConstraint);
                        Statement st = con.createStatement();
                        boolean success = st.execute(addConstraint);
                        System.out.println(success + ": added FK [" +
                                addConstraint + "]");
                        st.close();
                    }
                }

                rs.close();
                pstmt.close();
            }
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(), "addFK",
                    "Couldn't add a foreign key.", ex);
        }
    }

    /**
     * Transfers any existing reqsFulfilled constraints from the normalized relation
     * to the new relations.
     *
     * @param con a database connection
     */
    private void addChecks(Connection con) {
        String clauseQuery = "SELECT cc.constraint_name, " +
                "cc.check_clause " +
                "FROM information_schema.check_constraints cc " +
                "INNER JOIN information_schema.table_constraints tc " +
                "ON cc.constraint_name = tc.constraint_name " +
                "WHERE tc.constraint_type = 'CHECK' " +
                "AND cc.constraint_name NOT LIKE '%not_null' " +
                "AND cc.constraint_schema NOT IN ('information_schema', " +
                "'pg_catalog') " +
                "AND tc.table_name = ?";

        PreparedStatement pstmt;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(clauseQuery);
            pstmt.setString(1, relation.getRelationName());

            rs = pstmt.executeQuery();
            Set<String> clauses = new HashSet<String>();
            Set<String> affectedColumns;
            /*
             * Retrieve any checks.
             */
            while (rs.next()) {
                clauses.add(rs.getString(2));
            }
            rs.close();
            pstmt.close();

            for (String clause : clauses) {
                affectedColumns = new HashSet<String>();

                /*
                 * For each clause reqsFulfilled which columns are affected.
                 */
                for (String col : relation.getColumns()) {
                    if (clause.contains(col)) {
                        affectedColumns.add(col);
                    }
                }

                /*
                 * For each new relation, determine if it contains all affected
                 * columns. If so, create a reqsFulfilled constraint.
                 */
                for (SynRelation syn : normalizedRelations) {
                    if (syn.getColumns().containsAll(affectedColumns)) {
                        System.out.println("syn: " + syn);

                        StringBuffer sb = new StringBuffer();
                        sb.append(syn.getRelationName());
                        for (String col : affectedColumns) {
                            sb.append("_").append(col);
                        }
                        sb.append("_check");
                        System.out.println(sb);


                        String constraint = "ALTER TABLE \"" +
                                syn.getRelationName() +
                                "\" ADD CONSTRAINT \"" +
                                sb.toString() + "\" CHECK " + clause;
                        // ADD CONSTRAINT
                        System.out.println("New Check: " + constraint);
                        Statement stmt = con.createStatement();
                        boolean success = stmt.execute(constraint);
                        System.out.println(success + ": check added");
                        stmt.close();
                    }
                }
            }
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(), "addChecks",
                    "Couldn't add a check constraint.", ex);
        }
    }

    /**
     * Transfer existing unique constraints from the normalized relation to the
     * new ones.
     * 
     * @param con a database connection
     */
    private void addUniques(Connection con) {
        String uniqueQuery = "SELECT tc.constraint_name, " +
                "kcu.column_name " +
                "FROM information_schema.table_constraints tc " +
                "LEFT JOIN information_schema.key_column_usage kcu " +
                "ON tc.constraint_catalog = kcu.constraint_catalog " +
                "AND tc.constraint_schema = kcu.constraint_schema " +
                "AND tc.constraint_name = kcu.constraint_name " +
                "WHERE tc.constraint_type = 'UNIQUE' " +
                "AND tc.table_name = ?";

        PreparedStatement pstmt;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(uniqueQuery);
            pstmt.setString(1, relation.getRelationName());

            rs = pstmt.executeQuery();
            Set<String> affectedColumns = new HashSet<String>();
            Set<String> uniqueCols = null;
            while (rs.next()) {
                affectedColumns.add(rs.getString(2));
            }
            rs.close();
            pstmt.close();

            for (SynRelation syn : normalizedRelations) {
                uniqueCols = new HashSet<String>();
                for (String col : affectedColumns) {
                    if (syn.getColumns().contains(col)) {
                        uniqueCols.add(col);
                    }
                }
                System.out.println("syn: " + syn);
                System.out.println("uniques: " + uniqueCols);

                StringBuffer sb = new StringBuffer();
                sb.append(syn.getRelationName());
                for (String col : uniqueCols) {
                    sb.append("_").append(col);
                }
                sb.append("_key");
                System.out.println(sb);

                StringBuffer clause = new StringBuffer();
                clause.append("(");
                for (String col : uniqueCols) {
                    clause.append(col).append(",");
                }
                clause.deleteCharAt(clause.lastIndexOf(","));
                clause.append(")");

                String constraint = "ALTER TABLE \"" + syn.getRelationName() +
                        "\" ADD CONSTRAINT \"" +
                        sb.toString() + "\" UNIQUE " + clause;
                // ADD CONSTRAINT
                System.out.println("New Check: " + constraint);
                Statement stmt = con.createStatement();
                boolean success = stmt.execute(constraint);
                System.out.println(success + ": check added");
                stmt.close();
            }
            throw new UnsupportedOperationException();
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(), "addUniques",
                    "Couldn't add a unique constraint.", ex);
        }
    }

    /**
     * Drops the normalized relation.
     *
     * @param con a database connection
     */
    private void dropTable(Connection con) {
        String dropTable = "DROP TABLE \"" + relation.getRelationName() + "\"";
        try {
            Statement stmt = con.createStatement();
            stmt.execute(dropTable);
            stmt.close();
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "dropTable",
                    "Couldn't drop the normalized table.", ex);

        }
    }

    /**
     * Cancel a previews normalization process. The database is not altered in
     * any way.
     */
    public void cancelNormalization() {
        for (SynRelation rel : normalizedRelations) {
            controller.removeModel(rel);
        }

        normalizedRelations = new TreeSet<SynRelation>();
    }

    /**
     * Temporary adds a functional dependency to the loaded relation. Requires
     * a commit to really take effect in the database.
     *
     * @param fd a functional dependency
     */
    public void addFdep(FDep fd) {
        if (!isInClosure(fd)) {
            if (checkInstance(fd)) {
                relation.addAdditionalFdep(fd);
            } else {
                JOptionPane.showMessageDialog(null,
                        "The specified FD infringes the instance of the relation.",
                        "FD Not Possible",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null,
                    "The specified FD is already implied.",
                    "Closure Contains FD",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Temporarily removes a given functional dependency from the loaded
     * relation. Requires a commit to really take effect in the database.
     *
     * @param id the ID of the FD that is to be removed
     */
    public void removeFDep(Integer id) {
        FDep fd = catMan.getFD(id);
        relation.addRemovedFdep(fd);
    }

    /**
     * Commits changes, i.e. removes and adds so far temporary functional
     * dependencies.
     */
    public void commit() {
        String table = relation.getRelationName();

        Connection con = ConnectionManager.getConnection();
        // Start transaction.
        try {
            con.setAutoCommit(false);
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(), "commit",
                    "Couldn't start transaction.", ex);
        }

        for (FDep fd : relation.getAdditionalFdeps()) {
            catMan.addFD(table, fd, con);
        }

        for (FDep fd : relation.getRemovedFdeps()) {
            catMan.removeFD(fd.getId(), table, con);
        }

        // End transaction.
        try {
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException ex) {
            try {
                con.rollback();
            } catch (SQLException ex1) {
                logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                        "commit",
                        "Couldn't rollback transaction.", ex1);
            }
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "commit",
                    "Couldn't commit transaction.", ex);
        }

        ConnectionManager.closeConnection();

        reloadSchema();

    }

    /**
     * Rolls back all temporary changes in the loaded relation, i.e.
     * added and removed functional dependencies are discarded.
     */
    public void rollback() {
        Set<FDep> emptyFDSet = new LinkedHashSet<FDep>();
        relation.setRemovedFdeps(emptyFDSet);
        emptyFDSet = new LinkedHashSet<FDep>();
        relation.setAdditionalFdeps(emptyFDSet);

        loadRelation(relation);
    }

    /**
     * Checks whether there are changes in the loaded relations, that
     * require a commit or rolllback.
     *
     * @return true if there are changes in the relation
     */
    public boolean hasChanges() {
        if (relation == null) {
            return false;
        } else {
            return (!relation.getAdditionalFdeps().isEmpty() ||
                    !relation.getRemovedFdeps().isEmpty());
        }

    }

    /**
     * Checks if the given functional dependency (FD) is implied by the set of
     * all implemented FDs, i.e. if the FD is element of the closure of all fds.
     *
     * @param fd a functional dependency
     * @return true if the FD is implied by the other FDs in the loaded relation
     */
    private boolean isInClosure(FDep fd) {
        /*
         * Implementation according to bachelor's thesis
         * "Functional Dependencies in PostgreSQL Data Bases - Information
         * System". Instead of really computing the closure (which grows
         * exponentially), the relation's attributive closure is checked for
         * all left-hand side elements of the fd. This can be done in nearly
         * linear time.
         */
        Set<String> closure = relation.xPlus(true, fd.getLeftSide());
        return closure.containsAll(fd.getRightSide());
    }

    /**
     * Checks whether the instance of the loaded relation infringes a given
     * functional dependency.
     *
     * @param fd a functional dependecy
     * @return true if instance does not infringe the FD
     */
    private boolean checkInstance(FDep fd) {

        /*
         * Create a SQL statement that queries the number (COUNT(*)) of tupels
         * that have the same left-hand side attributes (as specified by
         * the FD), but do differ in their right-hand side attributes.
         *
         * Example:
         * SELECT COUNT(*) FROM relation r1, relation r2
         * WHERE r1.t1 = r2.t1
         * AND r1.t2 != r2.t2
         */
        String s = "SELECT COUNT(*) FROM \"" + relation.getRelationName() +
                "\" t1, \"" + relation.getRelationName() + "\" t2 WHERE";
        StringBuffer sb = new StringBuffer(s);

        boolean first = true;
        for (String lhs : fd.getLeftSide()) {
            if (first) {
                s = " t1.\"" + lhs + "\" = t2.\"" + lhs + "\"";
                first =
                        false;
            } else {
                s = " AND t1.\"" + lhs + "\" = t2.\"" + lhs + "\"";
            }

            sb.append(s);
        }

        first = true;
        Iterator<String> it = fd.getRightSide().iterator();
        String rhs;

        while (it.hasNext()) {
            rhs = it.next();
            if (first) {
                s = " AND (t1.\"" + rhs + "\" != t2.\"" + rhs + "\"";
                first =
                        false;
            } else {
                s = " OR t1.\"" + rhs + "\" != t2.\"" + rhs + "\"";
            }

            sb.append(s);
            if (!it.hasNext()) {
                sb.append(')');
            }

        }

        Statement stmt;
        ResultSet rs;

        int result = -1;
        try {
            Connection con = ConnectionManager.getConnection();

            stmt = con.createStatement();
            rs = stmt.executeQuery(sb.toString());

            while (rs.next()) {
                result = rs.getInt(1);
            }

            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "checkInstance",
                    "Couldn't check whether a new FD infringes the instance.",
                    ex);
        } finally {
            ConnectionManager.closeConnection();
        }

        /*
         * Result should be 0, otherwise there are tupels that infrige the FD.
         */
        if (result == 0) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Returns the schema of a database, including all relations and functional
     * dependencies. The database itself is specified by the selected
     * connection and the user's {@link Credentials}.
     *
     * @return the database schema
     */
    public DbSchema loadDB() {
        Connection con = ConnectionManager.getConnection();

        PreparedStatement pstmtRelations,
                pstmtColumns;
        ResultSet rsRelations = null, rsColumns = null;

        /*
         * Query selects all table names that do not belong to the fd_catalog
         * and are not listed in postgresqls internal schemas.
         */
        String queryTable = "SELECT table_name " +
                "FROM information_schema.tables " +
                "WHERE table_type = 'BASE TABLE' " +
                "AND table_name NOT LIKE 'fd_catalog%' " +
                "AND table_schema NOT IN " +
                "('pg_catalog', 'information_schema');";
        /*
         * Query selects all column names in the specified table.
         */
        String queryColumn = "SELECT column_name " +
                "FROM information_schema.columns " +
                "WHERE table_name = ?;";

        try {
            /*
             * Retrieves a <code>DatabaseMetaData</code> object that contains
             * metadata about the database to which this <code>Connection</code>
             * object represents a connection.
             */
            DatabaseMetaData dbmd = con.getMetaData();
            rsRelations = dbmd.getCatalogs();

            /*
             * Retrieves the schema's name.
             */
            while (rsRelations.next()) {
                String name = rsRelations.getString("TABLE_CAT");
                schema.setSchemaName(name);
            }

            pstmtRelations = con.prepareStatement(queryTable);
            pstmtColumns = con.prepareStatement(queryColumn);

            // Gets all relations in the database.
            rsRelations =
                    pstmtRelations.executeQuery();

            // Prepare temporary variables.
            String tableName;

            SortedSet<Relation> relations = new TreeSet<Relation>();
            Relation table;

            SortedSet<String> columns;
            String columnName;

            /*
             * For each relation, sets the second's query table name accordingly
             * and retrieves all its columns. The columns are added to a set
             * and this set is added to the relation object. After each round,
             * the relation object is added to the schema object.
             */
            while (rsRelations.next()) {
                tableName = rsRelations.getString("table_name");

                table = new Relation(tableName);
                columns = new TreeSet<String>();

                pstmtColumns.setString(1, tableName);
                rsColumns = pstmtColumns.executeQuery();

                while (rsColumns.next()) {
                    columnName = rsColumns.getString("column_name");
                    columns.add(columnName);
                }

                table.setColumns(columns);
                relations.add(table);
            }

            schema.setRelations(relations);

            // Closes all open ResultSets and Statements.
            rsRelations.close();
            pstmtRelations.close();

            rsColumns.close();
            pstmtColumns.close();

        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(), "loadDB",
                    "Couldn't load the schema.", ex);
        } finally {
            ConnectionManager.closeConnection();
        }

        /* Instantiates a catalog manager and lets him fetch all functional
         * dependencies for each relation object in the schema.
         */
        catMan = new CatalogManager();
        catMan.getFD(schema);

        return schema;
    }

    /**
     * Sets <code>this.relation</code> to the specified relation. Deregisters
     * the old relation's model from the controller and registers the new one,
     * respectively. Fires a property change event after registering the model,
     * so that the model changes are propagated to the according view.
     *
     * @param table the relation's name
     */
    public void loadRelation(Relation table) {

        if (this.relation != null) {
            controller.removeModel(relation);
        }

        this.relation = table;
        controller.addModel(relation);
        SortedSet<FDep> fdeps = relation.getFdeps();
        relation.firePropertyChange(
                DefaultController.ELEMENT_RELATION_FDEPS_PROPERTY,
                null,
                fdeps);
        relation.firePropertyChange(
                DefaultController.ELEMENT_RELATION_NF_PROPERTY,
                null,
                relation.getNormalform(false));

    }

    /**
     * Reloads the database schema. Neccessary after committing changes to the
     * schema.
     */
    private void reloadSchema() {

        controller.removeModel(schema);
        schema = new DbSchema("none");
        controller.addModel(schema);

        controller.removeModel(relation);
        relation = null;
        loadDB();
    }

    /**
     * Drops the fd_catalog relations from the database. Prior to this, all
     * triggers that represent a FD are deleted.
     */
    public void dropCatalog() {
        Map<Integer, String> fdMap = new HashMap<Integer, String>();

        Connection con = ConnectionManager.getConnection();
        Statement stmt;
        ResultSet rs = null;
        String getFDs = "SELECT id, relation FROM fd_catalog WHERE iskey = FALSE";

        try {
            stmt = con.createStatement();
            rs = stmt.executeQuery(getFDs);

            while (rs.next()) {
                fdMap.put(rs.getInt(1), rs.getString(2));
            }

            stmt.close();
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "dropCatalog",
                    "Couldn't fetch FDs from fd_catalog.", ex);
        }

        for (Map.Entry<Integer, String> entry : fdMap.entrySet()) {
            catMan.removeFD(entry.getKey(), entry.getValue(), con);
        }

        String dropIt = "DROP TABLE fd_catalog, " +
                "fd_catalog_lhs, " +
                "fd_catalog_rhs, " +
                "fd_catalog_trigger " +
                "CASCADE";

        try {
            stmt = con.createStatement();
            stmt.executeUpdate(dropIt);
            stmt.close();
        } catch (SQLException ex) {
            logger.logp(Level.SEVERE, SchemaManager.class.getName(),
                    "dropCatalog",
                    "Couldn't drop fd_catalog.", ex);
        }
    }

    /**
     * Discards the loaded schema and all included relations. Called when the
     * database connection is closed.
     */
    public void discard() {
        if (schema != null) {
            controller.removeModel(schema);
            schema = null;
        }

        if (relation != null) {
            controller.removeModel(relation);
            relation = null;
        }

        schema = new DbSchema("");
        controller.addModel(schema);
        schema.setSchemaName("No Connection");
    }
}
