import com.zaxxer.hikari.HikariDataSource;
import jooq.tables.records.LinkRecord;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static jooq.Tables.LINK;

public class PostgresqlDB implements AutoCloseable, Closeable {

	public final static String DB_NAME_PREFIX = "test-db-";

	private final Connection connection;
	private final String connectionString;
	private final String dbUsername;
	private final String dbPassword;
	private final DataSource dataSource;
	private final Settings jooqSettings;

	static {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static PostgresqlDB createPostgreWithHikari(String host, int port, String dbName, String username, String password) {
		String connectionString = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
		HikariDataSource ds = new HikariDataSource();
		ds.setJdbcUrl(connectionString);
		ds.setUsername(username);
		ds.setPassword(password);
		ds.setDriverClassName("org.postgresql.Driver");
		return new PostgresqlDB(ds);
	}

	public static PostgresqlDB createPostgreWithConnection(String host, int port, String dbName, String username, String password) {
		String connectionString = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;

		try {
			Connection connection = DriverManager.getConnection(connectionString, username, password);
			return new PostgresqlDB(connection, connectionString, username, password);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private PostgresqlDB(DataSource dataSource) {
		this.connection = null;
		this.connectionString = null;
		this.dbUsername = null;
		this.dbPassword = null;
		this.dataSource = dataSource;
		this.jooqSettings = new Settings();
		this.jooqSettings.setRenderNameCase(RenderNameCase.LOWER);
		this.runFlyway();
	}

	private PostgresqlDB(Connection connection, String connectionString, String dbUsername, String dbPassword) {
		this.connection = connection;
		this.connectionString = connectionString;
		this.dbUsername = dbUsername;
		this.dbPassword = dbPassword;
		this.dataSource = null;
		this.jooqSettings = new Settings();
		this.jooqSettings.setRenderNameCase(RenderNameCase.LOWER);
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
			DBUtils.runFlyway(DB_NAME_PREFIX, this.connectionString, this.dbUsername, this.dbPassword, "/migrations", DBUtils.DatabaseType.POSTGRESQL);
		} else if (this.dataSource != null) {
			DBUtils.runFlyway(DB_NAME_PREFIX, this.dataSource, "/migrations", DBUtils.DatabaseType.POSTGRESQL);
		}
	}

	private DSLContext db() {
		if (this.dataSource != null) {
			return DSL.using(this.dataSource, SQLDialect.POSTGRES, this.jooqSettings);
		}
		if (this.connection != null) {
			return DSL.using(this.connection, SQLDialect.POSTGRES, this.jooqSettings);
		}
		throw new RuntimeException("undefined db connection type");
	}

	private DSLContext db2() { // Connection from DataSource
		if (this.dataSource != null) {
			try {
				return DSL.using(this.dataSource.getConnection(), SQLDialect.POSTGRES, this.jooqSettings);
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
		db().transaction(c -> {
			batchInsert.execute();
		});
	}

	public void replaceLinks2(List<LinkDAO> linkDAOs) {
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
