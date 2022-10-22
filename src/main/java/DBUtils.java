import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DBUtils {

	public enum DatabaseType { SQLITE, POSTGRESQL }

	public static final Map<DatabaseType, Map<String, String>> MIGRATION_PLACEHOLDERS;

	static {
		MIGRATION_PLACEHOLDERS = new ConcurrentHashMap<>();
		Map<String, String> sqlitePlaceholders = new ConcurrentHashMap<>();
		Map<String, String> postgresqlPlaceholders = new ConcurrentHashMap<>();
		MIGRATION_PLACEHOLDERS.put(DatabaseType.SQLITE, sqlitePlaceholders);
		MIGRATION_PLACEHOLDERS.put(DatabaseType.POSTGRESQL, postgresqlPlaceholders);

		sqlitePlaceholders.put("identityColumnType", "INTEGER PRIMARY KEY");
		postgresqlPlaceholders.put("identityColumnType", "BIGSERIAL PRIMARY KEY");
	}

	public static void runFlyway(final String dbName, final String connectionString, final String username, final String password, String migrationsPath, DatabaseType dbType) {
		Flyway flyway = Flyway.configure()
			.locations("classpath:" + migrationsPath)
			.dataSource(connectionString, username, password)
			.placeholders(MIGRATION_PLACEHOLDERS.get(dbType))
			.load();
		runFlyway(dbName, flyway);
	}

	public static void runFlyway(String dbName, DataSource dataSource, String migrationsPath, DatabaseType dbType) {
		Flyway flyway = Flyway.configure()
			.locations("classpath:" + migrationsPath)
			.dataSource(dataSource)
			.placeholders(MIGRATION_PLACEHOLDERS.get(dbType))
			.load();
		runFlyway(dbName, flyway);
	}

	private static void runFlyway(String dbName, Flyway flyway) {
		String oldVersion = (flyway.info().current() == null ? "clean state" : flyway.info().current().getVersion().toString());
		MigrateResult result = flyway.migrate();
		if (result.migrationsExecuted > 0) {
			String message = "database " + dbName + " updated to version " + flyway.info().current().getVersion().toString() + " from version " + oldVersion;
			System.out.println(message);
		} else if (flyway.info().current() != null) {
			String message = "database " + dbName + " left unchanged at version " + flyway.info().current().getVersion().toString();
			System.out.println(message);
		} else {
			System.out.println("database could not be initialized.");
		}
	}

}
