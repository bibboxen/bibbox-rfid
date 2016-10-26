package rfid;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.java_websocket.drafts.Draft_10;

/**
 * Driver.
 * 
 * This class contains the main method which initiates the client with config.properties.
 */
public class Driver {
	private static String host;
	private static int port;
	private static LoggerImpl logger;

	/**
	 * Main entry point.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Read config.properties.
		readConfiguration();

		// @TODO: Read from config.properties.
		String filename = "LogFile.log";
		File out = new File(System.getProperty("user.home"), filename);
		logger = new LoggerImpl(out.getAbsolutePath());

		if (host == null || host.equals("") || host == "") {
			host = "localhost";
		}

		// Start client.
		ClientImpl client;
		try {
			client = new ClientImpl(new URI("ws://" + host + ":" + port), new Draft_10(), logger);
			client.connect();
		} catch (URISyntaxException ex) {
			logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
		}
	}

	/**
	 * Read configuration from properties file.
	 */
	public static void readConfiguration() {
		GetPropertiesImpl properties = new GetPropertiesImpl();

		try {
			properties.setPropValues();
			host = properties.getHostProperty();
			port = properties.getPortProperty();

		} catch (IOException ex) {
			logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
		}
	}
}
