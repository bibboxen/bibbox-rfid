package rfid;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GetPropertiesImpl {

	/*
	 * This class is used to read the config.properties file. The
	 * config.properties file must be located next to the .jar file
	 */

	private String host;
	private int port;

	public void setPropValues() throws IOException {

		Properties prop = new Properties();

		FileInputStream file;
		String path = "./config.properties";// config.properties file must be
											// located next to the executable
											// .jar file.
		file = new FileInputStream(path);

		try {
			prop.load(file);

			host = prop.getProperty("host");
			port = Integer.parseInt(prop.getProperty("port"));
		} catch (Exception ex) {
			//
		}

		file.close();
	}

	public String getHostProperty() {
		return this.host;
	}

	public int getPortProperty() {
		return this.port;
	}
}
