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
	private static String loglevel;
	private static String reader;
	private static LoggerImpl logger;
	private static Client client;
	private static boolean logtofile;
	private static boolean logtoconsole;

	/**
	 * Main entry point.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Read config.properties.
		if (!readConfiguration()) {
			System.out.println("config.properties could not be found");
			// Defaults.
			port = 3001;
			host = "localhost";
			loglevel = "prod";
			logtofile = false;
			logtoconsole = false;
			reader = "feig";
		}

		String filename = "rfid.log";
		File out = new File(System.getProperty("user.home"), filename);
		logger = new LoggerImpl(out.getAbsolutePath(), loglevel, logtofile, logtoconsole);

		logger.info(
				"Starting client with options --- " 
						+ "ws: " + host + ":" + port
						+ ", logLevel: " + loglevel
						+ ", logToFile: " + logtofile
						+ ", toLogFile: "+ out.getAbsolutePath()
						+ ", reader: " + reader);
		
		// Start client.
		try {
			client = new Client(reader, new URI("ws://" + host + ":" + port), logger);

			// Make sure the Client is connected every 10 s. 
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					client.checkConnections();
				}
			}, 5000, 10000);
		} catch (Exception e) {
			logger.error("Error message: " + e.getMessage() + "\n" + e.getStackTrace());
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
			loglevel = properties.getLogLevelProperty();
			logtofile = properties.getLogToFileProperty();
			logtoconsole = properties.getLogToConsoleProperty();
			reader = properties.getReaderProperty();
			
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
