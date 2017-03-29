package middleware;

import java.util.ArrayList;

public class WebSocketMessage {
	private ArrayList<BibTag> tags;
	private BibTag tag;
	private boolean success;
	private String event;
	private String message;
	private long timestamp; 
	
	public WebSocketMessage() {
		this.timestamp = System.currentTimeMillis();
	}

	public BibTag getTag() {
		return tag;
	}

	public void setTag(BibTag tag) {
		this.tag = tag;
	}

	public ArrayList<BibTag> getTags() {
		return tags;
	}

	public void setTags(ArrayList<BibTag> tags) {
		this.tags = tags;
	}

	public boolean getSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
