package rfid;

/**
 * BibTag.
 */
public class BibTag {
	private String UID, MID;
	private String AFI;

	/**
	 * Constructor.
	 * 
	 * @param UID
	 * @param MID
	 */
	public BibTag(String UID, String MID) {
		this.UID = UID;
		this.MID = MID;
	}

	public String getUID() {
		return this.UID;
	}

	public String getMID() {
		return this.MID;
	}

	public void setMID(String MID) {
		this.MID = MID;
	}

	public String getAFI() {
		return this.AFI;
	}

	public void setAFI(String AFI) {
		this.AFI = AFI;
	}

	/**
	 * To string.
	 */
	public String toString() {
		return "{ UID: " + this.UID + " MID: " + this.MID + " }";
	}
}
