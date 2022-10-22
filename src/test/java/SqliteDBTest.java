import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteDBTest {

	private void runPerformanceTest(SqliteDB db) throws IOException {
		int linkCount = 50_000;

		List<LinkDAO> daos = new ArrayList<>();
		for (int i = 0; i < linkCount; i++) {
			daos.add(new LinkDAO(i, Integer.toString(i), "\"LINESTRING (561770.1124776328 5801163.18893374, 561887.6550922041 5801106.550209084)\""));
		}

		long start = System.currentTimeMillis();
		db.replaceLinks(daos);
		long end = System.currentTimeMillis();
		long durationRetrieval = end - start;

		int expectedDurationInMilliseconds = 10_000;
		assertTrue(durationRetrieval < expectedDurationInMilliseconds, "The goal is to store " + linkCount + " links in less than " + expectedDurationInMilliseconds / 1000 + " seconds. Actual time was " + durationRetrieval + " milliseconds.");
		assertEquals(linkCount, db.getLinks().size());
	}

	private void runPerformanceTest_ConnectionFromDataSource(SqliteDB db) throws IOException {
		int linkCount = 50_000;

		List<LinkDAO> daos = new ArrayList<>();
		for (int i = 0; i < linkCount; i++) {
			daos.add(new LinkDAO(i, Integer.toString(i), "\"LINESTRING (561770.1124776328 5801163.18893374, 561887.6550922041 5801106.550209084)\""));
		}

		long start = System.currentTimeMillis();
		db.replaceLinks_ConnectionFromDataSource(daos);
		long end = System.currentTimeMillis();
		long durationRetrieval = end - start;

		int expectedDurationInMilliseconds = 10_000;
		assertTrue(durationRetrieval < expectedDurationInMilliseconds, "The goal is to store " + linkCount + " links in less than " + expectedDurationInMilliseconds / 1000 + " seconds. Actual time was " + durationRetrieval + " milliseconds.");
		assertEquals(linkCount, db.getLinks().size());
	}

	private void runPerformanceTest_JDBC(SqliteDB db) throws SQLException {
		int linkCount = 50_000;

		List<LinkDAO> daos = new ArrayList<>();
		for (int i = 0; i < linkCount; i++) {
			daos.add(new LinkDAO(i, Integer.toString(i), "\"LINESTRING (561770.1124776328 5801163.18893374, 561887.6550922041 5801106.550209084)\""));
		}

		long start = System.currentTimeMillis();
		db.replaceLinks_JDBC(daos);
		long end = System.currentTimeMillis();
		long durationRetrieval = end - start;

		int expectedDurationInMilliseconds = 10_000;
		assertTrue(durationRetrieval < expectedDurationInMilliseconds, "The goal is to store " + linkCount + " links in less than " + expectedDurationInMilliseconds / 1000 + " seconds. Actual time was " + durationRetrieval + " milliseconds.");
		assertEquals(linkCount, db.getLinks().size());
	}

	@Test
	void testSqlite_jooq_DataSource() throws IOException {
		File dbFile = new File("test-performance.db");
		if (dbFile.exists()) {
			dbFile.delete();
		}
		try (SqliteDB db = SqliteDB.createSqliteWithDataSource(dbFile)) {
			runPerformanceTest(db);
		}
	}

	@Test
	void testSqlite_jooq_ConnectionFromDataSource() throws IOException {
		File dbFile = new File("test-performance.db");
		if (dbFile.exists()) {
			dbFile.delete();
		}
		try (SqliteDB db = SqliteDB.createSqliteWithDataSource(dbFile)) {
			runPerformanceTest_ConnectionFromDataSource(db);
		}
	}

	@Test
	void testSqlite_jooq_Hikari() throws IOException {
		File dbFile = new File("test-performance.db");
		if (dbFile.exists()) {
			dbFile.delete();
		}
		try (SqliteDB db = SqliteDB.createSqliteWithHikari(dbFile)) {
			runPerformanceTest(db);
		}
	}

	@Test
	void testSqlite_jooq_Connection() throws IOException {
		File dbFile = new File("test-performance.db");
		if (dbFile.exists()) {
			dbFile.delete();
		}
		try (SqliteDB db = SqliteDB.createSqliteWithConnection(dbFile)) {
			runPerformanceTest(db);
		}
	}

	@Test
	void testSqlite_Jdbc_Connection() throws IOException, SQLException {
		File dbFile = new File("test-performance.db");
		if (dbFile.exists()) {
			dbFile.delete();
		}
		try (SqliteDB db = SqliteDB.createSqliteWithConnection(dbFile)) {
			runPerformanceTest_JDBC(db);
		}
	}

	@Test
	void testSqlite_Jdbc_DataSource() throws IOException, SQLException {
		File dbFile = new File("test-performance.db");
		if (dbFile.exists()) {
			dbFile.delete();
		}
		try (SqliteDB db = SqliteDB.createSqliteWithDataSource(dbFile)) {
			runPerformanceTest_JDBC(db);
		}
	}

}
