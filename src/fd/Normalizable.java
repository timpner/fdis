package fd;

import control.DefaultController.NF;
import java.util.Set;
import java.util.SortedSet;

/**
 * This interface defines the funcionality that a <code>Relation</code> has to
 * provide in order to be normalizable.
 * 
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.0
 */
public interface Normalizable {

    /**
     * Determines the normalform for this relation.
     *
     * @param preview true, if temporary FDs should be taken into account
     * @return the normalform
     */
    String getNormalform(boolean preview);

    /**
     * Decomposes this <code>Relation</code> into a set of relations in 2NF or
     * 3NF, as specified by <code>form</code>.
     *
     * @return a set of <code>Relation</code>s in normal <code>form</code>
     */
    SortedSet<SynRelation> normalize(NF form);

    /**
     * Return the relation's attributes.
     * 
     * @return the columns
     */
    SortedSet<String> getColumns();

    /**
     * Return the relation's name.
     * @return the name
     */
    String getRelationName();

    /**
     * Adds a temporary FD to the relation.
     * @param fd
     */
    void addAdditionalFdep(FDep fd);

    /**
     * Adds a FD to the relation's set of fdeps.
     *
     * @param fd
     */
    void addFDep(FDep fd);

    /**
     * Temporary marks the specified FD as to be deleted.
     * 
     * @param fd
     */
    void addRemovedFdep(FDep fd);

    /**
     * Returns all temporary added FDs.
     *
     * @return the additional FDs
     */
    Set<FDep> getAdditionalFdeps();

    /**
     * Returns the relation's FDs.
     * 
     * @return the FDs
     */
    SortedSet<FDep> getFdeps();

    /**
     * Returns all temporary deleted FDs.
     *
     * @return the removed FDs
     */
    Set<FDep> getRemovedFdeps();

    /**
     * Determines if the candidate is a key in this relation.
     *
     * @param candidate set of Strings that may represent a key
     * @return true if candidate is a key
     */
    boolean isKey(Set<String> candidate);

    /**
     * Sets the temporary added FDs.
     *
     * @param additionalFdeps the temporary FDs
     */
    void setAdditionalFdeps(Set<FDep> additionalFdeps);

    /**
     * Sets the relation's columns.
     * 
     * @param columns
     */
    void setColumns(SortedSet<String> columns);

    /**
     * Sets the temporary deleted FDs.
     *
     * @param removedFdeps the removed FDs
     */
    void setRemovedFdeps(Set<FDep> removedFdeps);

    /**
     * Calculates the attribute closure, i.e. all attributes that are
     * functionally determined by the given attributes.
     *
     * @param preview true if temporary FDs shall be taken into account
     * @param attributes the attributes whose attribute closure is to be determined.
     * @return the attribute closure
     */
    Set<String> xPlus(boolean preview, Set<String> attributes);
}
