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
			System.out.println("Could not read config.properties file. Make sure it is located next to .jar file");
			logger.log("config.properties could not be found");
		}

		if (host == null || host.equals("") || host == "") {
			host = "localhost";
		}

		// Start client.
		// Timer that checks if client is connected. If not, try create an new
		// connection.
		try {
			client = new ClientImpl(new URI("ws://" + host + ":" + port), new Draft_10(), logger, debug);
			client.connect();

			Timer t = new Timer();
			t.schedule(new TimerTask() {
				@Override
				public void run() {
					if (!client.isConnected()) {
						System.out.println("Websocket client not connected");
						logger.log("Websocket client not connected");
						createNewWebSocketInstance();
					}
				}
			}, 5000, 10000); // calls run every tenth second
		} catch (URISyntaxException ex) {
			logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Close WebSocket client and set to null. Create new instance of the
	 * WebSocket client.
	 */

	public static void createNewWebSocketInstance() {
		client.close();
		client = null;
		try {
			System.out.println("Websocket client: TRYING TO CONNECT TO:  " + host + ":" + port);
			logger.log("Websocket client trying to connect to " + host + ":" + port);
			client = new ClientImpl(new URI("ws://" + host + ":" + port), new Draft_10(), logger, debug);
			client.connect();
		} catch (URISyntaxException ex) {
			logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
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
