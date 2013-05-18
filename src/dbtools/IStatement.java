package dbtools;

import java.sql.Connection;
import java.util.Map;
import java.util.Set;

import fd.FDep;
import fd.SynRelation;

/**
 * This interface defines the functionality for the SQL statement generation.
 *
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.0
 */
public interface IStatement {

    /**
     * Generates a SQL statement that adds a foreign key to a given table.
     *
     * @param referencedTable
     * @param referencingTable
     * @param refMap
     * @param onUpdate
     * @param onDelete
     * @return a SQL statemtent for adding a foreign key constraint
     */
    String generateAddConstraint(String referencedTable,
            String referencingTable, Map<String, String> refMap,
            String onUpdate, String onDelete);

    /**
     * Generates a SQL statement that creates a table specified by the given
     * typeMap.
     *
     * @param typeMap a map that maps attributes to datatypes
     * @param srel the SynRelation to create
     * @return a SQL statment for creating a specific table
     */
    String generateCreateTable(Map<String, String> typeMap, SynRelation srel);

    /**
     * Generates a SQL statement that implements a trigger function.
     *
     * @param fd the FD that is to be implemented by the function
     * @param rname the relation's name
     * @param fname the trigger function's name
     * @return a SQL statement that implements the function
     */
    String generateTFunction(FDep fd, String rname, String fname);

    /**
     * Generates a name for a trigger function.
     *
     * @param id the ID of the FD in the catalog that the function is for
     * @param rname the relation's name
     * @return a name for the trigger function
     */
    String generateTFunctionName(int id, String rname);

    /**
     * Generates a SQL statement that implements a trigger.
     *
     * @param id the ID of the FD in the catalog
     * @param rname the relation's name
     * @param fname the name of the trigger function
     * @return a SQL statement that implements a trigger
     */
    String generateTrigger(int id, String rname, String fname);

    /**
     * Generates a name for a new trigger.
     *
     * @param id the ID of the FD in the catalog that the trigger is for
     * @param rname the relation's name
     * @return a name for the trigger
     */
    String generateTriggerName(int id, String rname);

    /**
     * Generates intra-relation foreign keys for new relations.
     *
     * @param newRelations a set of new relations
     * @return a set of SQL statements for adding foreign keys
     */
    Set<String> generateIntraFKeys(Set<SynRelation> newRelations, Connection con);
}
