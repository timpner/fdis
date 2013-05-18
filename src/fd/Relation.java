package fd;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import control.DefaultController;
import control.DefaultController.NF;

/**
 * Represents a relation of a relational database.
 * 
 * @author Matthias Meine <m.meine@tu-bs.de>
 * 
 */
public class Relation extends AbstractModel implements Comparable<Relation>,
		Normalizable {

	/** The name of the relation */
	private String relationName;
	/** The columns (attributes) of the relation */
	private SortedSet<String> columns;
	/** The functional dependencies of the relation */
	private SortedSet<FDep> fdeps;
	/**
	 * The functional dependencies that were added by the user, but not
	 * implemented yet
	 */
	private Set<FDep> additionalFdeps;
	/**
	 * The functional dependencies that were removed by the user, but are still
	 * implemented in the db
	 */
	private Set<FDep> removedFdeps;

	/**
	 * Constructs a new relation with a given relationName and empty sets for
	 * columns and functional dependencies
	 * 
	 * @param name
	 *            the name of the relation
	 */
	public Relation(String name) {
		setRelationName(name);
		this.columns = new TreeSet<String>();
		this.fdeps = new TreeSet<FDep>();
		this.additionalFdeps = new LinkedHashSet<FDep>();
		this.removedFdeps = new LinkedHashSet<FDep>();
	}

	/**
	 * Constructs a new relation with given relationName and columns and an
	 * empty set for functional dependencies
	 * 
	 * @param cols
	 *            the columns of the relation
	 * @param name
	 *            the name of the relation
	 */
	public Relation(Set<String> cols, String name) {
		setRelationName(name);
		setColumns(columns);
		this.fdeps = new TreeSet<FDep>();
		this.additionalFdeps = new LinkedHashSet<FDep>();
		this.removedFdeps = new LinkedHashSet<FDep>();
	}

	/**
	 * Constructs a new relation with given relationName, columns and fdeps
	 * 
	 * @param fdeps
	 *            the functional dependencies of the relation
	 * @param cols
	 *            the columns of the relation
	 * @param name
	 *            the name of the relation
	 */
	public Relation(SortedSet<FDep> fdeps, SortedSet<String> cols, String name) {
		setRelationName(name);
		setColumns(cols);
		setFdeps(fdeps);
		this.additionalFdeps = new LinkedHashSet<FDep>();
		this.removedFdeps = new LinkedHashSet<FDep>();
	}

	/**
	 * @return the relationName
	 */
	public String getRelationName() {
		return relationName;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setRelationName(String name) {
		String oldName = this.relationName;
		this.relationName = name;

		firePropertyChange(DefaultController.ELEMENT_RELATION_NAME_PROPERTY,
				oldName, relationName);
	}

	/**
	 * @return the columns
	 */
	public SortedSet<String> getColumns() {
		return columns;
	}

	/**
	 * @param columns
	 *            the columns to set
	 */
	public void setColumns(SortedSet<String> columns) {
		SortedSet<String> oldColumns = this.columns;
		this.columns = columns;

		firePropertyChange(DefaultController.ELEMENT_RELATION_COLUMNS_PROPERTY,
				oldColumns, columns);
	}

	/**
	 * @return the fdeps
	 */
	public SortedSet<FDep> getFdeps() {
		return fdeps;
	}

	/**
	 * @return the fdeps
	 */
	public SortedSet<FDep> getFdeps(boolean preview) {
		if (preview == false) {
			return fdeps;
		} else {
			SortedSet<FDep> allFDeps = new TreeSet<FDep>();
			allFDeps.addAll(fdeps);
			allFDeps.addAll(getAdditionalFdeps());
			HashSet<Integer> removedIds = new HashSet<Integer>();
			for (FDep rfd : removedFdeps) {
				removedIds.add(rfd.getId());
			}
			for (FDep fd : fdeps) {
				if (removedIds.contains(fd.getId())) {
					allFDeps.remove(fd);
				}
			}
			allFDeps.removeAll(removedFdeps);
			return allFDeps;
		}
	}

	/**
	 * @param fdeps
	 *            the fdeps to set
	 */
	public void setFdeps(SortedSet<FDep> fdeps) {
		SortedSet<FDep> oldFdeps = this.fdeps;
		this.fdeps = fdeps;

		firePropertyChange(DefaultController.ELEMENT_RELATION_FDEPS_PROPERTY,
				oldFdeps, fdeps);
	}

	/**
	 * @return the additionalFdeps
	 */
	public Set<FDep> getAdditionalFdeps() {
		return additionalFdeps;
	}

	/**
	 * @param additionalFdeps
	 *            the additionalFdeps to set
	 */
	public void setAdditionalFdeps(Set<FDep> additionalFdeps) {
		this.additionalFdeps = additionalFdeps;
	}

	/**
	 * @return the removedFdeps
	 */
	public Set<FDep> getRemovedFdeps() {
		return removedFdeps;
	}

	/**
	 * @param removedFdeps
	 *            the removedFdeps to set
	 */
	public void setRemovedFdeps(Set<FDep> removedFdeps) {
		this.removedFdeps = removedFdeps;
	}

	/**
	 * @return the normalform
	 */
	public String getNormalform(boolean preview) {
		String nf = "1 NF";
		if (is2NF(preview)) {
			nf = "2 NF";
			if (is3NF(preview)) {
				nf = "3 NF";
				if (isBCNF(preview)) {
					nf = "BCNF";
				}
			}
		}
		return nf;
	}

	/**
	 * adds a column to the set of colums
	 * 
	 * @param column
	 *            the column that shall be added
	 */
	public void addColumn(String column) {
		SortedSet<String> oldColumns = this.columns;
		columns.add(column);

		firePropertyChange(DefaultController.ELEMENT_RELATION_COLUMNS_PROPERTY,
				oldColumns, columns);
	}

	/**
	 * removes a column of the set of columns
	 * 
	 * @param column
	 *            the column that shall be removed
	 */
	public void removeColumn(String column) {
		SortedSet<String> oldColumns = this.columns;
		columns.remove(column);

		firePropertyChange(DefaultController.ELEMENT_RELATION_COLUMNS_PROPERTY,
				oldColumns, columns);
	}

	/**
	 * adds a functional dependency to the set of fdeps
	 * 
	 * @param fd
	 *            the dependency that shall be added
	 */
	public void addFDep(FDep fd) {
		Set<FDep> oldFdeps = this.fdeps;
		fdeps.add(fd);

		firePropertyChange(DefaultController.ELEMENT_RELATION_FDEPS_PROPERTY,
				oldFdeps, fdeps);

	}

	/**
	 * adds a functional dependency to the set of additionalFDeps
	 * 
	 * @param fd
	 *            the dependency that shall be added
	 */
	public void addAdditionalFdep(FDep fd) {
		additionalFdeps.add(fd);

		firePropertyChange(
				DefaultController.ELEMENT_RELATION_ADDITIONAL_FDEPS_PROPERTY,
				null, fd);
		firePropertyChange(
				DefaultController.ELEMENT_RELATION_NF_PREVIEW_PROPERTY, null,
				getNormalform(true));
	}

	/**
	 * adds a functional dependency to the set of removedFDeps
	 * 
	 * @param fd
	 *            the dependency that shall be added
	 */
	public void addRemovedFdep(FDep fd) {
		removedFdeps.add(fd);

		firePropertyChange(
				DefaultController.ELEMENT_RELATION_NF_PREVIEW_PROPERTY, null,
				getNormalform(true));
	}

	/**
	 * calculates candidate keys
	 * 
	 * @return all candidate keys
	 */
	public Set<Set<String>> determineKeys(boolean preview) {
		Set<Set<String>> k = new LinkedHashSet<Set<String>>();
		boolean isKey = false;
		Set<Set<String>> p = powerset(columns.toArray(new String[0]));

		for (Set<String> s : p) {

			// Wird Relation funktional bestimmt?
			isKey = true;
			Set<String> Huelle = this.xPlus(preview, s);
			if (!(Huelle.containsAll((columns)))) {
				isKey = false;
			}

			// Test auf Minimalitaet
			if (isKey) {
				LinkedHashSet<String> t = new LinkedHashSet<String>();
				for (String A : s) {
					t.clear();
					t.addAll(s);
					t.remove(A);
					Set<String> newHuelle = this.xPlus(preview, t);
					if ((newHuelle.containsAll(columns))) {
						isKey = false;
					}
				}
			}
			if (isKey) {
				k.add(s);
			}
		}
		return k;
	}

	/**
	 * calculates the attribute closure. All attributes that are functional
	 * determined by the given attributes.
	 * 
	 * @param preview
	 *            true if the function is used to generate a preview with
	 *            changed Set of FDs
	 * @param attributes
	 *            the attributes whose attribute closure shall be determined.
	 * @return the attribute closure
	 */
	public Set<String> xPlus(boolean preview, Set<String> attributes) {
		return xPlus(attributes, getFdeps(preview));
	}

	/**
	 * calculates the attribute closure. All attributes that are functional
	 * determined by the given attributes.
	 * 
	 * @param attributes
	 *            the attributes whose attribute closure shall be determined.
	 * @param fds
	 *            the FDs that shall be used to calculate the attribute closure
	 * @return the attribute closure
	 */
	public Set<String> xPlus(Set<String> attributes, Set<FDep> fds) {
		Set<String> Erg = new LinkedHashSet<String>();
		Erg.addAll(attributes);
		Set<String> Erg_old = new LinkedHashSet<String>();
		boolean Erg_changed = true;
		while (Erg_changed) {
			Erg_old.addAll(Erg);
			for (FDep f : fds) {
				if (Erg.containsAll(f.getLeftSide())) {
					Erg.addAll(f.getRightSide());
				}

			}
			Erg_changed = !Erg.equals(Erg_old);
		}
		return Erg;
	}

	/**
	 * Determines if the candidate is a key in this relation
	 * 
	 * @param candidate
	 *            Set of Strings that shall represent a key
	 * @return true if candidate is a key
	 */
	public boolean isKey(Set<String> candidate) {
		Set<String> attClosure = xPlus(false, candidate);
		if (attClosure.containsAll((columns))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Determines a 2NF Decomposition of this relation
	 * 
	 * @return a Decomposition of this relation
	 */
	private SortedSet<SynRelation> normalize2NF() {
		final String nl = System.getProperty("line.separator");

		// copy of fdeps and columns to rollback changes in the end
		SortedSet<FDep> originFDs = new TreeSet<FDep>();
		for (FDep f : fdeps) {
			originFDs.add(new FDep(f));
		}
		SortedSet<String> originColumns = new TreeSet<String>();
		for (String a : columns) {
			originColumns.add(new String(a));
		}

		SortedSet<SynRelation> newRelations = new TreeSet<SynRelation>();
		Set<String> y = new LinkedHashSet<String>();
		Set<String> xPlusVonY = new LinkedHashSet<String>();
		int i = 2;
		// initialize newRelations with this relation
		SynRelation r = new SynRelation(this);
		r.setName(this.getRelationName() + "_1");
		newRelations.add(r);
		// find partial dependencies for every relation in newRelations
		System.out.println("Initialize relation " + r + " with columns "
				+ r.getColumns() + nl);
		for (Set<String> x : r.determineKeys(false)) {
			for (String weg : x) {
				y.clear();
				y.addAll(x);
				y.remove(weg);
				xPlusVonY.clear();
				xPlusVonY.addAll(r.xPlus(false, y));
				Set<String> attsForY = new HashSet<String>();
				for (String z : r.determineNSAs(false)) {
					if (xPlusVonY.contains(z)) {
						attsForY.add(z);
						System.out.println("Non-key-attribute " + z
								+ " is only dependent from " + y
								+ " and not from " + x);
					}
				}
				if (!attsForY.isEmpty()) {
					// Create synRelation if partial dependency has been
					// detected
					SynRelation newRel = new SynRelation(this.getRelationName()
							+ "_" + i);
					newRel.setOriginName(this.getRelationName());
					FDep newFD = new FDep();
					i++;
					for (String a : attsForY) {
						newRel.addColumn(a);
						newFD.addRightSide(a);
						r.removeColumn(a);
						System.out.println(a + " removed from " + r);
					}
					for (String att : y) {
						newRel.addColumn(att);
						fdeps.add(newFD);
						newFD.addLeftSide(att);
					}

					System.out.println("New relation " + newRel
							+ " with columns " + newRel.getColumns()
							+ " created.");
					newRel.addFDep(newFD);
					newRelations.add(newRel);
				}
			}
		}

		// force garbage collection
		Runtime run = Runtime.getRuntime();
		run.gc();
		// summarize relations and distribute FDeps
		mergeRelations(newRelations);

		fdeps = decomposeRHS(fdeps);

		SortedSet<FDep> fdepscopy = new TreeSet<FDep>();
		for (FDep f : fdeps) {
			fdepscopy.add(new FDep(f));
		}

		fdeps = fdepscopy;

		distributeFDs(newRelations, fdepscopy);

		for (Relation rel : newRelations) {
			composeLHS(rel.getFdeps());
			for (FDep f : rel.getFdeps()) {
				if (rel.isKey(f.getLeftSide())) {
					f.setIsKey(true);
					break;
				}
			}
		}

		// remove changes of the original relation object
		fdeps = originFDs;
		columns = originColumns;

		return newRelations;
	}

	/**
	 * Composes functional dependencies with equal left sides
	 * 
	 * @param fds
	 *            the set of FDeps that shall be composed
	 */
	private static void composeLHS(Set<FDep> fds) {

		SortedSet<FDep> FDsToRemove = new TreeSet<FDep>();

		Iterator<FDep> it1 = fds.iterator();
		while (it1.hasNext()) {
			FDep fd1 = it1.next();
			Iterator<FDep> it2 = fds.iterator();
			while (it2.hasNext()) {
				FDep fd2 = it2.next();
				if (fd1.getLeftSide().equals(fd2.getLeftSide())
						&& !fd1.equals(fd2)) {
					fd1.addRightSide(fd2.getRightSide());
					FDsToRemove.add(fd2);
				}
			}
		}

		fds.removeAll(FDsToRemove);
	}

	/**
	 * Decomposes dependencies with several attributes on the right side into
	 * multiple dependencies with always only one attribute on the right side
	 * 
	 * @param fds
	 *            the set of FDeps that shall be decomposed
	 * @return a set of decomposed FDeps
	 */
	private static SortedSet<FDep> decomposeRHS(Set<FDep> fds) {

		Set<FDep> fdscopy = new HashSet<FDep>();
		SortedSet<FDep> decomposedFDs = new TreeSet<FDep>();
		for (FDep fd : fds) {
			fdscopy.add(new FDep(fd));
		}

		for (FDep fd : fdscopy) {
			for (String a : fd.getRightSide()) {
				FDep newFD = new FDep();
				newFD.addLeftSide(fd.getLeftSide());
				newFD.addRightSide(a);
				decomposedFDs.add(newFD);
			}
		}

		return decomposedFDs;
	}

	/**
	 * Removes dependencies that are contained within another relation of the
	 * given SynRelations
	 * 
	 * @param rels
	 *            the functional dependencies that shall be merged
	 */
	private static void mergeRelations(Set<SynRelation> rels) {

		Iterator<SynRelation> it1 = rels.iterator();
		Set<Relation> relsToRemove = new HashSet<Relation>();
		while (it1.hasNext()) {
			Relation rel1 = it1.next();
			Iterator<SynRelation> it2 = rels.iterator();
			while (it2.hasNext()) {
				Relation rel2 = it2.next();
				if (!rel1.equals(rel2)) {
					if (rel1.getColumns().containsAll(rel2.getColumns())
							&& !relsToRemove.contains(rel1)) {
						relsToRemove.add(rel2);
					}
				}
			}
		}
		rels.removeAll(relsToRemove);
	}

	/**
	 * distributes a set of FDeps to a set of relations
	 * 
	 * @param rels
	 *            the relations which shall get the distributed FDeps
	 * @param divideFDs
	 *            the FDeps that shall be distributed
	 */
	private static void distributeFDs(Set<SynRelation> rels, Set<FDep> divideFDs) {

		SortedSet<FDep> fds = new TreeSet<FDep>();
		// copy all fds
		for (FDep fa : divideFDs) {
			fds.add(new FDep(fa));
		}
		// clear old FDs
		for (SynRelation r : rels) {
			r.getFdeps().clear();
		}
		// Distribute new FDs
		Set<String> allAtts = new HashSet<String>();
		for (FDep f : fds) {
			for (String lhs : f.getLeftSide()) {
				allAtts.add(lhs);
			}
			for (String rhs : f.getRightSide()) {
				allAtts.add(rhs);
			}
			for (SynRelation r : rels) {
				if (r.getColumns().containsAll(allAtts)) {
					r.addFDep(f);
				}
			}
			allAtts.clear();
		}
		divideFDs = fds;
	}

	/**
	 * Calculates the CanonicalCover for the functional dependencies of the
	 * relation
	 * 
	 * @return the Canonical Cover
	 */
	private SortedSet<FDep> getCanonicalCover() {

		// Step 1 reduction of the left side
		SortedSet<FDep> fds = new TreeSet<FDep>();
		for (FDep fd : getFdeps()) {
			fds.add(new FDep(fd));
		}
		FDep newFD = null;
		Set<String> closure = null;

		for (FDep fd : fds) {
			newFD = new FDep(fd);
			for (String a : fd.getLeftSide()) {
				newFD.removeLeftSide(a);
				closure = xPlus(false, newFD.getLeftSide());
				if (closure.containsAll(newFD.getRightSide())) {
					fd.removeLeftSide(a);
				}
				newFD.addLeftSide(a);
			}
		}

		// Step 2 reduction of the right side
		for (FDep fd : fds) {
			for (String b : fd.getRightSide()) {

				fd.removeRightSide(b);
				closure = xPlus(fd.getLeftSide(), fds);
				if (!closure.contains(b)) {
					fd.addRightSide(b);
				} else {
				}
			}
		}

		// Step 3 remove dependencies with empty right side
		Set<FDep> FDtoDel = new HashSet<FDep>();
		for (FDep fd : fds) {
			if (fd.getRightSide().isEmpty()) {
				FDtoDel.add(fd);
			}
		}
		fds.removeAll(FDtoDel);

		// Step 4 summarize dependencies with same left side
		Iterator<FDep> it1 = fds.iterator();
		FDtoDel.clear();
		while (it1.hasNext()) {
			FDep fd1 = it1.next();
			Iterator<FDep> it2 = fds.iterator();
			while (it2.hasNext()) {
				FDep fd2 = it2.next();
				if (fd2.getLeftSide().equals(fd1.getLeftSide())
						&& !fd1.equals(fd2) && !FDtoDel.contains(fd1)) {
					fd1.addRightSide(fd2.getRightSide());
					FDtoDel.add(fd2);
				}
			}
		}		
		fds.removeAll(FDtoDel);

		return fds;
	}

	/**
	 * determines all attributes that are not part of a candidate key
	 * 
	 * @return a set of all NSAs
	 */
	public Set<String> determineNSAs(boolean preview) {
		Set<String> nsas = new LinkedHashSet<String>();
		for (String att : columns) {
			nsas.add(new String(att));
		}
		for (Set<String> key : determineKeys(preview)) {
			nsas.removeAll(key);
		}

		// force garbage collection
		Runtime r = Runtime.getRuntime();
		r.gc();

		return nsas;
	}

	/**
	 * decides whether a relation is in 2NF or not
	 * 
	 * @return true if relation is in 2NF
	 */
	private boolean is2NF(boolean preview) {
		boolean in2NF = true;
		Set<String> y = new LinkedHashSet<String>();
		Set<String> xPlusVonY = new LinkedHashSet<String>();
		Set<String> allNSAs = determineNSAs(preview);

		for (Set<String> x : determineKeys(preview)) {
			for (String weg : x) {
				y.clear();
				y.addAll(x);
				y.remove(weg);
				xPlusVonY.addAll(this.xPlus(preview, y));
				for (String z : allNSAs) {
					if (xPlusVonY.contains(z)) {
						in2NF = false;
					}
				}
			}
		}

		// force garbage collection
		Runtime r = Runtime.getRuntime();
		r.gc();

		return in2NF;
	}

	/**
	 * decides whether a relation is in 3NF or not
	 * 
	 * @return true if relation is in 3NF
	 */
	private boolean is3NF(boolean preview) {
		boolean in3NF = true;
		SortedSet<FDep> decomposedFDs = decomposeRHS(getFdeps(preview));
		for (FDep fd : decomposedFDs) {

			boolean conditionAccomplished = false;

			// check if fd is trivial
			if (fd.getLeftSide().containsAll(fd.getRightSide())) {
				conditionAccomplished = true;
			}

			// check if B is part of a key candidate
			Set<String> allNSAs = determineNSAs(preview);
			if (!allNSAs.containsAll(fd.getRightSide())) {
				conditionAccomplished = true;
			}

			// check if leftSide determines all attributes
			if (xPlus(preview, fd.getLeftSide()).containsAll(columns)) {
				conditionAccomplished = true;
			}

			// relation is not in 3NF if one fd does not accomplish any
			// condition
			if (conditionAccomplished == false) {
				in3NF = false;
			}
		}

		return in3NF;
	}

	/**
	 * decides whether a relation is in BCNF or not
	 * 
	 * @return true if relation is in BCNF
	 */
	private boolean isBCNF(boolean preview) {
		boolean inBCNF = true;
		Set<Set<String>> dets = new LinkedHashSet<Set<String>>();
		Set<Set<String>> ckeys = determineKeys(preview);
		for (String att : columns) {
			dets.clear();
			dets.addAll(this.getDeterminante(preview, att));
			for (Set<String> det : dets) {
				if (!ckeys.contains(det)) {
					inBCNF = false;
				}
			}
		}

		// force garbage collection
		Runtime r = Runtime.getRuntime();
		r.gc();

		return inBCNF;
	}

	/**
	 * Synthesizes a relation into 3NF
	 * 
	 * @return a set of SynRelation with every SynRelation in 3NF
	 */
	private SortedSet<SynRelation> synthesize() {
		final String nl = System.getProperty("line.separator");
		SortedSet<SynRelation> newRelations = new TreeSet<SynRelation>();
		// Step 1: get canonical cover
		System.out.println("Step 1 (Canonical Cover)");
		Set<FDep> fc = getCanonicalCover();
		System.out.println(fc);
		Iterator<FDep> it = fc.iterator();
		Set<Set<String>> OldCKeys = this.determineKeys(false);

		Set<Set<String>> AllNewRelCKeys = new HashSet<Set<String>>();
		// Step 2: create Relation for every FD and assign corresponding FD
		System.out
				.println(nl
						+ "Step 2 (Create a new relation for each FD in the canonical cover)");
		SynRelation rel = new SynRelation("");
		int i = 1;
		while (it.hasNext()) {
			FDep fd = it.next();
			rel = new SynRelation(this.relationName + "_" + i);
			rel.setOriginFD(fd);
			rel.setOriginName(this.relationName);
			for (String a : fd.getLeftSide()) {
				rel.addColumn(a);
			}
			for (String b : fd.getRightSide()) {
				rel.addColumn(b);
			}
			System.out.println("Relation created " + rel + " with columns "
					+ rel.getColumns());
			// add FDs to relation
			SortedSet<String> faAttributes = new TreeSet<String>();
			for (FDep fdToAdd : fc) {
				for (String a : fdToAdd.getLeftSide()) {
					faAttributes.add(a);
					for (String b : fdToAdd.getRightSide()) {
						faAttributes.add(b);
						if (rel.getColumns().containsAll(faAttributes)) {
							FDep newfd = new FDep();
							newfd.addLeftSide(fdToAdd.getLeftSide());
							newfd.addRightSide(b);
							rel.addFDep(newfd);
						}
						faAttributes.remove(b);
					}
					faAttributes.clear();
				}

			}
			
			composeLHS(rel.getFdeps());
			// set isKey attribute on FDep
			for (FDep eqFDep : rel.getFdeps()) {
				if (eqFDep.compareTo(rel.getOriginFD()) == 0) {
					eqFDep.setIsKey(true);
				}
			}
			newRelations.add(rel);
			
			// Step 3:
			Set<Set<String>> NewRelCKeys = rel.determineKeys(false);
			AllNewRelCKeys.addAll(NewRelCKeys);
			i++;

		}
		System.out
				.println(nl
						+ "Step 3 (Create a new relation if no relation contains a key candidate of the origin relation)");
		AllNewRelCKeys.retainAll(OldCKeys);
		if (AllNewRelCKeys.isEmpty()) {
			// find a key
			Set<String> ckey = new TreeSet<String>();
			for (Set<String> key : OldCKeys) {
				ckey = key;
				break;
			}
			SynRelation keyRelation = new SynRelation("");
			// generate name and add attributes
			for (String s : ckey) {
				keyRelation.addColumn(s);
			}
			keyRelation.setRelationName(this.relationName + "_" + i);
			FDep fa = new FDep();
			for (String a : keyRelation.getColumns()) {
				fa.addLeftSide(new String(a));
				fa.addRightSide(new String(a));
			}
			fa.setIsKey(true);
			keyRelation.setOriginFD(fa);
			keyRelation.setOriginName(this.relationName);
			keyRelation.addFDep(fa);
			newRelations.add(keyRelation);
			System.out.println("Relation created: " + keyRelation
					+ " with columns: " + keyRelation.getColumns());
		} else {
			System.out.println("No new relation created.");
		}

		// Step 4: remove unnecessary relations
		System.out.println(nl
				+ "Step 4 (Remove relations that are included in others)");
		Iterator<SynRelation> it1 = newRelations.iterator();
		Set<Relation> relsToRemove = new HashSet<Relation>();
		while (it1.hasNext()) {
			Relation rel1 = it1.next();
			Iterator<SynRelation> it2 = newRelations.iterator();
			while (it2.hasNext()) {
				Relation rel2 = it2.next();
				if (!rel1.equals(rel2)) {
					if (rel1.getColumns().containsAll(rel2.getColumns())
							&& !relsToRemove.contains(rel1)) {
						System.out.println(rel2 + " removed because " + rel1
								+ " contains it.");
						relsToRemove.add(rel2);
					}
				}
			}
		}
		if (relsToRemove.isEmpty()) {
			System.out.println("No relation has been removed.");
		}
		newRelations.removeAll(relsToRemove);

		// force garbage collection
		Runtime r = Runtime.getRuntime();
		r.gc();

		return newRelations;
	}

	/**
	 * 
	 * @param att
	 *            the attribute whose determinant shall be calculated
	 * @return a set of all determinants of att
	 */
	private Set<Set<String>> getDeterminante(boolean preview, String att) {
		Set<Set<String>> determinanten = new LinkedHashSet<Set<String>>();
		for (FDep f : getFdeps(preview)) {
			if (f.getRightSide().contains(att)) {
				determinanten.add(f.getLeftSide());
			}
		}
		return determinanten;
	}

	/**
	 * Returns the power set from the given set by using a binary counter
	 * Example: S = {a,b,c} P(S) = {[], [c], [b], [b, c], [a], [a, c], [a, b],
	 * [a, b, c]} The idea of the method has been taken from
	 * http://jvalentino.blogspot.com/2007
	 * /02/shortcut-to-calculating-power-set-using.html
	 * 
	 * @param set
	 *            an array whose powerset will be calculated
	 * @return a LinkedHashSet representing the powerset
	 */
	public static LinkedHashSet<Set<String>> powerset(String[] set) {

		LinkedHashSet<Set<String>> power = new LinkedHashSet<Set<String>>();
		int elements = set.length;

		// power set has 2^n elements
		int elementsInPower = (int) Math.pow(2, elements);

		// for each element in the powerset, create a number n and convert it to
		// binary representation and insert '0's in the beginning until the
		// binary's length = number of elements in the given set
		for (int n = 0; n < elementsInPower; n++) {

			// convert to binary representation and fill with '0's
			StringBuffer binary = new StringBuffer(Integer.toBinaryString(n));
			for (int i = binary.length(); i < elements; i++) {
				binary.insert(0, "0");
			}

			LinkedHashSet<String> subSet = new LinkedHashSet<String>();

			// create a new subset with this rule:
			// a '1' in the binary representation on position i means that
			// element i of the given set will be added
			for (int i = 0; i < binary.length(); i++) {

				if (binary.charAt(i) == '1') {
					subSet.add(set[i]);
				}

			}

			// the new subset is added to the powerset
			power.add(subSet);

		}

		return power;
	}

	/**
	 * Creates a LinkedHashSet containing all permutations of fdeps
	 * 
	 * @return a LinkedHashSet containing all permutations of fdeps
	 */
	private LinkedHashSet<LinkedList<FDep>> getAllFDepPermutations() {
		int[] indices;
		FDep[] elements = (FDep[]) fdeps.toArray(new FDep[0]);
		PermutationGenerator pg = new PermutationGenerator(elements.length);
		LinkedList<FDep> permutation;
		LinkedHashSet<LinkedList<FDep>> allPermutations = new LinkedHashSet<LinkedList<FDep>>();

		// create a permutation for every element in pg
		while (pg.hasMore()) {
			permutation = new LinkedList<FDep>();
			indices = pg.getNext();
			for (int i = 0; i < indices.length; i++) {
				permutation.add(new FDep(elements[indices[i]]));
			}
			allPermutations.add(permutation);
		}

		return allPermutations;
	}

	/**
	 * Calculates all CanonicalCovers by using every permutation of fdeps
	 * 
	 * @return a set that contains all CanonicalCovers
	 */
	private LinkedHashSet<LinkedHashSet<FDep>> getAllCanonicalCovers() {

		LinkedHashSet<LinkedHashSet<FDep>> allCanonicalCovers = new LinkedHashSet<LinkedHashSet<FDep>>();

		LinkedHashSet<LinkedList<FDep>> allPermutations = getAllFDepPermutations();

		for (LinkedList<FDep> permutation : allPermutations) {

			// Step 1 reduction of the left side
			LinkedHashSet<FDep> fds = new LinkedHashSet<FDep>(permutation);
			FDep newFD = null;
			Set<String> closure = null;

			for (FDep fd : fds) {
				newFD = new FDep(fd);
				for (String a : fd.getLeftSide()) {
					newFD.removeLeftSide(a);
					closure = xPlus(false, newFD.getLeftSide());
					if (closure.containsAll(newFD.getRightSide())) {
						fd.removeLeftSide(a);
					}
					newFD.addLeftSide(a);
				}
			}

			// Step 2 reduction of the right side
			for (FDep fd : fds) {
				for (String b : fd.getRightSide()) {

					fd.removeRightSide(b);
					closure = xPlus(fd.getLeftSide(), fds);
					if (!closure.contains(b)) {
						fd.addRightSide(b);
					}
				}
			}

			// Step 3 remove dependencies with empty right side
			Set<FDep> FDtoDel = new HashSet<FDep>();
			for (FDep fd : fds) {
				if (fd.getRightSide().isEmpty()) {
					FDtoDel.add(fd);
				}
			}
			fds.removeAll(FDtoDel);

			// Step 4 remove dependencies that are contained within another
			Iterator<FDep> it1 = fds.iterator();
			FDtoDel.clear();

			while (it1.hasNext()) {
				FDep fd1 = it1.next();
				Iterator<FDep> it2 = fds.iterator();
				while (it2.hasNext()) {
					FDep fd2 = it2.next();
					if (fd1.getLeftSide().equals(fd2.getLeftSide())
							&& !fd1.equals(fd2) && !FDtoDel.contains(fd1)) {
						fd1.addRightSide(fd2.getRightSide());
						FDtoDel.add(fd2);
					}
				}
			}
			fds.removeAll(FDtoDel);
			allCanonicalCovers.add(fds);

		}
		return allCanonicalCovers;
	}

	/**
	 * Returns a string representation of the relation
	 * 
	 * @return a string representation of the relation
	 */
	public String toString() {
		return this.relationName;
	}

	public int compareTo(Relation o) {
		int cmp = relationName.compareTo(o.relationName);
		return cmp;
	}

	/**
	 * Calls the needed method for the given NF form
	 * 
	 * @param form
	 *            the normal form that shall be reached
	 * @return a set of SynRelations that are in the given NF form
	 */
	public SortedSet<SynRelation> normalize(NF form) {
		SortedSet<SynRelation> syn = null;
		switch (form) {
		case NF2:
			syn = normalize2NF();
			break;
		case NF3:
			syn = synthesize();
			break;
		}
		return syn;
	}
}
