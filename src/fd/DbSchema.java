package fd;

import control.DefaultController;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a relational database schema
 *  
 * @author Matthias Meine <m.meine@tu-bs.de>
 * @author Julian Timpner <j.timpner@tu-bs.de>
 */
public class DbSchema extends AbstractModel {

    /** The schemaName of the schema */
    private String schemaName;
    /** The relations of the schema */
    private SortedSet<Relation> relations;

    /**
     * Constructs a new DbSchema with a given name and an empty set of relations
     *
     * @param name
     *            the name of the schema
     */
    public DbSchema(String name) {
        setSchemaName(name);
        this.relations = new TreeSet<Relation>();
    }

    /**
     * Constructs a new DbSchema with a given schemaName and a set of relations
     *
     * @param relations
     *            the relations of the schema
     * @param name
     *            the name of the schema
     */
    public DbSchema(SortedSet<Relation> relations, String name) {
        setSchemaName(name);
        setRelations(relations);
    }

    /**
     * @return the schemaName
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setSchemaName(String name) {
        String oldName = this.schemaName;
        this.schemaName = name;

        firePropertyChange(
                DefaultController.ELEMENT_SCHEMA_NAME_PROPERTY,
                oldName, name);
    }

    /**
     * @return the relations
     */
    public Set<Relation> getRelations() {
        return relations;
    }

    /**
     * @param relations
     *            the relations to set
     */
    public void setRelations(SortedSet<Relation> relations) {
        SortedSet<Relation> oldRelations = this.relations;
        this.relations = relations;

        firePropertyChange(
                DefaultController.ELEMENT_SCHEMA_RELATIONS_PROPERTY,
                oldRelations, relations);

    }

    /**
     * Adds a relation to the DbSchema
     *
     * @param rel
     *            the relation that shall be added
     */
    public void addRelation(Relation rel) {
        SortedSet<Relation> oldRelations = this.relations;
        relations.add(rel);

        firePropertyChange(
                DefaultController.ELEMENT_SCHEMA_RELATIONS_PROPERTY,
                oldRelations, relations);
    }

    /**
     * Returns a string representation of a database schema
     */
    @Override
    public String toString() {
        StringBuffer returner = new StringBuffer();
        returner.append(schemaName + "\n");
        for (Relation rel : relations) {
            returner.append("\n" + rel.toString());
        }
        return returner.toString();
    }
}
