package middleware;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import de.feig.FePortDriverException;
import de.feig.FeReaderDriverException;
import de.feig.FedmException;

/**
 * AbstractTagReader.
 * 
 * Supplies basic logic for a TagReader, with a Thread run-method that
 * reads/writes tags according to which state it is in.
 * 
 * This class should be extended for a new TagReader, and at least the abstract
 * methods need to be overridden.
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
	 * @throws FedmException 
	 * @throws FeReaderDriverException 
	 * @throws FePortDriverException 
	 */
	protected abstract HashMap<String, BibTag> getTags() throws FedmException, FePortDriverException, FeReaderDriverException;

	/**
	 * Connect to the device.
	 * 
	 * @return
	 *   On success returns true, otherwise false.
	 */
	public abstract boolean connect();

	/**
	 * Connect to the device.
	 * 
	 * @return
	 *   On success returns true, otherwise false.
	 */
	public abstract boolean closeConnection();

	/**
	 * Write the AFI.
	 * 
	 * @return
	 *   On success returns true, otherwise false.
	 */
	public abstract boolean writeAFI(String uid, String afi);

	/**
	 * Read the AFI.
	 * 
	 * @param uid
	 *   Unique Id of tag.
	 * @return
	 *   The AFI value.
	 */
	public abstract int readAFI(String uid);

	/**
	 * Clear already read data.
	 * 
	 * @return
	 *   On success returns true, otherwise false.
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
	 * @param data
	 *   Raw data from the reader.
	 * @return
	 *   String of data in reverse order.
	 */
	private String reverseData(String data) {
		return data.substring(6, 8) + data.substring(4, 6) + data.substring(2, 4) + data.substring(0, 2)
				+ data.substring(14, 16) + data.substring(12, 14) + data.substring(10, 12) + data.substring(8, 10)
				+ data.substring(22, 24) + data.substring(20, 22) + data.substring(18, 20) + data.substring(16, 18)
				+ data.substring(30, 32) + data.substring(28, 30) + data.substring(26, 28) + data.substring(24, 26)
				+ data.substring(38, 40) + data.substring(36, 38) + data.substring(34, 36) + data.substring(32, 34)
				+ data.substring(46, 48) + data.substring(44, 46) + data.substring(42, 44) + data.substring(40, 42)
				+ data.substring(54, 56) + data.substring(52, 54) + data.substring(50, 52) + data.substring(48, 50)
				+ data.substring(62, 64) + data.substring(60, 62) + data.substring(58, 60) + data.substring(56, 58);
	}

	/**
	 * Decode a utf-8 hex encoded string.
	 * 
	 * @see https://en.wikipedia.org/wiki/UTF-8
	 * 
	 * @param str
	 *   The string to decode.
	 * @param breakAtNull
	 *   Should the decoding end when encountering the null character?
	 * @return
	 *   The decoded string. 
	 * @throws UnsupportedEncodingException
	 */
	private String utf8decode(String str, boolean breakAtNull) throws UnsupportedEncodingException {
		ByteArrayOutputStream bOutput = new ByteArrayOutputStream(12);

		for (int i = 0; i < str.length(); i += 2) {
			int b = Integer.parseInt(str.substring(i, i + 2), 16);

			// Break at first null character.
			if (breakAtNull && b == 0) {
				break;
			}

			bOutput.write(b);
		}

		return new String(bOutput.toByteArray(), "UTF-8");
	}

	/**
	 * Decode a utf-8 hex encoded string and break at first null character.
	 *
	 * @param str
	 *   The string to decode.
	 * @return
	 *   The decoded string.
	 * @throws UnsupportedEncodingException
	 */
	private String utf8decode(String str) throws UnsupportedEncodingException {
		return utf8decode(str, true);
	}

	/**
	 * Calculate crc16ccitt.
	 * 
	 * @see http://introcs.cs.princeton.edu/java/61data/CRC16CCITT.java
	 * 
	 * @param data
	 *   Byte array of data.
	 * @return
	 *   The crc value.
	 */
	public static int crc16(byte[] data) {
		int crc = 0xFFFF; // initial value
		int polynomial = 0x1021; // 0001 0000 0010 0001 (0, 5, 12)

		for (byte b : data) {
			for (int i = 0; i < 8; i++) {
				boolean bit = ((b >> (7 - i) & 1) == 1);
				boolean c15 = ((crc >> 15 & 1) == 1);
				crc <<= 1;
				if (c15 ^ bit)
					crc ^= polynomial;
			}
		}

		crc &= 0xffff;
		return crc;
	}

	/**
	 * Check crc.
	 *
	 * @return
	 *   If the the CRCs match returns true, otherwise false.
	 */
	private boolean crc(String crc, String data) {
		data = data.substring(0, 38) + data.substring(42, 64) + "0000";
		byte[] buffer = DatatypeConverter.parseHexBinary(data);

		String calculatedCrc = Integer.toHexString(crc16(buffer));

		// Convert to match output from crc16, without leading zeros.
		crc = (crc.substring(2, 4) + crc.substring(0, 2)).replaceFirst("^0+(?!$)", "");

		return crc.equals(calculatedCrc);
	}

	/**
	 * Check if owner institution is valid.
	 *
	 * @param ownerInstitution
	 *   The owner institution string to validate.
	 * @return
	 *   Validity of value.
	 */
	private boolean checkOwnerInstitution(String ownerInstitution) {
		return ownerInstitution.matches("^[A-Z]{2}[0-9]{5}$");
	}

	/**
	 * Process each tag.
	 *
	 * Makes sure it complies with library standards.
	 *
	 * See ISO 28560-3:2014 "Example 1, encoding of truncated basic block"
	 *
	 * Tag layout (32 bytes)
	 * ---------------------
	 * 4 bits: Content parameter (should be 1)
	 * 4 bits: Type of usage (should be 1)
	 * 2 bytes: Set information: x of y
	 * 16 bytes: Primary item identifier
	 * 2 bytes: CRC
	 * 11 bytes: Owner institution
	 * ---------------------
	 *
	 * @TODO: Fix this to completely follow ISO 28560-1, ISO 28560-3.
	 * @TODO: Handle tags where primary item identifier is saved in the library extension block.
	 *
	 * @param tags
	 *   HashMap of tags.
	 */
	private void processTags(HashMap<String, BibTag> tags) {
		Iterator<Map.Entry<String, BibTag>> iterator = tags.entrySet().iterator();
		Map.Entry<String, BibTag> entry;
		BibTag tag;
		String data;
		String mid;
		String primaryItemIdentifierBlock;
		String ownerInstitution;

		while (iterator.hasNext()) {
			entry = iterator.next();

			tag = entry.getValue();

			data = tag.getData();

			// Make sure the tag has the correct order.
			// @TODO: Can we get this info from somewhere else?
			if (data.substring(6, 8).equals("11")) {
				logger.info("Ensuring tag order, reversing data in: " + data);

				data = reverseData(data);
			}

			// We only accept tags that start with "11" (see Tag layout)
			// and have a data length of 64.
			if (!data.substring(0, 2).equals("11") || data.length() != 64) {
				// Then we do not recognize the tag.
				logger.warning("Tag does start with 11 or length of 64: " + data);

				iterator.remove();
				continue;
			}

			// Validate tag.
			if (!crc(data.substring(38, 42).toLowerCase(), data)) {
				// Then we do not recognize the tag.
				logger.warning("Could not validate tag: " + data);

				// This is a HACK to handle tags that do not follow standards and do not have valid CRC's.
				// If it follows standards and does not have a valid CRC, ignore the tag.
				try {
					ownerInstitution = utf8decode(data.substring(42, 64), false);
					if (checkOwnerInstitution(ownerInstitution)) {
						// If the owner institution is valid and the CRC is invalid, the tag should be ignored.
						iterator.remove();
						continue;
					}
					else {
						// In this case we accept the tag even though the CRC is invalid, to allow accepting
						// tags that do not follow conventions.
						logger.warning(
							"Owner institution does not follow conventions: " +
							ownerInstitution +
							". Accepting tag even though validation fails."
						);
					}
				} catch (UnsupportedEncodingException e) {
					iterator.remove();
					continue;
				}
			}

			try {
				// Extract mid from primary item identifier block.
				primaryItemIdentifierBlock = data.substring(6, 38);

				mid = utf8decode(primaryItemIdentifierBlock);

				// Update tag with data.
				tag.setMID(mid);
			} catch (UnsupportedEncodingException e) {
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
		// Bookkeeping variables.
		BibTag tag;
		String uid;
		String chipAfi;

		while (running) {
			try {
				newTags = getTags();
				processTags(newTags);

				// Notify that new tags have been detected.
				if (currentTags.size() == 0 && newTags.size() > 0) {
					tagListener.processingNewTags();
				}

				// Compare current and new tags for tags removed.
				for (Map.Entry<String, BibTag> entry : currentTags.entrySet()) {
					uid = entry.getKey();
					tag = entry.getValue();

					if (!newTags.containsKey(uid)) {
						tagListener.tagRemoved(tag);
					}
				}

				// Compare current and new tags for tags detected.
				for (Map.Entry<String, BibTag> entry : newTags.entrySet()) {
					uid = entry.getKey();
					tag = entry.getValue();

					if (!currentTags.containsKey(uid)) {
						tagListener.tagDetected(tag);
					}
				}

				// Update current tags, with tags detected.
				currentTags = new HashMap<String, BibTag>(newTags);

				// Process EventSetAFI events.
				ArrayList<EventSetAFI> events = new ArrayList<EventSetAFI>(eventsSetAFI);
				eventsSetAFI.clear();

				// Set AFI values.
				for (EventSetAFI event : events) {
					tag = currentTags.get(event.getUid());

					// If tag exists, set the AFI.
					if (tag != null) {
						logger.info("Writing: " + event.toString());

						if (!writeAFI(tag.getUID(), event.getAfi())) {
							tagListener.tagAFISet(tag, false);
							events.remove(event);
						}
					} else {
						logger.warning("UID: " + event.getUid() + ", could not be found on reader");

						tagListener.tagAFISet(new BibTag(event.getUid(), "", event.getAfi()), false);
						events.remove(event);
					}
				}

				// If we have some completed write AFIs, we confirm the values
				// written.
				if (events.size() > 0) {
					// Clear reader from old data.
					clearReader();

					// Check AFI values written and report back result.
					for (EventSetAFI event2 : events) {
						chipAfi = Integer.toString(readAFI(event2.getUid()));
						tag = currentTags.get(event2.getUid());

						if (event2.getAfi().equals(chipAfi)) {
							tag.setAFI(chipAfi);
							tagListener.tagAFISet(tag, true);
						} else {
							tagListener.tagAFISet(tag, false);
						}
					}
				}

				// If requested current tags.
				if (detectCurrentTags) {
					// Find tags that have been read the proper number of times.
					ArrayList<BibTag> tagsDetected = new ArrayList<BibTag>();
					for (BibTag detectedTag : currentTags.values()) {
						tagsDetected.add(detectedTag);
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
