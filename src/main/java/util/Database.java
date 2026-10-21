package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
	 * Si la base de datos SQLite no tiene tablas (archivo vacío/nuevo), la inicializa automáticamente.
	 */
	public Database() {
		this(null);
	}

	/**
	 * Como el constructor sin parámetros, pero con una URL de base de datos específica (por ejemplo para pruebas
	 * unitarias). La comprobación e inicialización de arranque se hacen sobre esta URL, no sobre la configurada.
	 */
	public Database(String customUrl) {
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
		if (url != null && url.startsWith("jdbc:sqlite:")) {
			// Separar la ruta del archivo de los query params (?foreign_keys=true, etc.)
			String afterPrefix = url.substring("jdbc:sqlite:".length());
			String queryParams = "";
			int qIdx = afterPrefix.indexOf('?');
			if (qIdx != -1) {
				queryParams = afterPrefix.substring(qIdx);
				afterPrefix = afterPrefix.substring(0, qIdx);
			}

			// Solo resolver rutas relativas simples (sin / ni \)
			if (!afterPrefix.contains("/") && !afterPrefix.contains("\\")) {
				File dbDirecto = new File(afterPrefix);
				File dbSubdir = new File("IPS2026-PL22", afterPrefix);
				if (dbSubdir.exists() && dbSubdir.length() > 0
						&& (!dbDirecto.exists() || dbDirecto.length() == 0)) {
					url = "jdbc:sqlite:IPS2026-PL22/" + afterPrefix + queryParams;
				}
			}
		}

		if (customUrl != null && !customUrl.trim().isEmpty()) {
			url = customUrl.trim();
		}
		if (driver == null || url == null) {
			throw new ApplicationException("Configuracion de driver y/o url no encontrada en application.properties");
		}
		DbUtils.loadDriver(driver);

		// 4. Si la BD no tiene las tablas del esquema, inicializarla automáticamente.
		//    IMPORTANTE: la comprobación de lectura debe cerrarse ANTES de llamar a
		//    createDatabase/loadDatabase, que abren conexiones de escritura propias.
		//    SQLite solo admite un writer a la vez → SQLITE_BUSY si se anidan conexiones.
		checkAndInitDatabase();
	}

	/**
	 * Si la BD no tiene tablas (archivo nuevo) ejecuta schema.sql + data.sql.
	 * Una BD con tablas nunca se toca, aunque alguna esté vacía: la carga es transaccional
	 * (executeBatch), así que no puede quedar a medias. Si no se puede leer el estado, se lanza
	 * la excepción en vez de recrear la BD y perder los datos.
	 *
	 * La conexión de lectura se cierra antes de que executeBatch abra conexiones de escritura.
	 */
	private void checkAndInitDatabase() {
		boolean tableExists;
		try (Connection conn = getConnection();
			 Statement stmt = conn.createStatement();
			 ResultSet rs = stmt.executeQuery(
					"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='Grado'")) {
			tableExists = rs.next() && rs.getInt(1) > 0;
		} catch (Exception ex) {
			throw new ApplicationException(ex);
		}

		if (!tableExists) {
			System.out.println("BD sin tablas detectada, ejecutando schema.sql y data.sql...");
			createDatabase(false);
			loadDatabase();
		}
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
