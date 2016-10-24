package rfid;

/*
    An interface that describes the reader.
*/

public interface TagRead {
	public void tagChanged();

	public void lastError(String error);
}
