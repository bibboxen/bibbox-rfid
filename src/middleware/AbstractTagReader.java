package middleware;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	protected HashMap<String, BibTag> newTags = new HashMap<String, BibTag>();
	protected HashMap<String, BibTag> currentTags = new HashMap<String, BibTag>();
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
	protected abstract HashMap<String, BibTag> getTags();
	
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
	 * Write the AFI.
	 * 
	 * @return
	 */
	public abstract boolean writeAFI(String uid, String afi);

	/**
	 * Read the AFI.
	 * 
	 * @param uid
	 * @return
	 */
	public abstract int readAFI(String uid);
	
	/**
	 * Clear already read data.
	 * 
	 * @return
	 */
	public abstract boolean clearReader();
	
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
	 * @param data Raw data from the reader.
	 * 
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
	 * 
	 * @param s
	 * 
	 * @return
	 * 
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
	private void processTags(HashMap<String, BibTag> tags) {
		Iterator<Map.Entry<String, BibTag>> iterator = tags.entrySet().iterator();
		Map.Entry<String, BibTag> entry;
		BibTag tag;
		String data;
		String primaryItemIdentifierBlock;

		while (iterator.hasNext()) {
		    entry = iterator.next();
		    
		    tag = entry.getValue();

			data = tag.getData();

			// Make sure the tag has the correct order.
			// @TODO: Can we get this info from somewhere else?
			if (data.substring(6, 8).equals("11")) {
				data = reverseData(data);
			}

			// We only accept tags that start with 11
			if (!data.substring(0, 2).equals("11") || data.length() != 64) {
				// Then we do not recognize the tag.
				logger.warning("Could not create MID from data: " + data);

				iterator.remove();

				continue;
			}

			// @TODO: Validate tag.
			
			// Extract mid from primaryItemIdentifierBlock
			primaryItemIdentifierBlock = data.substring(6, 38);
			
			try {
				String mid = utf8decode(primaryItemIdentifierBlock);

				// Update tag with data.
				tag.setMID(mid);
			}
			catch (UnsupportedEncodingException e) {
				// Then we do not recognize the tag.
				logger.warning("Could not create MID from data: " + data);

				iterator.remove();

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
				newTags = getTags();
				processTags(newTags);

				// Bookkeeping variables.
				BibTag bibTag;
				BibTag currentTag;
				String uid;
				boolean contains;
				String afi;

				// Notify that new tags have been detected.
				if (currentTags.size() == 0 && newTags.size() > 0) {
					tagListener.processingNewTags();
				}

				// Compare current tags with tags detected.
				// Update successful reads counter for tags detected in last iteration.
				// Emit removed event, for tags that are no longer detected compared with last iteration.
				for (Map.Entry<String, BibTag> entry : currentTags.entrySet()) {
				    uid = entry.getKey();
				    currentTag = entry.getValue();

				    contains = false;

					if (newTags.containsKey(uid) && newTags.get(uid).getMID().equals(currentTag.getMID())) {
						contains = true;

						bibTag = newTags.get(uid);
						
						// Count up number of successful reads, until one larger than successfulReadsThreshold.
						bibTag.setSuccessfulReads(Math.min(currentTag.getSuccessfulReads() + 1, successfulReadsThreshold + 1));
						
						// If tag has been detected enough times, send tag detected.
						if (bibTag.getSuccessfulReads() == successfulReadsThreshold) {
							tagListener.tagDetected(bibTag);
						}
					}
					
					if (!contains) {
						tagListener.tagRemoved(currentTag);
					}
				}
				
				// Update current tags, with tags detected.
				currentTags = new HashMap<String, BibTag>(newTags);

				// Process EventSetAFI events.
				ArrayList<EventSetAFI> events = new ArrayList<EventSetAFI>(eventsSetAFI);
				eventsSetAFI.clear();
				
				// Set AFI values.
				for (EventSetAFI event : events) {
					currentTag = currentTags.get(event.getUid());
					
					// If tag exists, set the AFI.
					if (currentTag != null) {
						logger.info("Writing: " + event.toString());

						if (!writeAFI(currentTag.getUID(), event.getAfi())) {
							tagListener.tagAFISet(currentTag, false);
							events.remove(event);
						}
					}
					else {
						logger.warning("UID: " + event.getUid() + ", could not be found on reader");

						tagListener.tagAFISet(currentTag, false);
						events.remove(event);
					}
				}
				
				// If we have some completed write AFIs, we confirm the values written.
				if (events.size() > 0) {
					// Clear reader from old data.
					clearReader();
					
					// Check AFI values written and report back result.
					for (EventSetAFI event2 : events) {
						afi = Integer.toString(readAFI(event2.getUid()));
						currentTag = currentTags.get(event2.getUid());
						currentTag.setAFI(afi);

						if (event2.getAfi().equals(afi)) {
							tagListener.tagAFISet(currentTag, true);
						}
						else {
							tagListener.tagAFISet(currentTag, false);
						}
					}
				}
				
				// If requested current tags.
				if (detectCurrentTags) {
					// Find tags that have been read the proper number of times.
					ArrayList<BibTag> tagsDetected = new ArrayList<BibTag>();
					for (BibTag detectedTag : currentTags.values()) {
						if (detectedTag.getSuccessfulReads() >= successfulReadsThreshold) {
							tagsDetected.add(detectedTag);
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
		currentTags = new HashMap<String, BibTag>();
	}
}
