package middleware;

/**
 * BibTag.
 */
public class BibTag {
	private String uid, mid, afi;
	private int seriesLength, numberInSeries, successfulReads;

	/**
	 * Constructor.
	 * 
	 * @param uid
	 * @param mid
	 * @param afi
	 * @param seriesLength
	 * @param numberInSeries
	 */
	public BibTag(String uid, String mid, String afi, int seriesLength, int numberInSeries) {
		super();
		this.uid = uid;
		this.mid = mid;
		this.afi = afi;
		this.seriesLength = seriesLength;
		this.numberInSeries = numberInSeries;
		this.successfulReads = 0;
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

	public int getSuccessfulReads() {
		return successfulReads;
	}

	public void setSuccessfulReads(int successfulReads) {
		this.successfulReads = successfulReads;
	}

	/**
	 * To string.
	 */
	public String toString() {
		return "{ uid: " + uid + ", mid: " + mid + " ( " + numberInSeries  + "/" + seriesLength + "), successfulReads: " + successfulReads + " }";
	}
}
