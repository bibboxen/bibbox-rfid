package middleware;

/**
 * TagReaderInterface.
 * 
 * Describes the interface for a tag-reader.
 */
public interface TagReaderInterface {
	public void startReading();
	
	public void stopReading();
	
	public boolean connect();
	
	public boolean closeConnection();
	
	public boolean isRunning();
	
	public void addEventSetTagAFI(String uid, String afi);
	
	public void detectCurrentTags();
}
