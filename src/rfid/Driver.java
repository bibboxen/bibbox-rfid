package rfid;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import org.java_websocket.drafts.Draft_10;

/**
 * Driver.
 * 
 * This class contains the main method which initiates the client with
 * config.properties.
 */
public class Driver {
	private static String host;
	private static Integer port;
	private static Boolean debug;
	private static LoggerImpl logger;
	private static ClientImpl client;

	/**
	 * Main entry point.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = "LogFile.log";
		File out = new File(System.getProperty("user.home"), filename);
		logger = new LoggerImpl(out.getAbsolutePath());

		// Read config.properties.
		if (!readConfiguration()) {
			logger.log("config.properties could not be found");
			
			// Defaults.
			port = 3001;
			host = "localhost";
			debug = false;
		}

		// Start client.
		try {
			client = new ClientImpl(new URI("ws://" + host + ":" + port), logger, debug);

			// Make sure the Client is connected every 10 s. 
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					client.checkConnections();
				}
			}, 5000, 10000);
		} catch (Exception e) {
			logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
		}
	}

	/**
	 * Read configuration from properties file.
	 */
	public static boolean readConfiguration() {
		GetPropertiesImpl properties = new GetPropertiesImpl();

		try {
			properties.setPropValues();
			host = properties.getHostProperty();
			port = properties.getPortProperty();
			debug = properties.getDebugProperty();
			return true;

		} catch (IOException ex) {
			logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
			return false;
		}
	}
}
