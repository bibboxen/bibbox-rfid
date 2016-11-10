package rfid;

import java.util.ArrayList;

public class WebSocketMessage {
	private ArrayList<BibTag> tags;
	private BibTag tag;
	private Boolean success;
	private String event;
	private String message;
	
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
	public Boolean getSuccess() {
		return success;
	}
	public void setSuccess(Boolean success) {
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
}
