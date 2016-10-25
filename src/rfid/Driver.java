package rfid;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.java_websocket.drafts.Draft_10;

/**
 * Driver.
 * 
 * This class contains the main method which initiates the client with the config properties.
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
		String filename = "LogFile.log";
		File out = new File(System.getProperty("user.home"), filename);

		logger = new LoggerImpl(out.getAbsolutePath());

		getConfigInformations();

		if (host == null || host.equals("") || host == "") {
			host = "localhost";
		}

		ClientImpl client;
		try {
			client = new ClientImpl(new URI("ws://" + host + ":" + port), new Draft_10(), logger);
			client.connect();
		} catch (URISyntaxException ex) {
			logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
		}

	}

	/**
	 * Get configuration from properties file.
	 */
	public static void getConfigInformations() {
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
