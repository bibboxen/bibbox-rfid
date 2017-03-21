package middleware;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
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
	protected int successfulReadsThreshold;
	protected int threadSleepInMillis;

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
	 * Reverse the order of the bytes in the data.
	 * 
	 * @TODO: Optimize process.
	 * 
	 * @param b
	 * @return
	 */
	private String reverseData(String data) {
		return 
				data.substring(6, 8) + data.substring(4, 6) + data.substring(2, 4) + data.substring(0, 2) +
				data.substring(14, 16) + data.substring(12, 14) + data.substring(10, 12) + data.substring(8, 10) +
				data.substring(22, 24) + data.substring(20, 22) + data.substring(18, 20) + data.substring(16, 18) +
				data.substring(30, 32) + data.substring(28, 30) + data.substring(26, 28) + data.substring(24, 26) +
				data.substring(38, 40) + data.substring(36, 38) + data.substring(34, 36) + data.substring(32, 34) +
				data.substring(46, 48) + data.substring(44, 46) + data.substring(42, 44) + data.substring(40, 42) +
				data.substring(54, 56) + data.substring(52, 54) + data.substring(50, 52) + data.substring(48, 50) +
				data.substring(62, 64) + data.substring(60, 62) + data.substring(58, 60) + data.substring(56, 58);
	}
	
	/**
	 * Decode a utf-8 hex encoded string.
	 * @param s
	 * @return
	 * @throws UnsupportedEncodingException 
	 * 
	 * @see https://en.wikipedia.org/wiki/UTF-8
	 */
	private String utf8decode(String s) throws UnsupportedEncodingException {
		ByteArrayOutputStream bOutput = new ByteArrayOutputStream(12);
		
		for (int i = 0; i < s.length(); i+=2) {
			int b = Integer.parseInt(s.substring(i, i+2), 16);
 			
			// Break at first null character.
			if (b == 0) {
				break;
			}
			
			bOutput.write(b);
		}
		
		return new String(bOutput.toByteArray(), "UTF-8");
	}
	
	/**
	 * Read each tag.
	 * 
	 * Make sure it complies with library standards.
	 * 
	 * @TODO: Fix this to correctly follow ISO 28560-1, ISO 28560-3.
	 * 
	 * @param tags
	 */
	private void processTags(ArrayList<BibTag> tags) {
		for (BibTag tag : tags) {
			String data = tag.getData();

			// Make sure the tag has the correct order.
			// @TODO: Can we get this info from somewhere else?
			if (data.substring(6, 8).equals("11")) {
				data = reverseData(data);
			}

			// We only accept tags that start with 11
			if (!data.substring(0, 2).equals("11") || data.length() != 64) {
				// Then we do not recognize the tag.
				logger.warning("Could not create MID from data: " + data);

				tags.remove(tag);

				continue;
			}

			// @TODO: Validate tag.
			
			// Extract mid from primaryItemIdentifierBlock
			String primaryItemIdentifierBlock = data.substring(6, 38);
			
			try {
				String mid = utf8decode(primaryItemIdentifierBlock);

				// Update tag with data.
				tag.setMID(mid);
			}
			catch (UnsupportedEncodingException e) {
				// Then we do not recognize the tag.
				logger.warning("Could not create MID from data: " + data);

				tags.remove(tag);

				continue;
			}
			
			tag.setSeriesLength(Integer.parseInt(data.substring(2, 4)));
			tag.setNumberInSeries(Integer.parseInt(data.substring(4, 6)));
		}
	}
	
	/**
	 * Start the thread.
	 */
	public void run() {
		while (running) {
			try {
				bibTags = getTags();
				processTags(bibTags);
				
				// Notify that new tags have been detected.
				if (currentTags.size() == 0 && bibTags.size() > 0) {
					tagListener.processingNewTags();
				}

				// Compare current tags with tags detected.
				// Update successful reads counter for tags detected in last iteration.
				// Emit removed event, for tags that are no longer detected compared with last iteration.
				for (BibTag currentTag : currentTags) {
					boolean contains = false;
					
					for (BibTag bibTag : bibTags) {
						// If tag is the same, update successful reads counter.
						if (bibTag.getMID().equals(currentTag.getMID()) && 
							bibTag.getUID().equals(currentTag.getUID())) {
						
							contains = true;
							
							// Count up number of successful reads, until one larger than successfulReadsThreshold.
							bibTag.setSuccessfulReads(Math.min(currentTag.getSuccessfulReads() + 1, successfulReadsThreshold + 1));
							
							// If tag has been detected enough times, send tag detected.
							if (bibTag.getSuccessfulReads() == successfulReadsThreshold) {
								tagListener.tagDetected(bibTag);
							}
							
							break;
						}
					}
					
					if (!contains) {
						tagListener.tagRemoved(currentTag);
					}
				}
				
				// Update current tags, with tags detected.
				currentTags = new ArrayList<BibTag>(bibTags);

				// Process EventSetAFI events.
				ArrayList<EventSetAFI> events = new ArrayList<EventSetAFI>(eventsSetAFI);
				eventsSetAFI.clear();
				
				for (EventSetAFI event : events) {
					BibTag tag = null;
					
					// Get tag.
					for (BibTag currentTag : currentTags) {
						if (currentTag.getUID().equals(event.getUid())) {
							tag = currentTag;
							
							break;
						}
					}
					
					// If tag exists, set the AFI.
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
				
				// Log current tags.
				logger.info(currentTags.toString());
			} catch (Exception e) {
				logger.error("Error message: " + e.getMessage() + "\n" + e.getStackTrace());
			}

			// Yield CPU.
			try {
				Thread.sleep(threadSleepInMillis);
			} catch (InterruptedException e) {
				logger.error("InterruptedException: " + e.getMessage() + "\n" + e.getStackTrace());
			}
		}
		
		// Make sure detected tags are cleared.
		currentTags = new ArrayList<BibTag>();
	}
}
