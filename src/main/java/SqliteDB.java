import com.zaxxer.hikari.HikariDataSource;
import jooq.tables.records.LinkRecord;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static jooq.Tables.LINK;

public class SqliteDB implements AutoCloseable, Closeable {

	public final static String DB_NAME_PREFIX = "test-db-";

	private final Connection connection;
	private final String connectionString;
	private final DataSource dataSource;
	private final Settings jooqSettings;

	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static SqliteDB createSqliteWithDataSource(File dbFile) {
		String connectionString = "jdbc:sqlite:" + dbFile.getAbsolutePath();
		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.enforceForeignKeys(true);
		SQLiteDataSource ds = new SQLiteDataSource();
		ds.setUrl(connectionString);
		return new SqliteDB(null, null, ds, SQLDialect.SQLITE);
	}

	public static SqliteDB createSqliteWithHikari(File dbFile) {
		String connectionString = "jdbc:sqlite:" + dbFile.getAbsolutePath();
		SQLiteConfig sqliteConfig = new SQLiteConfig();
		sqliteConfig.enforceForeignKeys(true);
		HikariDataSource ds = new HikariDataSource();
		ds.setJdbcUrl(connectionString);
		ds.setDriverClassName("org.sqlite.JDBC");
		return new SqliteDB(null, null, ds, SQLDialect.SQLITE);
	}

	public static SqliteDB createSqliteWithConnection(File dbFile) {
		String filename = dbFile.getAbsolutePath();
		String connectionString = "jdbc:sqlite:" + filename;

		try {
			SQLiteConfig config = new SQLiteConfig();
			config.enforceForeignKeys(true);
			Connection connection = DriverManager.getConnection(connectionString, config.toProperties());
			return new SqliteDB(connection, connectionString, null, SQLDialect.SQLITE);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private SqliteDB(Connection connection, String connectionString, DataSource dataSource, SQLDialect sqlDialect) {
		this.connection = connection;
		this.connectionString = connectionString;
		this.dataSource = dataSource;
		this.jooqSettings = new Settings();
		if (sqlDialect == SQLDialect.POSTGRES) {
			this.jooqSettings.setRenderNameCase(RenderNameCase.LOWER);
		}
		this.runFlyway();
	}

	@Override
	public void close() throws IOException {
		if (this.dataSource instanceof AutoCloseable) {
			try {
				((AutoCloseable) this.dataSource).close();
			} catch (Exception e) {
				throw new IOException("Could not close database connection.", e);
			}
		}
	}

	public void runFlyway() {
		if (this.connectionString != null) {
			DBUtils.runFlyway(DB_NAME_PREFIX, this.connectionString, null, null, "/migrations", DBUtils.DatabaseType.SQLITE);
		} else if (this.dataSource != null) {
			DBUtils.runFlyway(DB_NAME_PREFIX, this.dataSource, "/migrations", DBUtils.DatabaseType.SQLITE);
		}
	}

	private DSLContext db() {
		if (this.dataSource != null) {
			return DSL.using(this.dataSource, SQLDialect.SQLITE, this.jooqSettings);
		}
		if (this.connection != null) {
			return DSL.using(this.connection, SQLDialect.SQLITE, this.jooqSettings);
		}
		throw new RuntimeException("undefined db connection type");
	}

	private DSLContext db2() {
		if (this.dataSource != null) {
			try {
				return DSL.using(this.dataSource.getConnection(), SQLDialect.SQLITE, this.jooqSettings);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		throw new RuntimeException("undefined db connection type");
	}

	// ==== LINK ====

	public void replaceLinks(List<LinkDAO> linkDAOs) {
		if (linkDAOs.isEmpty()) {
			return;
		}
		db().transaction(c -> {
			BatchBindStep batchInsert = db().batch(
				db()
					.insertInto(LINK, LINK.LINKID, LINK.GEOMETRY)
					.values((String) null, null)
			);
			for (LinkDAO linkDAO : linkDAOs) {
				batchInsert.bind(linkDAO.linkId, linkDAO.geometry);
			}
			db()
					.truncate(LINK)
					.cascade()
					.execute();
			batchInsert.execute();
		});
	}

	public void replaceLinks_ConnectionFromDataSource(List<LinkDAO> linkDAOs) {
		if (linkDAOs.isEmpty()) {
			return;
		}
		db2().transaction(c -> {
			BatchBindStep batchInsert = db().batch(
					db2()
							.insertInto(LINK, LINK.LINKID, LINK.GEOMETRY)
							.values((String) null, null)
			);
			for (LinkDAO linkDAO : linkDAOs) {
				batchInsert.bind(linkDAO.linkId, linkDAO.geometry);
			}
			db2()
					.truncate(LINK)
					.cascade()
					.execute();


			batchInsert.execute();
		});
	}

	public void replaceLinks_JDBC(List<LinkDAO> linkDAOs) throws SQLException {
		Connection conn = this.connection;
		if (this.connection == null) {
			conn = this.dataSource.getConnection();
		}
		conn.setAutoCommit(false);
		conn.createStatement().execute("DELETE FROM LINK;");
		PreparedStatement pstmt = conn.prepareStatement(
				"INSERT INTO Link(id, linkid, geometry) VALUES(?,?,?)");

		for (LinkDAO dao : linkDAOs) {
			pstmt.setLong(1, dao.id);
			pstmt.setString(2, dao.linkId);
			pstmt.setString(3, dao.geometry);
			pstmt.addBatch();
		}
		pstmt.executeBatch();
		conn.commit();
		if (this.connection == null) {
			conn.close();
		}
	}

	public List<LinkDAO> getLinks() {
		List<LinkDAO> linkDAOs = new ArrayList<>();

		Result<LinkRecord> linkRecords = db()
			.selectFrom(LINK)
			.orderBy(LINK.ID)
			.fetch();

		for (LinkRecord lr : linkRecords) {
			linkDAOs.add(new LinkDAO(lr.getId(), lr.getLinkid(), lr.getGeometry()));
		}

		return linkDAOs;
	}

}
