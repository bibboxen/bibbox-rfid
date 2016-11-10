package rfid;

import java.net.URI;
import org.java_websocket.drafts.Draft_10;
import java.util.ArrayList;
import com.google.gson.Gson;

/**
 * Client.
 * 
 * Handles communication with FEIG reader and WebSocket.
 */
public class ClientImpl implements TagListenerInterface, WebSocketListener {
	private TagReaderInterface tagReader;
	private WebSocketImpl webSocket;
	private LoggerImpl logger;
	private Boolean debug;
	private URI serverUri;
	private Gson gson;

	/**
	 * Constructor.
	 *
	 * @param serverUri
	 * @param draft
	 * @param logger
	 */
	public ClientImpl(URI serverUri, LoggerImpl logger, Boolean debug) {
		this.logger = logger;
		this.debug = debug;
		this.serverUri = serverUri;
		
		// Initialize TagReader.		
		tagReader = new FeigReader(logger, this, debug);

		this.gson = new Gson();
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
		
		if (!tagReader.isRunning()) {
			tagReader.startReading();
		}
	}

	/**
	 * Is the WebSocket connected?
	 * 
	 * @return
	 */
	public Boolean webSocketIsConnected() {
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
		webSocket = new WebSocketImpl(serverUri, new Draft_10(), this, logger);
		webSocket.connect();
	}

	/**
	 * Tag has been detected (TagListenerInterface).
	 * 
	 * Emit event to server.
	 */
	@Override
	public void tagDetected(BibTag bibTag) {
		if (debug) {
			System.out.println("Tag detected: " + bibTag);		
		}
		
		WebSocketMessage resp = new WebSocketMessage();
		resp.setTag(bibTag);
		resp.setEvent("rfid.tag.detected");

		sendMessage(resp);
	}

	/**
	 * Tag has been removed (TagListenerInterface).
	 * 
	 * Emit event to server.
	 */
	@Override
	public void tagRemoved(BibTag bibTag) {
		if (debug) {
			System.out.println("Tag removed: " + bibTag);		
		}
		
		WebSocketMessage resp = new WebSocketMessage();
		resp.setTag(bibTag);
		resp.setEvent("rfid.tag.removed");

		sendMessage(resp);
	}

	@Override
	public void tagsDetected(ArrayList<BibTag> bibTags) {
		WebSocketMessage resp = new WebSocketMessage();
		resp.setTags(bibTags);
		resp.setEvent("rfid.tags.detected");

		sendMessage(resp);
	}

	@Override
	public void tagAFISet(BibTag bibTag, Boolean success) {
		if (debug) {
			System.out.println("Tag afi set " + (success ? "success" : "error") + ": " + bibTag);		
		}
		
		WebSocketMessage resp = new WebSocketMessage();
		resp.setTag(bibTag);
		resp.setSuccess(success);
		resp.setEvent("rfid.afi.set");
		
		sendMessage(resp);
	}

	@Override
	public void webSocketMessage(String message) {
		if (debug) {
			System.out.println("WebSocket: message RECEIVED: " + message);
		}

		WebSocketMessage msg = gson.fromJson(message, WebSocketMessage.class);

		// detectTags Event
		if (msg.getEvent().equals("detectTags")) {
			tagReader.detectCurrentTags();
		}
		// setAFI Event
		else if (msg.getEvent().equals("setAFI")) {
			try {
				String afi = msg.getTag().getAFI();
				String uid = msg.getTag().getUID();

				if (afi != null && uid != null && !afi.equals("") && !uid.equals("")) {
					tagReader.addEventSetTagAFI(uid, afi);
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
				
				WebSocketMessage resp = new WebSocketMessage();
				resp.setEvent("rfid.error");
				resp.setMessage(e.getMessage());
				sendMessage(resp);
			}
		}
	}
}