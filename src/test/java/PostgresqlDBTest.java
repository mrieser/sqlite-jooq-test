import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class PostgresqlDBTest {

	private final static String PG_DBNAME = "test-db";
	private final static String PG_USERNAME = "postgresTest";
	private final static String PG_PASSWORD = "just_for_tests";

	private static int COUNTER = 0;

	@Container
	public static GenericContainer postgres = new GenericContainer(DockerImageName.parse("postgres:14.5-alpine"))
			.withExposedPorts(5432)
			.withEnv("POSTGRES_USER", PG_USERNAME)
			.withEnv("POSTGRES_PASSWORD", PG_PASSWORD);

	private void runPerformanceTest(PostgresqlDB db) throws IOException {
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

	private void runPerformanceTest_JDBC(PostgresqlDB db) throws SQLException {
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
	void testPostgresql_jooq_Hikari() throws IOException {
		String host = postgres.getHost();
		Integer port = postgres.getFirstMappedPort();
		String dbName = PG_DBNAME + COUNTER++;

		createPostgresqlDatabase(host, port, dbName, PG_USERNAME, PG_PASSWORD);

		try (PostgresqlDB db = PostgresqlDB.createPostgreWithHikari(host, port, dbName, PG_USERNAME, PG_PASSWORD)) {
			runPerformanceTest(db);
		}
	}

	@Test
	void testPostgresql_jooq_Connection() throws IOException {
		String host = postgres.getHost();
		Integer port = postgres.getFirstMappedPort();
		String dbName = PG_DBNAME + COUNTER++;

		createPostgresqlDatabase(host, port, dbName, PG_USERNAME, PG_PASSWORD);

		try (PostgresqlDB db = PostgresqlDB.createPostgreWithConnection(host, port, dbName, PG_USERNAME, PG_PASSWORD)) {
			runPerformanceTest(db);
		}
	}

	@Test
	void testPostgresql_Jdbc_Hikari() throws IOException, SQLException {
		String host = postgres.getHost();
		Integer port = postgres.getFirstMappedPort();
		String dbName = PG_DBNAME + COUNTER++;

		createPostgresqlDatabase(host, port, dbName, PG_USERNAME, PG_PASSWORD);

		try (PostgresqlDB db = PostgresqlDB.createPostgreWithHikari(host, port, dbName, PG_USERNAME, PG_PASSWORD)) {
			runPerformanceTest_JDBC(db);
		}
	}


	public static void createPostgresqlDatabase(String host, int port, String databaseName, String username, String password) {
		String url = "jdbc:postgresql://" + host + ":" + port + "/";
		Properties props = new Properties();
		props.setProperty("user", username);
		props.setProperty("password", password);
		try {
			Connection connection = DriverManager.getConnection(url, props);
			DSL.using(connection).createDatabase(databaseName).execute();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
