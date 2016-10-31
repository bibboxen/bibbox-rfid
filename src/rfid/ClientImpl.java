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
 * Handles communication with FEIG reader and WebSocket.
 * 
 * @TODO: Replace org.json.simple.JSON* with GSON: https://github.com/google/gson to avoid type warnings.
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
	 * @TODO: Move FEIG implementation into separate class.
	 * 
	 * @param serverUri
	 * @param draft
	 * @param logger
	 */
	public ClientImpl(URI serverUri, Draft draft, LoggerImpl logger) {
		// Call constructor of WebSocketClient.
		super(serverUri, draft);

		this.logger = logger;
                
                
		// Initialize FEIG Reader.
		if (!initiateFeigReader()) {
			// @TODO: Emit error.
			logger.log("FEIG Reader: Error - CANNOT INITIALIZE");
                        tagReader.setRunning(false);
		} else {
                    // Open USBPort
                    if (openUSBPort()) {
                            // @TODO: Emit.
                            logger.log("USB Connection: ESTABLISHED");
                            
                            // Start TagReader.
                            tagReader = new TagReader(this, fedm, logger);
                            tagReader.setRunning(true);
                            tagReader.start();
                    } 
                    else {
                            // @TODO: Emit error.
                            tagReader.setRunning(false);
                            logger.log("USB Connection: Error - NO USB CONNECTION");
                    }
                }
		
		
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
		// The reader will therefore work best when 50 tags on reader.
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
                        tagReader.setRunning(false);
                        System.out.println("FEIGReader not connected to usb");
		} catch (FePortDriverException ex) {
			logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
			tagReader.setRunning(false);
                        System.out.println("FEIGReader not connected to usb: FePortDriverException");
		} catch (FeReaderDriverException ex) {
			logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
			tagReader.setRunning(false);
                        System.out.println("FEIGReader not connected to usb: FeReaderDriverException");
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
		JSONObject callback = new JSONObject();

		logger.log("WebSocket: message RECEIVED: " + message);

		try {
			JSONObject jsonMessage = (JSONObject) parser.parse(message);

			// detectTags Event
			if (jsonMessage.get("event").equals("detectTags")) {
				tagReader.setState("detectTags");

			} 
			// setTag Event
			else if (jsonMessage.get("event").equals("setTag")) {
				String mid = jsonMessage.get("mid").toString();
				String afi = jsonMessage.get("afi").toString();

				callback.put("event", "setTagResult");
				
				if (mid != null && !mid.equals("") && afi != null && !afi.equals("")) {
					tagReader.setMID(mid);
					tagReader.setAFI(afi);
					tagReader.setState("tagSet");
					callback.put("success", true);
				}
				else {
					callback.put("MID", mid);
					callback.put("AFI", afi);
					callback.put("success", false);
					callback.put("message", "You need to insert a MID and an AFI");
				}				
			} 
			// setAFI Event
			else if (jsonMessage.get("event").equals("setAFI")) {
				callback.put("event", "setAFIResult");
				
				try {
					String afi = jsonMessage.get("afi").toString();
					String uid = jsonMessage.get("uid").toString();

					callback.put("UID", uid);
					callback.put("AFI", afi);
					
					if (!uid.equals("") && uid != null) {
						if (!afi.equals("") && afi != null) {
							tagReader.setUidToWriteAfiTo(uid);
							tagReader.setAFI(afi);
							tagReader.setState("tagSetAFIOnUID");
							callback.put("success", true);
						}
					} else {
						if (!afi.equals("") && afi != null) {
							tagReader.setAFI(afi);
							tagReader.setState("tagSetAFI");
							callback.put("success", true);
						} else {
							callback.put("success", false);
							callback.put("message", "You need to insert an AFI");
						}
					}
				} catch (Exception e) {
					callback.put("success", false);
					callback.put("message", e.getMessage());
			
					e.printStackTrace();
					logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
				}
			}
			
			send(callback.toJSONString());
		} catch (ParseException ex) {
			ex.printStackTrace();
			logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
			
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
		
		connected = false;
                tagReader.setRunning(false);
	}

	/**
	 * onError WebSocket.
	 */
	@Override
	public void onError(Exception ex) {
		logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
                connected = false;
                tagReader.setRunning(false);
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
        
        public boolean isConnected(){
            return this.connected;
        }
}