package rfid;

/**
 * BibTag.
 */
public class BibTag {
	private String uid, mid;
	private String afi;

	/**
	 * Constructor.
	 * 
	 * @param uid
	 * @param mid
	 */
	public BibTag(String uid, String mid) {
		this.uid = uid;
		this.mid = mid;
	}

	public String getUID() {
		return this.uid;
	}

	public String getMID() {
		return this.mid;
	}

	public void setMID(String mid) {
		this.mid = mid;
	}

	public String getAFI() {
		return this.afi;
	}

	public void setAFI(String afi) {
		this.afi = afi;
	}

	/**
	 * To string.
	 */
	public String toString() {
		return "{ uid: " + this.uid + ", mid: " + this.mid + " }";
	}
}
