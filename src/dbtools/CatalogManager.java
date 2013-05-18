package dbtools;

import fd.DbSchema;
import fd.FDep;
import fd.Relation;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>CatalogManager</code> class is responsible for storing and retrieving
 * functional dependencies in the fd_catalog user relations.
 * 
 * @author Julian Timpner <j.timpner@tu-bs.de>
 * @version 1.0
 */
public class CatalogManager {

	/** The loaded database schema. */
	private DbSchema schema;
	/** The global <code>logger</code> object. */
	private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/**
	 * Sole constructor.
	 */
	public CatalogManager() {
	}

	/**
	 * Adds a functional dependency (FD) to a given relation. This includes the
	 * generation of a trigger and a function, that implement the FD as a
	 * dynamic constraint on the database, as well as adding the FD to the
	 * fd_catalog user relations.
	 * 
	 * @param table
	 *            the relation in that the FD is valid
	 * @param fd
	 *            a functional dependency
	 * @param con
	 *            a database connection
	 */
	public void addFD(String table, FDep fd, Connection con) {
		if (isUnique(table, fd, con)) {
			insert(table, fd.getLeftSide(), con);
		} else {
			/*
			 * The SQL statements as Strings.
			 */
			String insertIntoCatalog = "INSERT INTO fd_catalog (relation, iskey) VALUES(?, ?)";
			String insertLHS = "INSERT INTO fd_catalog_lhs VALUES(?, ?)";
			String insertRHS = "INSERT INTO fd_catalog_rhs VALUES(?, ?)";
			String insertTrigger = "INSERT INTO fd_catalog_trigger VALUES(?, ?, ?)";
			String getID = "SELECT MAX(id) FROM fd_catalog WHERE relation = ?";

			PreparedStatement pstmt = null;
			PreparedStatement pstmtFunc = null;
			PreparedStatement pstmtTrigger = null;

			ResultSet rs = null;

			int id = -1;

			IStatement gen = new StatementGenerator();
			String funcName = null;
			String func = null;
			String trigger = null;
			String triggerName = null;

			try {
				// Insert fd into fd_catalog.
				pstmt = con.prepareStatement(insertIntoCatalog);
				pstmt.setString(1, table);
				pstmt.setBoolean(2, fd.getIsKey());
				pstmt.executeUpdate();

				// Get id for the inserted fd.
				pstmt = con.prepareStatement(getID);
				pstmt.setString(1, table);
				rs = pstmt.executeQuery();

				while (rs.next()) {
					id = rs.getInt(1);
				}

				rs.close();

				if (!fd.getIsKey()) {
					/*
					 * Generate trigger and trigger function.
					 */
					funcName = gen.generateTFunctionName(id, table);
					func = gen.generateTFunction(fd, table, funcName);
					triggerName = gen.generateTriggerName(id, table);
					trigger = gen.generateTrigger(id, table, funcName);

					// Insert trigger into fd_catalog_trigger.
					pstmt = con.prepareStatement(insertTrigger);
					pstmt.setInt(1, id);
					pstmt.setString(2, triggerName);
					pstmt.setString(3, funcName);

					pstmt.executeUpdate();

					// Implement trigger and trigger function.
					pstmtFunc = con.prepareStatement(func);
					pstmtTrigger = con.prepareStatement(trigger);

					pstmtFunc.executeUpdate();
					pstmtTrigger.executeUpdate();

					pstmtFunc.close();
					pstmtTrigger.close();
				}

				/*
				 * For each left-hand side attribute, insert a tupel into
				 * fd_catalog_lhs.
				 */
				pstmt = con.prepareStatement(insertLHS);
				Set<String> columns = fd.getLeftSide();
				for (String column : columns) {

					pstmt.setInt(1, id);
					pstmt.setString(2, column);
					pstmt.executeUpdate();

				}

				/*
				 * For each right-hand side attribute, insert a tupel into
				 * fd_catalog_rhs.
				 */
				pstmt = con.prepareStatement(insertRHS);
				columns = fd.getRightSide();
				for (String column : columns) {

					pstmt.setInt(1, id);
					pstmt.setString(2, column);
					pstmt.executeUpdate();

				}

				// Close ResultSets and Statements.
				pstmt.close();

			} catch (SQLException ex) {
				logger.logp(Level.SEVERE, CatalogManager.class.getName(),
						"addFD", "Couldn't add FD.", ex);
			}
		}
	}

	/**
	 * Retrieves functional dependencies for the specified database schema.
	 * <p>
	 * Two different cases are possible:
	 * <ul>
	 * <li>The <code>schema</code> is loaded for the first time. A new
	 * fd_catalog user relations will be created. For each relation, its primary
	 * and unique key constraints are added as initial values.</li>
	 * <li>The <code>schema</code> is loaded at least for the second time. The
	 * fd_catalog user relations already exists. No further action is
	 * neccessary.</li>
	 * </ul>
	 * <p>
	 * Next, the functional dependencies are retrieved from the fd_catalog
	 * relations.
	 * 
	 * @param schema
	 *            database schema
	 */
	public void getFD(DbSchema schema) {
		this.schema = schema;

		Connection con = ConnectionManager.getConnection();

		boolean catalogExists = checkIfCatalogExists(con);

		if (!catalogExists) {
			createCatalog(con);
			insertPrimaries(con);
			insertUniques(con);
		}

		fetchKeyFD(con);
		fetchNonKeyFD(con);

		ConnectionManager.closeConnection();

	}

	/**
	 * Retrieves a functional dependency that is specified by its ID from the
	 * fd_catalog.
	 * 
	 * @param id
	 *            a FD's ID
	 * @return a functional dependency
	 */
	public FDep getFD(int id) {

		String fdQuery = "SELECT fd.iskey, lhs.attribute, rhs.attribute "
				+ "FROM fd_catalog fd " + "INNER JOIN fd_catalog_lhs lhs "
				+ "ON fd.id = lhs.id " + "INNER JOIN fd_catalog_rhs rhs "
				+ "ON lhs.id = rhs.id " + "WHERE iskey = FALSE "
				+ "AND fd.id = ?";

		PreparedStatement queryFD;
		ResultSet rs = null;

		/*
		 * Local variables. lhs and rhs are sets of left- and right-hand side
		 * column names.
		 */
		FDep fd = null;
		SortedSet<String> lhs = new TreeSet<String>();
		SortedSet<String> rhs = new TreeSet<String>();
		String left, right;
		boolean isKey = false;

		Connection con = ConnectionManager.getConnection();

		try {
			queryFD = con.prepareStatement(fdQuery);

			queryFD.setInt(1, id);
			rs = queryFD.executeQuery();

			while (rs.next()) {
				isKey = rs.getBoolean(1);
				left = rs.getString(2);
				right = rs.getString(3);

				lhs.add(left);
				rhs.add(right);
			}

			fd = new FDep(lhs, rhs);
			fd.setIsKey(isKey);
			fd.setId(id);

			rs.close();
			queryFD.close();
		} catch (SQLException ex) {
			logger.logp(Level.SEVERE, CatalogManager.class.getName(), "getFD",
					"Couldn't get FD.", ex);
		} finally {
			ConnectionManager.closeConnection();
		}

		return fd;
	}

	/**
	 * Removes a given functional dependency from the database, including all
	 * trigger constraints and fd_catalog entries.
	 * 
	 * @param id
	 *            the FD's ID
	 * @param table
	 *            the relation in that the FD is valid
	 * @param con
	 *            a database connection
	 */
	public void removeFD(Integer id, String table, Connection con) {

		String deleteTrigger = "DELETE FROM fd_catalog_trigger WHERE id = ?";
		String deleteFromCat = "DELETE FROM fd_catalog WHERE id = ?";
		String deleteLHS = "DELETE FROM fd_catalog_lhs WHERE id = ?";
		String deleteRHS = "DELETE FROM fd_catalog_rhs WHERE id = ?";
		String getName = "SELECT name, function FROM fd_catalog_trigger WHERE id = ?";

		PreparedStatement pstmt = null;

		ResultSet rs = null;
		String trigger = null, function = null;

		try {
			/*
			 * Get names of the (trigger, trigger function)-pair, that
			 * implements the FD.
			 */
			pstmt = con.prepareStatement(getName);
			pstmt.setInt(1, id);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				trigger = rs.getString(1);
				function = rs.getString(2);
			}

			rs.close();

			if (trigger != null) {
				/*
				 * The typical use of a PreparedStatement via .setString(1,
				 * trigger); .setString(2, table); doesn't work here, probably
				 * because of the escaped quotation marks in the Strings.
				 */
				String dropTriggerQuery = "DROP TRIGGER " + trigger + " ON \""
						+ table + "\"";
				pstmt = con.prepareStatement(dropTriggerQuery);
				pstmt.executeUpdate();

				// drop function
				String dropFunctionQuery = "DROP FUNCTION " + function;
				pstmt = con.prepareStatement(dropFunctionQuery);
				pstmt.executeUpdate();

				// delete trigger catalog entries
				pstmt = con.prepareStatement(deleteTrigger);
				pstmt.setInt(1, id);
				pstmt.executeUpdate();
			}

			// delete left-hand side entries
			pstmt = con.prepareStatement(deleteLHS);
			pstmt.setInt(1, id);
			pstmt.executeUpdate();

			// delete right-hand side entries
			pstmt = con.prepareStatement(deleteRHS);
			pstmt.setInt(1, id);
			pstmt.executeUpdate();

			// delete master catalog entry
			pstmt = con.prepareStatement(deleteFromCat);
			pstmt.setInt(1, id);
			pstmt.executeUpdate();

			pstmt.close();
		} catch (SQLException ex) {
			logger.logp(Level.SEVERE, CatalogManager.class.getName(),
					"removeFD", "Couldn't remove FD.", ex);
		}
	}

	/**
	 * Returns true if the fd_catalog relations already exists in the loaded
	 * schema.
	 * 
	 * @param con
	 *            a database connection
	 * @return true if the fd_catalog relations already exist.
	 */
	private boolean checkIfCatalogExists(Connection con) {

		// Query the number of tables in the schema with name = 'fd_catalog'.
		String query = "SELECT count(*) " + "FROM information_schema.tables "
				+ "WHERE table_schema NOT IN "
				+ "('pg_catalog', 'information_schema') "
				+ "AND table_name = 'fd_catalog'";
		PreparedStatement pstmt;
		ResultSet rs;

		boolean catalogExists = false;
		try {
			pstmt = con.prepareStatement(query);
			rs = pstmt.executeQuery();

			// Number of results should be 1 if the fd_catalog relations exist.
			while (rs.next()) {
				if (rs.getInt(1) == 1) {
					catalogExists = true;
				}
			}

			// Close ResultSets and Statements.
			rs.close();
			pstmt.close();
		} catch (SQLException ex) {
			logger.logp(Level.SEVERE, CatalogManager.class.getName(),
					"checkIfCatalogExists",
					"Couldn't check if catalog exists.", ex);
		}

		return catalogExists;
	}

	/**
	 * Creates the fd_catalog user relations.
	 * 
	 * @param con
	 *            a database connection
	 */
	private void createCatalog(Connection con) {
		/*
		 * Create the fd_catalog relation, that holds a unique id, the
		 * relation's name and whether the fd is implemented by a key
		 * constraint.
		 */
		String createCatalog = "CREATE TABLE fd_catalog (" + "id serial, "
				+ "relation character varying(80), " + "iskey boolean, "
				+ "PRIMARY KEY (id) " + ");";

		/*
		 * Create the fd_catalog_lhs relation, that holds information about the
		 * fd's left-hand side attributes.
		 */
		String createLHS = "CREATE TABLE fd_catalog_lhs (" + "id integer, "
				+ "attribute character varying(80), "
				+ "FOREIGN KEY (id) REFERENCES fd_catalog (id) "
				+ "ON UPDATE CASCADE ON DELETE CASCADE, "
				+ "PRIMARY KEY (id, attribute) " + ");";

		/*
		 * Create the fd_catalog_rhs relation, that holds information about the
		 * fd's right-hand side attributes.
		 */
		String createRHS = "CREATE TABLE fd_catalog_rhs (" + "id integer, "
				+ "attribute character varying(80), "
				+ "PRIMARY KEY (id, attribute), "
				+ "FOREIGN KEY (id) REFERENCES fd_catalog (id) "
				+ "ON UPDATE CASCADE ON DELETE CASCADE " + ");";

		/*
		 * Create the fd_catalog_trigger relation, that holds information about
		 * the fd's implementing trigger and trigger function.
		 */
		String createTriggers = "CREATE TABLE fd_catalog_trigger ( "
				+ "id integer, " + "name character varying(80), "
				+ "function character varying(80), "
				+ "FOREIGN KEY (id) REFERENCES fd_catalog (id) "
				+ "ON UPDATE CASCADE ON DELETE CASCADE, " + "PRIMARY KEY (id) "
				+ ");";

		Statement stmt;
		try {
			con.setAutoCommit(false);

			stmt = con.createStatement();
			stmt.executeUpdate(createCatalog);
			stmt.executeUpdate(createLHS);
			stmt.executeUpdate(createRHS);
			stmt.executeUpdate(createTriggers);
			stmt.close();

			con.commit();
			con.setAutoCommit(true);
		} catch (SQLException ex) {
			try {
				con.rollback();
			} catch (SQLException ex1) {
				logger.logp(Level.SEVERE, CatalogManager.class.getName(),
						"createCatalog", "Couldn't rollback transaction.", ex1);
			}
			logger.logp(Level.SEVERE, CatalogManager.class.getName(),
					"createCatalog", "Couldn't commit transaction.", ex);
		}
	}

	/**
	 * Retrieves functional dependencies from the fd_catalog relations that are
	 * implemented via a SQL key constraint.
	 * 
	 * @param con
	 *            a database connection
	 */
	private void fetchKeyFD(Connection con) {

		String keyQuery = "SELECT fd.id, " + "attribute "
				+ "FROM fd_catalog fd " + "INNER JOIN fd_catalog_lhs lhs  "
				+ "ON fd.id = lhs.id " + "WHERE iskey = TRUE "
				+ "AND fd.relation = ?";

		PreparedStatement queryKeys;
		ResultSet rs = null;

		/*
		 * Temporary variables. keyMap maps the fd's unique id to a set of
		 * left-hand side column names.
		 */
		SortedMap<Integer, SortedSet<String>> keyMap;
		SortedSet<String> lhs;

		try {
			queryKeys = con.prepareStatement(keyQuery);

			String table = null;
			int id;
			String column;

			/*
			 * Gets the fd's id and all left-hand side attributes for each
			 * relation in the schema and stores them in a map. If keyMap
			 * already contains the retrieved id as a key, get it's associated
			 * set and add the column name to it. If keyMap doesn't contain id
			 * as a key, create a new set, add the column to it and put the (id,
			 * set)-tupel into the map.
			 */
			for (Relation rel : schema.getRelations()) {
				table = rel.getRelationName();
				keyMap = new TreeMap<Integer, SortedSet<String>>();

				queryKeys.setString(1, table);
				rs = queryKeys.executeQuery();

				while (rs.next()) {
					id = rs.getInt(1);
					column = rs.getString(2);

					if (keyMap.containsKey(id)) {
						lhs = keyMap.get(id);
					} else {
						lhs = new TreeSet<String>();
						keyMap.put(id, lhs);
					}

					lhs.add(column);
				}

				FDep fd;
				int key;
				SortedSet<String> value;

				/*
				 * For each entry in the map, create a new FDep object and set
				 * its left side attributes according to the map values.
				 */
				for (Map.Entry<Integer, SortedSet<String>> entry : keyMap
						.entrySet()) {
					key = entry.getKey();
					value = entry.getValue();

					fd = new FDep(true);
					fd.setId(key);
					fd.setLeftSide(value);
					SortedSet<String> cols = new TreeSet<String>(rel
							.getColumns());
                    cols.removeAll(value);
					fd.setRightSide(cols);
					rel.addFDep(fd);
				}

			}

			rs.close();
			queryKeys.close();
		} catch (SQLException ex) {
			logger.logp(Level.SEVERE, CatalogManager.class.getName(),
					"fetchKeyFD", "Couldn't get key FDs.", ex);
		}

	}

	/**
	 * Retrieves functional dependencies from the fd_catalog relations that are
	 * implemented via a trigger and, thus, are specified manually by the user.
	 * These fd can later be removed from the catalog.
	 * 
	 * @param con
	 *            a database connection
	 */
	private void fetchNonKeyFD(Connection con) {
		String fdQuery = "SELECT fd.id, lhs.attribute, rhs.attribute "
				+ "FROM fd_catalog fd " + "INNER JOIN fd_catalog_lhs lhs "
				+ "ON fd.id = lhs.id " + "INNER JOIN fd_catalog_rhs rhs "
				+ "ON lhs.id = rhs.id " + "WHERE iskey = FALSE "
				+ "AND fd.relation = ?";

		PreparedStatement queryFD;
		ResultSet rs = null;

		/*
		 * Temporary variables. keyMap maps the FD's unique id to a set of left-
		 * and right-hand side column names.
		 */
		SortedMap<Integer, List<SortedSet<String>>> keyMap;
		List<SortedSet<String>> att;
		SortedSet<String> lhs;
		SortedSet<String> rhs;

		try {
			queryFD = con.prepareStatement(fdQuery);

			String table = null;
			int id;
			String left, right;
			/*
			 * Gets the fd's id and all left-hand side attributes as well as all
			 * right-hand side attributes for each relation in the schema and
			 * stores them in a map. If keyMap already contains the retrieved id
			 * as a key, get it's associated sets and add the column name to it.
			 * If keyMap doesn't contain id as a key, create new sets, add the
			 * column to it and put the (id, set)-tupel into the map.
			 */
			for (Relation rel : schema.getRelations()) {
				table = rel.getRelationName();
				keyMap = new TreeMap<Integer, List<SortedSet<String>>>();

				queryFD.setString(1, table);
				rs = queryFD.executeQuery();

				while (rs.next()) {
					id = rs.getInt(1);
					left = rs.getString(2);
					right = rs.getString(3);

					if (keyMap.containsKey(id)) {
						lhs = keyMap.get(id).get(0);
						rhs = keyMap.get(id).get(1);
					} else {
						lhs = new TreeSet<String>();
						rhs = new TreeSet<String>();
						att = new ArrayList<SortedSet<String>>();

						att.add(lhs);
						att.add(rhs);

						keyMap.put(id, att);
					}

					lhs.add(left);
					rhs.add(right);
				}

				FDep fd;
				int key;
				List<SortedSet<String>> value;
				/*
				 * For each entry in the map, create a new FDep object and set
				 * its left and right side attributes according to the map
				 * values.
				 */
				for (Map.Entry<Integer, List<SortedSet<String>>> entry : keyMap
						.entrySet()) {
					key = entry.getKey();
					value = entry.getValue();

					fd = new FDep(value.get(0), value.get(1));
					fd.setId(key);
					rel.addFDep(fd);
				}

			}
			rs.close();
			queryFD.close();
		} catch (SQLException ex) {
			logger.logp(Level.SEVERE, CatalogManager.class.getName(),
					"fetchNonKeyFD", "Couldn't get non-key FDs.", ex);
		}
	}

	/**
	 * Inserts attributes belonging to a certain fd that is implemented via a
	 * <emph>key constraint</emph> into the fd_catalog relations.
	 * 
	 * @param table
	 *            the relation the fd refers to.
	 * @param columns
	 *            the fd attributes.
	 * @param con
	 *            a database connection
	 */
	private void insert(String table, Set<String> columns, Connection con) {

		String insertCatalogQuery = "INSERT INTO fd_catalog (relation, iskey) VALUES(?, true)";
		String insertLHSQuery = "INSERT INTO fd_catalog_lhs VALUES(?, ?)";
		String getIDQuery = "SELECT MAX(id) FROM fd_catalog WHERE relation = ?";

		PreparedStatement insertCatalog = null;
		PreparedStatement insertLHS = null;
		PreparedStatement getID = null;

		ResultSet rs = null;

		int id = -1;

		try {
			con.setAutoCommit(false);

			insertCatalog = con.prepareStatement(insertCatalogQuery);
			insertLHS = con.prepareStatement(insertLHSQuery);
			getID = con.prepareStatement(getIDQuery);

			// Insert fd into fd_catalog.
			insertCatalog.setString(1, table);
			insertCatalog.executeUpdate();

			// Get id for the inserted fd.
			getID.setString(1, table);
			rs = getID.executeQuery();

			while (rs.next()) {
				id = rs.getInt(1);
			}

			// For each attribute, insert a tupel into fd_catalog_lhs.
			for (String column : columns) {

				insertLHS.setInt(1, id);
				insertLHS.setString(2, column);
				insertLHS.executeUpdate();

			}

			// Close ResultSets and Statements.
			getID.close();
			insertLHS.close();
			insertCatalog.close();
			rs.close();

			con.commit();
			con.setAutoCommit(true);

		} catch (SQLException ex) {
			try {
				con.rollback();
			} catch (SQLException ex1) {
				logger.logp(Level.SEVERE, CatalogManager.class.getName(),
						"insert", "Couldn't rollback transaction.", ex);
			}
			logger.logp(Level.SEVERE, CatalogManager.class.getName(), "insert",
					"Couldn't commit transaction.", ex);
		}
	}

	/**
	 * Fetches primary key constraints and inserts them as functional
	 * dependencies into the fd_catalog.
	 * 
	 * @param con
	 *            a database connection
	 */
	private void insertPrimaries(Connection con) {
		Statement stmt;
		DatabaseMetaData dbmd;

		ResultSet rs = null;
		Set<String> columns = null;

		try {
			stmt = con.createStatement();
			/*
			 * Retrieves a <code>DatabaseMetaData</code> object that contains
			 * metadata about the database to which this <code>Connection</code>
			 * object represents a connection. This includes the primary keys.
			 */
			dbmd = con.getMetaData();

			// For each relation, get primary keys.
			for (Relation rel : schema.getRelations()) {
				columns = new HashSet<String>();
				String table = rel.getRelationName();

				rs = dbmd.getPrimaryKeys(null, null, table);

				// For each attribute of the key, add it to the colums set.
				while (rs.next()) {
					String column = rs.getString("COLUMN_NAME");
					columns.add(column);
				}
				// Insert attributes into the relation.
				insert(table, columns, con);

			}

			rs.close();
			stmt.close();
		} catch (SQLException ex) {
			logger.logp(Level.SEVERE, CatalogManager.class.getName(),
					"insertPrimaries",
					"Couldn't insert primary keys into catalog.", ex);
		}

	}

	/**
	 * Checks whether the given FD is implemented as a unique constraint in the
	 * given table.
	 * 
	 * @param table
	 *            a table name
	 * @param fd
	 *            a FD
	 * @param con
	 *            a database connection
	 * @return true, if FD is unique in <code>table</code>
	 */
	private boolean isUnique(String table, FDep fd, Connection con) {
		boolean isUnique = false;

		ResultSet rsConstraints = null;
		ResultSet rsColumns = null;
		PreparedStatement queryColumns = null;
		PreparedStatement queryConstraints = null;

		// Query unique constraints using information_schema.
		String constraintQuery = "SELECT " + "constraint_name "
				+ "FROM information_schema.table_constraints "
				+ "WHERE table_name = ?" + "AND constraint_type = 'UNIQUE'";

		/*
		 * Query attributes of a specific unique constraint using
		 * information_schema.
		 */
		String columnsQuery = "SELECT " + "kcu.column_name "
				+ "FROM information_schema.table_constraints tc "
				+ "LEFT JOIN information_schema.key_column_usage kcu "
				+ "ON tc.constraint_catalog = kcu.constraint_catalog "
				+ "AND tc.constraint_schema = kcu.constraint_schema "
				+ "AND tc.constraint_name = kcu.constraint_name "
				+ "WHERE tc.table_name = ? " + "AND tc.constraint_name = ?";

		Set<String> columns = null;

		try {
			queryConstraints = con.prepareStatement(constraintQuery);
			queryColumns = con.prepareStatement(columnsQuery);
			columns = new HashSet<String>();

			// Set table name.
			queryConstraints.setString(1, table);
			queryColumns.setString(1, table);

			// Fetch unique constraints.
			rsConstraints = queryConstraints.executeQuery();
			String constraintName = null;

			// For each unique constraint fetch corresponding column names.
			while (rsConstraints.next()) {
				columns.clear();
				constraintName = rsConstraints.getString(1);

				queryColumns.setString(2, constraintName);
				rsColumns = queryColumns.executeQuery();
				while (rsColumns.next()) {
					columns.add(rsColumns.getString(1));
				}

				// If columns equal the FD's LHS attributes, FD is unique.
				if (columns.equals(fd.getLeftSide())) {
					isUnique = true;
				}

				rsColumns.close();
			}

			rsConstraints.close();
			queryColumns.close();
			queryConstraints.close();

		} catch (SQLException ex) {
			logger.logp(Level.SEVERE, CatalogManager.class.getName(),
					"isUnique", "Couldn't fetch unique attributes.", ex);
		}

		return isUnique;
	}

	/**
	 * Fetches unique key constraints and inserts them as functional
	 * dependencies into the fd_catalog.
	 * 
	 * @param con
	 *            a database connection
	 */
	private void insertUniques(Connection con) {
		ResultSet rsConstraints = null;
		ResultSet rsColumns = null;
		PreparedStatement queryColumns = null;
		PreparedStatement queryConstraints = null;

		// Query unique constraints using information_schema.
		String constraintQuery = "SELECT " + "constraint_name "
				+ "FROM information_schema.table_constraints "
				+ "WHERE table_name = ?" + "AND constraint_type = 'UNIQUE'";

		/*
		 * Query attributes of a specific unique constraint using
		 * information_schema.
		 */
		String columnsQuery = "SELECT " + "kcu.column_name "
				+ "FROM information_schema.table_constraints tc "
				+ "LEFT JOIN information_schema.key_column_usage kcu "
				+ "ON tc.constraint_catalog = kcu.constraint_catalog "
				+ "AND tc.constraint_schema = kcu.constraint_schema "
				+ "AND tc.constraint_name = kcu.constraint_name "
				+ "WHERE tc.table_name = ? " + "AND tc.constraint_name = ?";

		Set<String> columns = null;

		try {
			queryConstraints = con.prepareStatement(constraintQuery);
			queryColumns = con.prepareStatement(columnsQuery);

			// For each relation fetch unique constraints.
			for (Relation rel : schema.getRelations()) {
				columns = new HashSet<String>();
				String table = rel.getRelationName();

				// Set table name.
				queryConstraints.setString(1, table);
				queryColumns.setString(1, table);

				rsConstraints = queryConstraints.executeQuery();
				String constraintName = null;

				// For each unique constraint fetch corresponding column names.
				while (rsConstraints.next()) {
					constraintName = rsConstraints.getString(1);

					queryColumns.setString(2, constraintName);
					rsColumns = queryColumns.executeQuery();

					while (rsColumns.next()) {
						columns.add(rsColumns.getString(1));
					}
					rsColumns.close();

					// Insert attributes into the relation.
					insert(table, columns, con);

				}
				rsConstraints.close();
			}

			queryColumns.close();
			queryConstraints.close();

		} catch (SQLException ex) {
			logger.logp(Level.SEVERE, CatalogManager.class.getName(),
					"insertUniques",
					"Couldn't insert unique keys into catalog.", ex);
		}
	}

	/**
	 * Implements the given set of attributes (a candidate key) as a unique
	 * constraint in the given table.
	 * 
	 * @param referencedTable
	 *            a table name
	 * @param ckey
	 *            a set of attributes belonging to a candidate key
	 * @param con
	 *            a database connection
	 */
	public void setAsUnique(String referencedTable, Set<String> ckey,
			Connection con) {

		ResultSet rs = null;
		DatabaseMetaData dbmd;
		Set<String> columns = new HashSet<String>();
		/**
		 * Retrieve the relation's primary key attributes.
		 */
		try {
			dbmd = con.getMetaData();
			rs = dbmd.getPrimaryKeys(null, null, referencedTable);

			while (rs.next()) {
				String column = rs.getString("COLUMN_NAME");
				columns.add(column);
			}
		} catch (SQLException e) {
			logger.logp(Level.SEVERE, CatalogManager.class.getName(),
					"setAsUnique", "Couldn't retrieve primary key attributes.",
					e);
		}

		/*
		 * If the retrieved primary key equals the given candidate key, do
		 * nothing. Otherwise, set the candidate key attributes as unique.
		 */
		if (!columns.equals(ckey)) {
			// Create unique statement for all attributes of ckey
			StringBuffer sb = new StringBuffer();
			sb.append("ALTER TABLE ");
			sb.append("\"" + referencedTable + "\" ");
			sb.append("ADD UNIQUE(");
			Iterator<String> it = ckey.iterator();
			while (it.hasNext()) {
				sb.append("\"" + it.next() + "\"");
				if (it.hasNext()) {
					sb.append(",");
				} else {
					sb.append(")");
				}
			}

			// Execute unique statement
			Statement stmt;
			try {
				stmt = con.createStatement();
				stmt.executeUpdate(sb.toString());
				stmt.close();
			} catch (SQLException ex) {
				logger.logp(Level.SEVERE, CatalogManager.class.getName(),
						"setAsUniques", "Couldn't update unique constraint.",
						ex);
			}
		}
	}
}
