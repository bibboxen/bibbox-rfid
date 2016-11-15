package middleware;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocketImpl extends WebSocketClient  {
	private LoggerImpl logger;
	private boolean connected = false;
	private WebSocketListener listener;
	
	public WebSocketImpl(URI serverUri, Draft draft, WebSocketListener listener, LoggerImpl logger) {
		super(serverUri, draft);
		
		this.logger = logger;
		this.listener = listener;
	}

	/**
	 * onOpen WebSocket.
	 */
	@Override
	public void onOpen(ServerHandshake sh) {
		// WebSocket client connected to server
		logger.log("WebSocket: connection OPEN");

		connected = true;
	}

	/**
	 * onMessage WebSocket.
	 * 
	 * This method is called whenever the WebSocket client receives a message.
	 */
	@Override
	public void onMessage(String message) {
		listener.webSocketMessage(message);
	}
	
	/**
	 * Is the Web Socket connected.
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * onClose WebSocket.
	 * 
	 * @TODO: Reconnect.
	 */
	@Override
	public void onClose(int i, String string, boolean bln) {
		logger.log("WebSocket: connection CLOSED");
		
		connected = false;
	}

	/**
	 * onError WebSocket.
	 */
	@Override
	public void onError(Exception ex) {
		logger.log("Error message: " + ex.getMessage() + "\n" + ex.toString());
		
		connected = false;
	}
}
