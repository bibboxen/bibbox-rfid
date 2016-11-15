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
	protected boolean debug;

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
			if (debug) {
				System.out.println("Starting reader thread");
			}

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
				
				// Compare current tags with tags detected.
				// Emit events if changes.
				for (int i = 0; i < bibTags.size(); i++) {
					boolean contains = false;
					for (int j = 0; j < currentTags.size(); j++) {
						if (bibTags.get(i).getMID().equals(currentTags.get(j).getMID()) && 
							bibTags.get(i).getUID().equals(currentTags.get(j).getUID())) {
							contains = true;
							break;
						}
					}
					if (!contains) {
						tagListener.tagDetected(bibTags.get(i));
					}
				}
				for (int i = 0; i < currentTags.size(); i++) {
					boolean contains = false;
					for (int j = 0; j < bibTags.size(); j++) {
						if (currentTags.get(i).getMID().equals(bibTags.get(j).getMID()) &&
							currentTags.get(i).getUID().equals(bibTags.get(j).getUID())) {
							contains = true;
							break;
						}
					}
					if (!contains) {
						tagListener.tagRemoved(currentTags.get(i));
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
						// DEBUG
						if (debug) {
							System.out.println("Writing: " + event.toString());
						}

						if (writeAFI(tag.getUID(), event.getAfi())) {
							tag.setAFI(event.getAfi());
							
							tagListener.tagAFISet(tag, true);
						}
						else {
							tagListener.tagAFISet(tag, false);
						}
					} 
					else {
						logger.log("UID: " + event.getUid() + ", could not be found on reader");

						tagListener.tagAFISet(tag, false);
					}
				}
				
				// If requested current tags.
				if (detectCurrentTags) {
					tagListener.tagsDetected(currentTags);
					detectCurrentTags = false;
				}
				
				// DEBUG
				if (debug) {
					System.out.println(currentTags.toString());
				}

				// Yield. 
				Thread.sleep(50);
			} catch (Exception e) {
				e.printStackTrace();
				logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
			}
		}
	}
}
