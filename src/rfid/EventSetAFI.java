package rfid;

public class EventSetAFI {
	private String uid;
	private String afi;
	
	public EventSetAFI(String uid, String afi) {
		super();
		this.uid = uid;
		this.afi = afi;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getAfi() {
		return afi;
	}

	public void setAfi(String afi) {
		this.afi = afi;
	}
	
	public String toString() {
		return "{uid:" + uid + ", afi:" + afi + "}";
	}
}
