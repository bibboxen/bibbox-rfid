package rfid;

import java.util.ArrayList;

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
	
	/**
	 * Lists tags currently on device.
	 * 
	 * @param bibTags
	 */
	public void tagsDetected(ArrayList<BibTag> bibTags);
	
	/**
	 * A tag's AFI has been set.
	 * 
	 * @param bibTag
	 */
	public void tagAFISetSuccess(BibTag bibTag);
	
	/**
	 * An error occurred when writing tag's AFI.
	 * 
	 * @param bibTag
	 */
	public void tagAFISetFailure(BibTag bibTag);
}
