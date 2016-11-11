package middleware;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

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
	private static String reader;
	private static LoggerImpl logger;
	private static Client client;

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
			reader = "feig";
		}

		// Start client.
		try {
			client = new Client(reader, new URI("ws://" + host + ":" + port), logger, debug);

			// Make sure the Client is connected every 10 s. 
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					client.checkConnections();
				}
			}, 5000, 10000);
		} catch (Exception e) {
			e.printStackTrace();
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
			reader = properties.getReaderProperty();
			
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
			
			return false;
		}
	}
}
