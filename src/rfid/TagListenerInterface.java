package rfid;

/*
    An interface that describes the reader.
*/

public interface TagListenerInterface {
	public void tagChanged();

	public void tagDetected(BibTag bibTag);

	public void tagRemoved(BibTag bibTag);

	public void lastError(String error);
}
