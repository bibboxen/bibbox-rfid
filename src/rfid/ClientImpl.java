package rfid;

import java.net.URI;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
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
public class ClientImpl extends WebSocketClient implements TagListenerInterface {
	private TagReaderInterface tagReader;
	private LoggerImpl logger;
	private Boolean connected;
	private Boolean debug;

	/**
	 * Constructor.
	 * 
	 * @TODO: Move WebSocket implementation into separate class.
	 * @TODO: Move FEIG implementation into separate class.
	 * 
	 * @param serverUri
	 * @param draft
	 * @param logger
	 */
	public ClientImpl(URI serverUri, Draft draft, LoggerImpl logger, Boolean debug) {
		// Call constructor of WebSocketClient.
		super(serverUri, draft);

		this.logger = logger;
		this.debug = debug;
		
		// Initialize TagReader.
		tagReader = new FeigReader(logger, this, debug);
		tagReader.startReading();
	}

	/**
	 * onOpen WebSocket.
	 */
	@Override
	public void onOpen(ServerHandshake sh) {
		// WebSocket client connected to server
		logger.log("WebSocket: connection OPEN");

		// Send connected to server.
		JSONObject json = new JSONObject();
		json.put("event", "connected");
		send(json.toJSONString());

		connected = true;
	}

	/**
	 * onMessage WebSocket.
	 * 
	 * This method is called whenever the WebSocket client receives a message.
	 */
	@Override
	public void onMessage(String message) {
		JSONParser parser = new JSONParser();

		logger.log("WebSocket: message RECEIVED: " + message);

		try {
			JSONObject jsonMessage = (JSONObject) parser.parse(message);

			// detectTags Event
			if (jsonMessage.get("event").equals("detectTags")) {
				tagReader.detectCurrentTags();
			}
			// setAFI Event
			else if (jsonMessage.get("event").equals("setAFI")) {
				try {
					String afi = jsonMessage.get("AFI").toString();
					String uid = jsonMessage.get("UID").toString();

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

			JSONObject callback = new JSONObject();
			callback.put("event", "error");
			callback.put("message", ex.getMessage());
			send(callback.toJSONString());
		}
	}

	/**
	 * onClose WebSocket.
	 * 
	 * @TODO: Reconnect.
	 */
	@Override
	public void onClose(int i, String string, boolean bln) {
		// this method is called when connection to websocket server is closed.
		logger.log("WebSocket: connection CLOSED");

		if (tagReader.isRunning()) {
			tagReader.disconnect();
		}

		connected = false;
	}

	/**
	 * onError WebSocket.
	 */
	@Override
	public void onError(Exception ex) {
		logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());

		if (tagReader.isRunning()) {
			tagReader.disconnect();
		}

		connected = false;
	}
	
	public boolean isConnected() {
		return this.connected;
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
		
		if (connected) {
			JSONObject tag = new JSONObject();
			tag.put("UID", bibTag.getUID());
			tag.put("MID", bibTag.getMID());

			JSONObject json = new JSONObject();
			json.put("tag", tag);
			json.put("event", "tagDetected");

			send(json.toJSONString());
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
		
		if (connected) {
			JSONObject tag = new JSONObject();
			tag.put("UID", bibTag.getUID());
			tag.put("MID", bibTag.getMID());

			JSONObject json = new JSONObject();
			json.put("tag", tag);
			json.put("event", "tagRemoved");

			send(json.toJSONString());
		}
	}

	@Override
	public void tagsDetected(ArrayList<BibTag> bibTags) {
		if (connected) {
			JSONArray jsonArray = new JSONArray();

			// Add bibTags
			for (BibTag bibTag : bibTags) {
				JSONObject json = new JSONObject();
				json.put("UID", bibTag.getUID());
				json.put("MID", bibTag.getMID());
				jsonArray.add(json);
			}

			// Setup return object
			JSONObject returnObj = new JSONObject();
			returnObj.put("tags", jsonArray);
			returnObj.put("event", "tagsDetected");
			send(returnObj.toJSONString());
		}
	}

	@Override
	public void tagAFISetSuccess(BibTag bibTag) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void tagAFISetFailure(BibTag bibTag) {
		// TODO Auto-generated method stub
		
	}
}