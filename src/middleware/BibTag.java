package middleware;

/**
 * BibTag.
 */
public class BibTag {
	private String uid, mid, afi, data;
	private int seriesLength, numberInSeries;
	private long timestamp; 

	/**
	 * Constructor.
	 * 
	 * @param uid
	 * @param mid
	 * @param afi
	 * @param seriesLength
	 * @param numberInSeries
	 */
	public BibTag(String uid, String data, String mid, String afi, int seriesLength, int numberInSeries) {
		super();
		this.uid = uid;
		this.mid = mid;
		this.afi = afi;
		this.data = data;
		this.seriesLength = seriesLength;
		this.numberInSeries = numberInSeries;
		this.timestamp = System.currentTimeMillis();
	}

	public BibTag(String uid, String data, String afi) {
		this.uid = uid;
		this.data = data;
		this.afi = afi;
		this.timestamp = System.currentTimeMillis();
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

	public int getSeriesLength() {
		return seriesLength;
	}

	public void setSeriesLength(int seriesLength) {
		this.seriesLength = seriesLength;
	}

	public int getNumberInSeries() {
		return numberInSeries;
	}

	public void setNumberInSeries(int numberInSeries) {
		this.numberInSeries = numberInSeries;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * To string.
	 */
	public String toString() {
		return "{ uid: " + uid + ", mid: " + mid + " ( " + numberInSeries  + "/" + seriesLength + ") }";
	}
}
