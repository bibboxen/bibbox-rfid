package middleware;

import java.util.ArrayList;

/**
 * AbstractTagReader.
 * 
 * Supplies basic logic for a TagReader, with a Thread run-method that reads/writes tags according
 * to which state it is in.
 * 
 * This class should be extended for a new TagReader, and at least the abstract methods
 * need to be overridden.
 */
public abstract class AbstractTagReader extends Thread implements TagReaderInterface {
	protected boolean connected = false;
	protected boolean running = false;
	protected boolean detectCurrentTags = false;
	protected ArrayList<BibTag> bibTags = new ArrayList<BibTag>();
	protected ArrayList<BibTag> currentTags = new ArrayList<BibTag>();
	protected ArrayList<EventSetAFI> eventsSetAFI = new ArrayList<EventSetAFI>();
	protected LoggerImpl logger;
	protected TagListenerInterface tagListener;
	protected int successfulReadsThreshold = 2;
	protected int threadSleepInMillis = 10;

	/**
	 * Get the tags on the device.
	 * 
	 * @return
	 *   ArrayList of BibTags.
	 */
	protected abstract ArrayList<BibTag> getTags();
	
	/**
	 * Connect to the device.
	 * 
	 * @return
	 */
	public abstract boolean connect();
	
	/**
	 * Connect to the device.
	 * 
	 * @return
	 */
	public abstract boolean closeConnection();
	
	/**
	 * Connect to the device.
	 * 
	 * @return
	 */
	public abstract boolean writeAFI(String uid, String afi);

	/**
	 * Is the reader running?
	 */
	@Override
	public boolean isRunning() {
		return running;
	}
	
	/**
	 * Start reading.
	 */
	@Override
	public void startReading() {
		if (!connected) {
			connect();
		}
		
		if (connected && !running) {
			logger.info("Starting reader thread");

			running = true;
			start();
		}
	}

	/**
	 * Stop reading.
	 */
	@Override
	public void stopReading() {
		running = false;
	}

	/**
	 * Add event - set tag AFI.
	 */
	@Override
	public void addEventSetTagAFI(String uid, String afi) {
		eventsSetAFI.add(new EventSetAFI(uid, afi));
	}

	/**
	 * The reader should detect current tags.
	 */
	@Override
	public void detectCurrentTags() {
		detectCurrentTags = true;
	}
	
	/**
	 * Start the thread.
	 */
	public void run() {
		while (running) {
			try {
				bibTags = getTags();
				
				// Count up number of times detected.
				// When detected 3 times in a row send tag detected event.
				for (BibTag bibTag : bibTags) {
					for (BibTag currentTag : currentTags) {
						if (currentTag.getMID().equals(bibTag.getMID()) && 
							currentTag.getUID().equals(bibTag.getUID())) {
							// Avoid overflow. With overflow maintain old max.
							bibTag.setSuccessfulReads(Math.max(currentTag.getSuccessfulReads(), currentTag.getSuccessfulReads() + 1));
						}
					}
				}
				
				// Compare current tags with tags detected.
				// Emit events if changes.
				for (BibTag bibTag : bibTags) {
					for (BibTag currentTag : currentTags) {
						if (bibTag.getMID().equals(currentTag.getMID()) && 
							bibTag.getUID().equals(currentTag.getUID()) &&
							bibTag.getSuccessfulReads() == successfulReadsThreshold) {
							tagListener.tagDetected(bibTag);
							break;
						}
					}
				}
				for (BibTag currentTag : currentTags) {
					boolean contains = false;
					for (BibTag bibTag : bibTags) {
						if (currentTag.getMID().equals(bibTag.getMID()) &&
							currentTag.getUID().equals(bibTag.getUID())) {
							contains = true;
							break;
						}
					}
					if (!contains) {
						tagListener.tagRemoved(currentTag);
					}
				}
				
				// Updated current tags.
				currentTags = new ArrayList<BibTag>(bibTags);

				// Process EventSetAFI events.
				ArrayList<EventSetAFI> events = new ArrayList<EventSetAFI>(eventsSetAFI);
				eventsSetAFI.clear();
				
				for (EventSetAFI event : events) {
					BibTag tag = null;
					
					// Get tag.
					for (BibTag b : bibTags) {
						if (b.getUID().equals(event.getUid())) {
							tag = b;
							
							break;
						}
					}
					
					if (tag != null) {
						logger.info("Writing: " + event.toString());

						if (writeAFI(tag.getUID(), event.getAfi())) {
							tag.setAFI(event.getAfi());
							
							tagListener.tagAFISet(tag, true);
						}
						else {
							tagListener.tagAFISet(tag, false);
						}
					} 
					else {
						logger.warning("UID: " + event.getUid() + ", could not be found on reader");

						tagListener.tagAFISet(tag, false);
					}
				}
				
				// If requested current tags.
				if (detectCurrentTags) {
					// Find tags that have been read the proper number of times.
					ArrayList<BibTag> tagsDetected = new ArrayList<BibTag>();
					for (BibTag currentTag : currentTags) {
						if (currentTag.getSuccessfulReads() >= successfulReadsThreshold) {
							tagsDetected.add(currentTag);
						}
					}
				
					tagListener.tagsDetected(tagsDetected);
					detectCurrentTags = false;
				}
				
				logger.info(currentTags.toString());

				// Yield. 
				Thread.sleep(threadSleepInMillis);
			} catch (Exception e) {
				logger.error("Error message: " + e.getMessage() + "\n" + e.getStackTrace());
			}
		}
	}
}
