package fd;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Represents a functional dependency
 * 
 * @author Matthias Meine <m.meine@tu-bs.de>
 * 
 */
public class FDep extends AbstractModel implements Comparable<FDep>, Cloneable {

	/** The left side of a functional dependency */
	private SortedSet<String> leftSide;
	/** The right side of a functional dependency */
	private SortedSet<String> rightSide;
	/** Denotes if a FDep represents a Key */
	private boolean isKey;
	/** The id with which the FDep is saved in the catalog */
	private int id;

	/**
	 * Constructs a new functional dependency with empty HashSets for the left
	 * and right side of a dependency
	 */
	public FDep() {
		leftSide = new ConcurrentSkipListSet<String>();
		rightSide = new ConcurrentSkipListSet<String>();
	}

	/**
	 * Constructs a new functional dependency with empty HashSets for the left
	 * and right side of a dependency and specifies isKey
	 */
	public FDep(boolean isKey) {
		leftSide = new ConcurrentSkipListSet<String>();
		this.isKey = isKey;
		rightSide = new ConcurrentSkipListSet<String>();
	}

	/**
	 * Constructs a new functional dependency with given left and right Side of
	 * the dependency
	 * 
	 * @param left
	 *            the left side of a dependency
	 * @param right
	 *            the right side of a dependency
	 */
	public FDep(SortedSet<String> left, SortedSet<String> right) {
		leftSide = left;
		rightSide = right;
	}

	/**
	 * Constructs a new functional dependency from another FD with same values
	 * but different references
	 * 
	 * @param fd
	 *            the fd that shall be copied
	 */
	public FDep(FDep fd) {
		this.leftSide = new ConcurrentSkipListSet<String>();
		for (String a : fd.getLeftSide()) {
			this.leftSide.add(new String(a));
		}
		this.rightSide = new ConcurrentSkipListSet<String>();
		for (String b : fd.getRightSide()) {
			this.rightSide.add(new String(b));
		}
		this.isKey = fd.getIsKey();
		this.id = fd.getId();
	}

	/**
	 * @return the leftSide
	 */
	public SortedSet<String> getLeftSide() {
		return leftSide;
	}

	/**
	 * @param leftSide
	 *            the leftSide to set
	 */
	public void setLeftSide(SortedSet<String> leftSide) {
		this.leftSide = leftSide;
	}

	/**
	 * @return the rightSide
	 */
	public SortedSet<String> getRightSide() {
		return rightSide;
	}

	/**
	 * @param rightSide
	 *            the rightSide to set
	 */
	public void setRightSide(SortedSet<String> rightSide) {
		this.rightSide = rightSide;
	}

	/**
	 * @return the isKey
	 */
	public boolean getIsKey() {
		return isKey;
	}

	/**
	 * @param isKey
	 *            the isKey to set
	 */
	public void setIsKey(boolean isKey) {
		this.isKey = isKey;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Adds an attribute to the Left Side of a FDep
	 * 
	 * @param a
	 *            the attribute that shall be added
	 */
	public void addLeftSide(String a) {
		this.leftSide.add(a);
	}

	/**
	 * Adds an attribute to the right Side of a FDep
	 * 
	 * @param a
	 *            the attribute that shall be added
	 */
	public void addRightSide(String a) {
		this.rightSide.add(a);
	}

	/**
	 * Adds a collection of attributes to the Left Side of a FDep
	 * 
	 * @param c
	 *            the collection of attributes that shall be added
	 */
	public void addLeftSide(Collection<String> c) {
		this.leftSide.addAll(c);
	}

	/**
	 * Adds a collection of attributes to the Right Side of a FDep
	 * 
	 * @param c
	 *            the collection of attributes that shall be added
	 */
	public void addRightSide(Collection<String> c) {
		this.rightSide.addAll(c);
	}

	/**
	 * Removes an attribute of the Left Side of a FDep
	 * 
	 * @param a
	 *            the attribute that shall be removed
	 */
	public void removeLeftSide(String a) {
		this.leftSide.remove(a);
	}

	/**
	 * Removes an attribute of the Right Side of a FDep
	 * 
	 * @param a
	 *            the attribute that shall be removed
	 */
	public void removeRightSide(String a) {
		this.rightSide.remove(a);
	}

	/**
	 * Removes a collection of attributes of the Left Side of a FDep
	 * 
	 * @param c
	 *            the collection of attributes that shall be removed
	 */
	public void removeLeftSide(Collection<String> c) {
		this.leftSide.removeAll(c);
	}

	/**
	 * Removes a collection of attributes of the Right Side of a FDep
	 * 
	 * @param c
	 *            the collection of attributes that shall be removed
	 */
	public void removeRightSide(Collection<String> c) {
		this.rightSide.removeAll(c);
	}

	/**
	 * Compares this object with the specified object for order.
	 * 
	 * @param o
	 *            the Object to be compared.
	 * 
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 */
	public int compareTo(FDep o) {
		Iterator<String> iter1l = leftSide.iterator();
		Iterator<String> iter2l = o.leftSide.iterator();

		Iterator<String> iter1r = rightSide.iterator();
		Iterator<String> iter2r = o.rightSide.iterator();

		String s, t;
		int cmp = 0;

		// Compare left-hand sides.
		while (iter1l.hasNext() && iter2l.hasNext()) {
			s = iter1l.next();
			t = iter2l.next();

			cmp = s.compareTo(t);
			if (cmp == 0) {
				continue;
			} else {
				return cmp;
			}
		}

		if (iter1l.hasNext() && !iter2l.hasNext()) {
			return 1;
		} else if (!iter1l.hasNext() && iter2l.hasNext()) {
			return -1;
		}

		// Compare right-hand sides.
		while (iter1r.hasNext() && iter2r.hasNext()) {
			s = iter1r.next();
			t = iter2r.next();

			cmp = s.compareTo(t);
			if (cmp == 0) {
				continue;
			} else {
				return cmp;
			}
		}
		if (iter1r.hasNext() && !iter2r.hasNext()) {
			return 1;
		} else if (!iter1r.hasNext() && iter2r.hasNext()) {
			return -1;
		}

		return 0;
	}

	/**
	 * Returns a string representation of a functional dependency
	 */
	public String toString() {
		StringBuffer ls = new StringBuffer();
		StringBuffer rs = new StringBuffer();
		for (String a : leftSide) {
			ls.append(" " + a);
		}

		for (String b : rightSide) {
			rs.append(" " + b);
		}
		String left = ls.toString();
		String right = rs.toString();
		left = left.trim();
		right = right.trim();
		return (left + " -> " + right);
	}
}
