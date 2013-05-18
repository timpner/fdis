package fd;

import java.util.Set;
import java.util.SortedSet;

/**
 * Represents a relation of a relational database created by the synthesize() algorithm
 * 
 * @author Matthias Meine <m.meine@tu-bs.de>
 * 
 */
public class SynRelation extends Relation {

    /** the fd from which the relation has been created while normalizing to 3NF */
    FDep originFD;
    /** name of the origin relation from which the SynRelation has been created while normalizing to 2NF*/
    String originName;

    public SynRelation(String name) {
        super(name);
        originFD = new FDep();
    }

    public SynRelation(Set<String> cols, String name) {
        super(cols, name);
        originFD = new FDep();
    }

    public SynRelation(SortedSet<FDep> fdeps, SortedSet<String> cols,
            String name) {
        super(fdeps, cols, name);
        originFD = new FDep();
    }
    
    public SynRelation(Relation r) {
    	super(r.getFdeps(),r.getColumns(),r.getRelationName());
    	originName = r.getRelationName();
    	originFD = new FDep();
    }

    /**
     * @return the originFD
     */
    public FDep getOriginFD() {
        return originFD;
    }

    /**
     * @param originFD the originFD to set
     */
    public void setOriginFD(FDep originFD) {
        this.originFD = originFD;
    }

    /**
     * @return the originName
     */
    public String getOriginName() {
        return originName;
    }

    /**
     * @param originName the originName to set
     */
    public void setOriginName(String originName) {
        this.originName = originName;
    }

    /**
     * @return the name
     */
    public String getName() {
        return super.getRelationName();
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        //String oldName = super.getRelationName();

        super.setRelationName(name);
    }
}
