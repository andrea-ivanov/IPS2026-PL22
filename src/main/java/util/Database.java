package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.dbutils.DbUtils;

/**
 * Encapsula los datos de acceso JDBC, lectura de la configuracion
 * y scripts de base de datos para creacion y carga.
 * Soporta carga tanto desde classpath como desde disco relativo a la raíz del workspace o proyecto.
 */
public class Database extends DbUtil {
	// Localizacion de ficheros de configuracion y carga de bases de datos
	private static final String APP_PROPERTIES = "src/main/resources/application.properties";
	private static final String SQL_SCHEMA = "src/main/resources/schema.sql";
	private static final String SQL_LOAD = "src/main/resources/data.sql";

	// Parametros de la base de datos leidos de application.properties
	private String driver;
	private String url;
	private static boolean databaseCreated = false;

	/**
	 * Crea una instancia, leyendo los parametros de driver y url de application.properties.
	 * Tolera ejecuciones desde el directorio raíz del repositorio (IPS) o del subproyecto (IPS2026-PL22),
	 * así como desde empaquetado JAR mediante el ClassLoader.
	 */
	public Database() {
		Properties prop = new Properties();
		InputStream in = null;

		// 1. Intentar cargar desde classpath (funciona siempre que esté en target/classes o src/main/resources)
		in = getClass().getClassLoader().getResourceAsStream("application.properties");
		if (in == null) {
			in = Database.class.getResourceAsStream("/application.properties");
		}

		// 2. Si no se encuentra en classpath, buscar por rutas relativas en disco
		if (in == null) {
			String[] posiblesRutas = new String[] {
				APP_PROPERTIES,
				"IPS2026-PL22/" + APP_PROPERTIES
			};
			for (String ruta : posiblesRutas) {
				File f = new File(ruta);
				if (f.exists() && f.isFile()) {
					try {
						in = new FileInputStream(f);
						break;
					} catch (IOException ignored) {
					}
				}
			}
		}

		if (in != null) {
			try (InputStream is = in) {
				prop.load(is);
			} catch (IOException e) {
				throw new ApplicationException(e);
			}
		}

		driver = prop.getProperty("datasource.driver", "org.sqlite.JDBC");
		url = prop.getProperty("datasource.url", "jdbc:sqlite:DemoDB.db");

		// 3. Si la URL apunta a una base de datos local SQLite relativa, resolver si reside en subcarpeta
		if (url != null && url.startsWith("jdbc:sqlite:") && !url.contains("/")) {
			String dbFileName = url.substring("jdbc:sqlite:".length());
			File dbDirecto = new File(dbFileName);
			File dbSubdir = new File("IPS2026-PL22/" + dbFileName);
			if (!dbDirecto.exists() && dbSubdir.exists()) {
				url = "jdbc:sqlite:IPS2026-PL22/" + dbFileName;
			}
		}

		if (driver == null || url == null) {
			throw new ApplicationException("Configuracion de driver y/o url no encontrada en application.properties");
		}
		DbUtils.loadDriver(driver);
	}

	public String getUrl() {
		return url;
	}

	/** 
	 * Creacion de una base de datos limpia a partir del script schema.sql
	 * (si onlyOnce=true solo ejecutara el script la primera vez para pruebas)
	 */
	public void createDatabase(boolean onlyOnce) {
		if (!databaseCreated || !onlyOnce) {
			executeScript(SQL_SCHEMA);
			databaseCreated = true; // NOSONAR
		}
	}

	/** 
	 * Carga de datos iniciales a partir del script data.sql
	 */
	public void loadDatabase() {
		executeScript(SQL_LOAD);
	}
}
