package middleware;

import java.net.URI;
import org.java_websocket.drafts.Draft_10;
import java.util.ArrayList;

import com.google.gson.Gson;
import readers.FeigReader;

/**
 * Client.
 * 
 * Handles communication with FEIG reader and WebSocket.
 */
public class Client implements TagListenerInterface, WebSocketListener {
	private TagReaderInterface tagReader;
	private WebSocketImpl webSocket;
	private LoggerImpl logger;
	private URI serverUri;
	private Gson gson;
	private String reader;
	private int successfulReadsThreshold;
	private int threadSleepInMillis;

	/**
	 * Constructor.
	 *
	 * @param reader
	 *   The name of the tag reader to initialize. Defaults to "feigReader".
	 * @param serverUri
	 *   The WebSocket URI.
	 * @param logger
	 *   The logger.
	 */
	public Client(String reader, URI serverUri, LoggerImpl logger, int successfulReadsThreshold, int threadSleepInMillis) {
		this.gson = new Gson();
		this.logger = logger;
		this.serverUri = serverUri;
		this.reader = reader;
		this.successfulReadsThreshold = successfulReadsThreshold;
		this.threadSleepInMillis = threadSleepInMillis;
	}
	
	/**
	 * Check that connections are up.
	 * 
	 * If not retry.
	 */
	public void checkConnections() {
		if (webSocket == null || !webSocket.isConnected()) {
			connectWebSocket();
		}
		
		if (tagReader == null || !tagReader.isRunning()) {
			// Setup new thread.
			switch (reader) {
				case "feig":
				default:
					tagReader = new FeigReader(logger, this, successfulReadsThreshold, threadSleepInMillis);
			}
			
			tagReader.startReading();
		}
		
		if (tagReader == null || !tagReader.isRunning()) {
			WebSocketMessage msg = new WebSocketMessage();
			msg.setEvent("rfid.offline");
			sendMessage(msg);
		}
		else {
			WebSocketMessage msg = new WebSocketMessage();
			msg.setEvent("rfid.online");
			sendMessage(msg);
		}
	}

	/**
	 * Is the WebSocket connected?
	 * 
	 * @return
	 */
	public boolean webSocketIsConnected() {
		return webSocket != null && webSocket.isConnected();
	}

	/**
	 * Send a message through the WebSocket.
	 * 
	 * Serializes the message to JSON and sends it if the socket is connected.
	 * 
	 * @param msg
	 *   The message to send.
	 */
	public void sendMessage(WebSocketMessage msg) {
		if (webSocketIsConnected()) {
			webSocket.send(gson.toJson(msg));
		}
	}

	/**
	 * Open a new WebSocket connection.
	 */
	public void connectWebSocket() {
		// Make sure the WebSocket is closed.
		if (webSocket != null) {
			webSocket.close();
		}
		
		webSocket = new WebSocketImpl(serverUri, new Draft_10(), this, logger);
		webSocket.connect();
	}

	/**
	 * Tag has been detected (TagListenerInterface).
	 * 
	 * Emit event through WebSocket.
	 */
	@Override
	public void tagDetected(BibTag bibTag) {
		logger.info("Tag detected: " + bibTag);
		
		WebSocketMessage resp = new WebSocketMessage();
		resp.setTag(bibTag);
		resp.setEvent("rfid.tag.detected");

		sendMessage(resp);
	}

	/**
	 * Tag has been removed (TagListenerInterface).
	 * 
	 * Emit event through WebSocket.
	 */
	@Override
	public void tagRemoved(BibTag bibTag) {
		logger.info("Tag removed: " + bibTag);
		
		WebSocketMessage resp = new WebSocketMessage();
		resp.setTag(bibTag);
		resp.setEvent("rfid.tag.removed");

		sendMessage(resp);
	}

	/**
	 * Tags have been detected (TagListenerInterface).
	 * 
	 * Emit event through WebSocket.
	 */
	@Override
	public void tagsDetected(ArrayList<BibTag> bibTags) {
		WebSocketMessage resp = new WebSocketMessage();
		resp.setTags(bibTags);
		resp.setEvent("rfid.tags.detected");

		sendMessage(resp);
	}

	/**
	 * Tag AFI has been attempted to be set (TagListenerInterface).
	 * 
	 * Emit event through WebSocket.
	 */
	@Override
	public void tagAFISet(BibTag bibTag, boolean success) {
		logger.info("Tag afi set " + (success ? "success" : "error") + ": " + bibTag);
		
		WebSocketMessage resp = new WebSocketMessage();
		resp.setTag(bibTag);
		resp.setSuccess(success);
		resp.setEvent("rfid.afi.set");
		
		sendMessage(resp);
	}
	
	/**
	 * New tags are being processed (TagListenerInterface).
	 * 
	 * Emit event through WebSocket.
	 */
	@Override
	public void processingNewTags() {
		WebSocketMessage resp = new WebSocketMessage();
		resp.setEvent("rfid.processing");
		sendMessage(resp);
	}

	/**
	 * Message has been received through WebSocket (TagListenerInterface).
	 */
	@Override
	public void webSocketMessage(String message) {
		logger.info("WebSocket: message RECEIVED: " + message);

		WebSocketMessage msg = gson.fromJson(message, WebSocketMessage.class);

		// detectTags Event
		if (msg.getEvent().equals("detectTags")) {
			tagReader.detectCurrentTags();
		}
		// setAFI Event
		else if (msg.getEvent().equals("setAFI")) {
			String afi = msg.getTag().getAFI();
			String uid = msg.getTag().getUID();

			if (afi != null && uid != null && !afi.equals("") && !uid.equals("")) {
				tagReader.addEventSetTagAFI(uid, afi);
			}
		}
	}
}