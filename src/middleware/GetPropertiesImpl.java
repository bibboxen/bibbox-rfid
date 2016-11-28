package middleware;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * GetProperties.
 * 
 * This class is used to read the config.properties file. The
 * config.properties file must be located next to the .jar file
 */
public class GetPropertiesImpl {
	private String host;
	private Integer port;
	private String reader;
	private String loglevel;
	private Boolean logtofile;
	private Boolean logtoconsole;

	/**
	 * Set Properties values.
	 * 
	 * @throws IOException
	 */
	public void setPropValues() throws IOException {
		Properties prop = new Properties();

		FileInputStream file;

		// The config.properties file must be located next to the executable .jar file.
		String path = "./config.properties";
		file = new FileInputStream(path);

		try {
			prop.load(file);

			host = prop.getProperty("host");
			port = Integer.parseInt(prop.getProperty("port"));
			loglevel = prop.getProperty("loglevel");
			logtofile = Boolean.parseBoolean(prop.getProperty("logtofile"));
			logtoconsole = Boolean.parseBoolean(prop.getProperty("logtoconsole"));
			reader = prop.getProperty("reader");
		} catch (Exception ex) {
			// @TODO: Handle error.
		}

		file.close();
	}

	/**
	 * Get host.
	 * 
	 * @return String
	 */
	public String getHostProperty() {
		return this.host;
	}

	/**
	 * Get port.
	 * 
	 * @return Integer
	 */
	public int getPortProperty() {
		return this.port;
	}
	
	/**
	 * Get loglevel.
	 * 
	 * @return String
	 */
	public String getLogLevelProperty() {
		return this.loglevel;
	}
	
	/**
	 * Get logtofile.
	 * 
	 * @return Boolean
	 */
	public Boolean getLogToFileProperty() {
		return this.logtofile;
	}	
	
	/**
	 * Get logtoconsole.
	 * 
	 * @return Boolean
	 */
	public Boolean getLogToConsoleProperty() {
		return this.logtoconsole;
	}
	
	/**
	 * Get reader.
	 * 
	 * @return String
	 */
	public String getReaderProperty() {
		return this.reader;
	}
}