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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Client.
 * 
 * Handles communication with reader and websocket.
 */
public class ClientImpl extends WebSocketClient implements TagListenerInterface, FeIscListener {
	private FedmIscReader fedm;
	private TagReader tagReader;
	private ArrayList<BibTag> bibTags;
	private LoggerImpl logger;

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
			logger.log("************Cannot initate FEIG Reader************");
		}
		
		// Open USBPort
		if (openUSBPort()) {
			logger.log("************USB CONNECTION ESTABLISHED************");
		} 
		else {
			logger.log("************NO USB CONNECTION************");
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
		// As of now it has been set to 10
		// which means the reader can read an inventory of max 10.
		// This can be set to for instance 100.
		try {
			fedm.setTableSize(FedmIscReaderConst.ISO_TABLE, 10);
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
		logger.log("************WebSocket connection opened************");
		JSONObject json = new JSONObject();
		json.put("event", "client_connected");
		send(json.toJSONString());
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
		logger.log("************WebSocket onMessage, message received: " + message + "************");
		try {
			JSONParser parser = new JSONParser();
			JSONObject jsonMessage = (JSONObject) parser.parse(message);
			JSONObject callback = new JSONObject();

			if (jsonMessage.get("event").equals("tagDetected")) {
				tagReader.setEvent("tagDetected");

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
						tagReader.setEvent("tagSet");
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
							tagReader.setEvent("tagSetAFIOnUID");
							callback.put("callback", "true");
							send(callback.toJSONString());
						}
					} else {
						if (!afi.equals("") && afi != null && afi != "") {
							tagReader.setAFI(afi);
							tagReader.setEvent("tagSetAFI");
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
		logger.log("************WebSocket connection closed************");
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
		JSONObject json = new JSONObject();
		json.put("event", "tagDetected");
		json.put("UID", bibTag.getUID());
		json.put("MID", bibTag.getMID());
		send(json.toJSONString());
	}

	/**
	 * Tag has been removed (TagListenerInterface).
	 * 
	 * Emit event to server.
	 */
	@Override
	public void tagRemoved(BibTag bibTag) {
		JSONObject json = new JSONObject();
		json.put("event", "tagRemoved");
		json.put("UID", bibTag.getUID());
		json.put("MID", bibTag.getMID());
		send(json.toJSONString());
	}
}