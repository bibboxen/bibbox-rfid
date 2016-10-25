package rfid;

/**
 * TagListenerInterface.
 */
public interface TagListenerInterface {
	/**
	 * A tag has been detected.
	 * 
	 * @param bibTag
	 */
	public void tagDetected(BibTag bibTag);

	/**
	 * A tag has been removed.
	 * 
	 * @param bibTag
	 */
	public void tagRemoved(BibTag bibTag);
}
