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
	private static int successfulReadsThreshold = 2;
	private static int threadSleepInMillis = 200;

	/**
	 * Main entry point.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Read config.properties.
		if (!readConfiguration()) {
			// Defaults.
			port = 3001;
			host = "localhost";
			loglevel = "prod";
			logtofile = false;
			logtoconsole = false;
			reader = "feig";
		}
		
		// Command line arguments override config.properties.
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--help")) {
				System.out.println("Run with options set through commandline arguments: key=value. E.g. java -jar rfid.jar port=5000");
				System.out.println("Alternatively place a config.properties in the same directory as the jar file with arguments.");
				System.out.println("Default options are:");
				System.out.println("port=3001 host=localhost loglevel=prod logtofile=false logtoconsole=false reader=feig thread_sleep_in_millis=200 successful_reads_threshold=2");
			}
			
			String[] split = args[i].split("=");
			
			if (split.length == 2) {
				System.out.println(args[i]);
				
				switch(split[0]) {
					case "port":
						port = Integer.parseInt(split[1]);
						break;

					case "host":
						host = split[1];
						break;

					case "loglevel":
						loglevel = split[1];
						break;

					case "logtofile":
						logtofile = Boolean.parseBoolean(split[1]);
						break;

					case "logtoconsole":
						logtoconsole = Boolean.parseBoolean(split[1]);
						break;

					case "successful_reads_threshold":
						successfulReadsThreshold = Integer.parseInt(split[1]);
						break;

					case "thread_sleep_in_millis":
						threadSleepInMillis = Integer.parseInt(split[1]);
						break;

					case "reader":
						reader = split[1];
						break;
				}
			}
		}
		
		// Setup Logger.
		String filename = "rfid.log";
		File out = new File(System.getProperty("user.home"), filename);
		logger = new LoggerImpl(out.getAbsolutePath(), loglevel, logtofile, logtoconsole);
		
		logger.info(
				"Starting client with options --- " 
						+ "ws: " + host + ":" + port
						+ ", logLevel: " + loglevel
						+ ", logToFile: " + logtofile
						+ ", logToConsole: " + logtoconsole
						+ ", toLogFile: " + out.getAbsolutePath()
						+ ", successfulReadsThreshold: " + successfulReadsThreshold
						+ ", threadSleepInMillis: " + threadSleepInMillis
						+ ", reader: " + reader);
		
		// Start client.
		try {
			client = new Client(reader, new URI("ws://" + host + ":" + port), logger, successfulReadsThreshold, threadSleepInMillis);

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
