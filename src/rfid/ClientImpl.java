package rfid;

import java.net.URI;

import org.java_websocket.drafts.Draft_10;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Client.
 * 
 * Handles communication with FEIG reader and WebSocket.
 * 
 * @TODO: Replace org.json.simple.JSON* with GSON:
 *        https://github.com/google/gson to avoid type warnings.
 */
public class ClientImpl implements TagListenerInterface, WebSocketListener {
	private TagReaderInterface tagReader;
	private WebSocketImpl webSocket;
	private LoggerImpl logger;
	private Boolean debug;
	private URI serverUri;

	
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
		
		if (webSocket.isConnected()) {
			JSONObject tag = new JSONObject();
			tag.put("uid", bibTag.getUID());
			tag.put("mid", bibTag.getMID());

			JSONObject json = new JSONObject();
			json.put("tag", tag);
			json.put("event", "rfid.tag.detected");

			webSocket.send(json.toJSONString());
		}
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
		
		if (webSocket.isConnected()) {
			JSONObject tag = new JSONObject();
			tag.put("uid", bibTag.getUID());
			tag.put("mid", bibTag.getMID());

			JSONObject json = new JSONObject();
			json.put("tag", tag);
			json.put("event", "rfid.tag.removed");

			webSocket.send(json.toJSONString());
		}
	}

	@Override
	public void tagsDetected(ArrayList<BibTag> bibTags) {
		if (webSocket.isConnected()) {
			JSONArray jsonArray = new JSONArray();

			// Add bibTags
			for (BibTag bibTag : bibTags) {
				JSONObject json = new JSONObject();
				json.put("uid", bibTag.getUID());
				json.put("mid", bibTag.getMID());
				jsonArray.add(json);
			}

			// Setup return object
			JSONObject returnObj = new JSONObject();
			returnObj.put("tags", jsonArray);
			returnObj.put("event", "rfid.tags.detected");
			webSocket.send(returnObj.toJSONString());
		}
	}

	@Override
	public void tagAFISet(BibTag bibTag, Boolean success) {
		if (webSocket.isConnected()) {
			if (debug) {
				System.out.println("Tag afi set " + (success ? "success" : "error") + ": " + bibTag);		
			}
			
			JSONObject tag = new JSONObject();
			tag.put("uid", bibTag.getUID());
			tag.put("mid", bibTag.getMID());
			tag.put("afi", bibTag.getAFI());

			JSONObject json = new JSONObject();
			json.put("tag", tag);
			json.put("success", success);
			json.put("event", "rfid.afi.set");

			webSocket.send(json.toJSONString());
		}		
	}

	@Override
	public void webSocketMessage(String message) {
		JSONParser parser = new JSONParser();

		if (debug) {
			System.out.println("WebSocket: message RECEIVED: " + message);
		}

		try {
			JSONObject jsonMessage = (JSONObject) parser.parse(message);

			// detectTags Event
			if (jsonMessage.get("event").equals("detectTags")) {
				tagReader.detectCurrentTags();
			}
			// setAFI Event
			else if (jsonMessage.get("event").equals("setAFI")) {
				try {
					String afi = jsonMessage.get("afi").toString();
					String uid = jsonMessage.get("uid").toString();

					// Only set tag and 
					if (!uid.equals("") && uid != null && !afi.equals("") && afi != null) {
						tagReader.addEventSetTagAFI(uid, afi);
					}
				} catch (Exception e) {
					e.printStackTrace();
					logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
				}
			}
		} catch (ParseException ex) {
			ex.printStackTrace();
			logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());

			if (webSocket.isConnected()) {
				JSONObject callback = new JSONObject();
				callback.put("event", "error");
				callback.put("message", ex.getMessage());
				webSocket.send(callback.toJSONString());
			}			
		}
	}
}