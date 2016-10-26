package rfid;

import de.feig.FeIscListener;
import de.feig.FePortDriverException;
import de.feig.FeReaderDriverException;
import de.feig.FedmException;
import de.feig.FedmIscReader;
import de.feig.FedmIscReaderConst;
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
 * Handles communication with Feig reader and WebSocket.
 */
public class ClientImpl extends WebSocketClient implements TagListenerInterface, FeIscListener {
	private FedmIscReader fedm;
	private TagReader tagReader;
	private ArrayList<BibTag> bibTags;
	private LoggerImpl logger;
	private Boolean connected = false;

	/**
	 * Constructor.
	 * 
	 * @TODO: Move WebSocket implementation into separate class.
	 * @TODO: Move Feig implementation into separate class.
	 * 
	 * @param serverUri
	 * @param draft
	 * @param logger
	 */
	public ClientImpl(URI serverUri, Draft draft, LoggerImpl logger) {
		// Call constructor of WebSocketClient.
		super(serverUri, draft);

		this.logger = logger;

		// Initialize Feig Reader.
		if (!initiateFeigReader()) {
			// @TODO: Emit error.
			logger.log("FEIG Reader: Error - CANNOT INITIALIZE");
		}
		
		// Open USBPort
		if (openUSBPort()) {
			// @TODO: Emit.
			logger.log("USB Connection: ESTABLISHED");
		} 
		else {
			// @TODO: Emit error.
			logger.log("USB Connection: Error - NO USB CONNECTION");
		}

		// Start TagReader.
		tagReader = new TagReader(this, fedm, logger);
		tagReader.start();
	}

	/**
	 * Initialize FeigReader
	 * 
	 * @return Boolean
	 */
	public boolean initiateFeigReader() {
		// Initiate the reader object.
		try {
			fedm = new FedmIscReader();
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
		}

		if (fedm == null) {
			return false;
		}
		
		// Set the table size of the reader.
		// As of now it has been set to 50
		// which means the reader can read an inventory of max 50.
		// This can be set to for instance 100.
		try {
			fedm.setTableSize(FedmIscReaderConst.ISO_TABLE, 50);
			return true;
		} catch (FedmException ex) {
			ex.printStackTrace();
			logger.log("Error code: " + ex.getErrorcode() + "\n" + ex.getStackTrace());
		}
		return false;
	}

	/**
	 * Open USB port.
	 * 
	 * @return Boolean
	 */
	public boolean openUSBPort() {
		// Close connection if any has already been established.
		closeConnection();

		// Connect to usb
		try {
			fedm.connectUSB(0);
			fedm.addEventListener(this, FeIscListener.SEND_STRING_EVENT);
			fedm.addEventListener(this, FeIscListener.RECEIVE_STRING_EVENT);

			// Read important reader properties and set reader type in reader
			// object
			fedm.readReaderInfo();
			return true;
		} catch (FedmException ex) {
			logger.log("Error code: " + ex.getErrorcode() + "\n" + ex.toString());
			ex.printStackTrace();
		} catch (FePortDriverException ex) {
			logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
			ex.printStackTrace();
		} catch (FeReaderDriverException ex) {
			logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
			ex.printStackTrace();
		}
		return false;
	}

	/**
	 * Close connection to FeigReader.
	 */
	private void closeConnection() {
		// close connection there is any
		if (fedm.isConnected()) {
			try {
				fedm.removeEventListener(this, FeIscListener.SEND_STRING_EVENT);
				fedm.removeEventListener(this, FeIscListener.RECEIVE_STRING_EVENT);
				fedm.disConnect();
			} catch (FedmException ex) {
				ex.printStackTrace();
				logger.log("Error code: " + ex.getErrorcode() + "\n" + ex.toString());
			} catch (FePortDriverException ex) {
				ex.printStackTrace();
				logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
			} catch (FeReaderDriverException ex) {
				ex.printStackTrace();
				logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
			}
		}
	}

	/**
	 * onOpen WebSocket.
	 */
	@Override
	public void onOpen(ServerHandshake sh) {
		// websocket client connected to websocket server
		logger.log("WebSocket: connection OPEN");
		JSONObject json = new JSONObject();
		json.put("event", "connected");
		send(json.toJSONString());
		
		connected = true;
	}

	/**
	 * onMessage WebSocket.
	 * 
	 * Handle events from socket.
	 */
	@Override
	public void onMessage(String message) {
		// This method is called whenever the websocket client receives a
		// message.
		logger.log("WebSocket: message RECEIVED: " + message);
		try {
			JSONParser parser = new JSONParser();
			JSONObject jsonMessage = (JSONObject) parser.parse(message);
			JSONObject callback = new JSONObject();

			if (jsonMessage.get("event").equals("detectTags")) {
				tagReader.setState("detectTags");

			} else if (jsonMessage.get("event").equals("tagSet")) {
				// TODO
				// send success/failure + the uid of the success/failed tag if
				// possible
				String mid = jsonMessage.get("mid").toString();
				String afi = jsonMessage.get("afi").toString();

				if (mid != null && !mid.equals("") && mid != "") {
					// there is an MID in message
					if (afi != null && !afi.equals("") && afi != null) {
						// there is an AFI in message
						tagReader.setMID(mid);
						tagReader.setAFI(afi);
						tagReader.setState("tagSet");
						callback.put("callback", "true");
						send(callback.toJSONString());
					} else {
						// System.out.println("You need to insert an AFI");
						callback.put("MID", mid);
						callback.put("callback", "false");
						send(callback.toJSONString());
					}
				} else {
					// System.out.println("You need to insert an MID");
					callback.put("callback", "false");
					send(callback.toJSONString());
				}

			} else if (jsonMessage.get("event").equals("tagSetAFI")) {
				try {
					String afi = jsonMessage.get("afi").toString();
					String uid = jsonMessage.get("uid").toString();

					if (!uid.equals("") && uid != null && uid != "") {
						if (!afi.equals("") && afi != null && afi != "") {
							tagReader.setUidToWriteAfiTo(uid);
							tagReader.setAFI(afi);
							tagReader.setState("tagSetAFIOnUID");
							callback.put("callback", "true");
							send(callback.toJSONString());
						}
					} else {
						if (!afi.equals("") && afi != null && afi != "") {
							tagReader.setAFI(afi);
							tagReader.setState("tagSetAFI");
							callback.put("callback", "true");
							send(callback.toJSONString());
						} else {
							callback.put("callback", "false");
							send(callback.toJSONString());
						}
					}
				} catch (NullPointerException e) {
					callback.put("callback", "false");
					send(callback.toJSONString());
					e.printStackTrace();
					logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
				} catch (Exception e) {
					callback.put("callback", "false");
					e.printStackTrace();
					logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
				}
			}
		} catch (ParseException ex) {
			ex.printStackTrace();
			logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
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
		
		connected = false;
	}

	/**
	 * onError WebSocket.
	 */
	@Override
	public void onError(Exception ex) {
		logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
	}

	/**
	 * onSendProtocol FeigReader
	 */
	@Override
	public void onSendProtocol(FedmIscReader reader, String string) {
	}

	/**
	 * onReceiveProtocol FeigReader
	 */
	@Override
	public void onReceiveProtocol(FedmIscReader reader, String string) {

	}

	/**
	 * onSendProtocol FeigReader
	 */
	@Override
	public void onSendProtocol(FedmIscReader reader, byte[] bytes) {
	}

	/**
	 * onReceiveProtocol FeigReader
	 */
	@Override
	public void onReceiveProtocol(FedmIscReader reader, byte[] bytes) {
	}

	/**
	 * Tag has been detected (TagListenerInterface).
	 * 
	 * Emit event to server.
	 */
	@Override
	public void tagDetected(BibTag bibTag) {
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
}