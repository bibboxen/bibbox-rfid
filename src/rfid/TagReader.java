package rfid;

import de.feig.FePortDriverException;
import de.feig.FeReaderDriverException;
import de.feig.FedmException;
import de.feig.FedmIscReader;
import de.feig.FedmIscReaderConst;
import de.feig.FedmIscReaderID;
import java.util.ArrayList;

/**
 * This is the reader class.
 * This class handles the following states: detectTags, tagSet, tagSetAFI.
 * This is also the class that can write to tags
*/
public class TagReader extends Thread {
	private FedmIscReader fedm = null;
	private boolean running = true;
	private TagListenerInterface tagListener;
	private ArrayList<BibTag> bibTags = new ArrayList<BibTag>();
	private String state = "";
	private String AFI = "07"; // standard that the book is in the house
	private String MID;
	private String uidToWriteTo;
	private LoggerImpl logger;
	private String callbackMessage = "";
	private ArrayList<BibTag> currentTags = new ArrayList<BibTag>();

	public TagReader(TagListenerInterface tagListener, FedmIscReader fedm, LoggerImpl logger) {
		this.fedm = fedm;
		this.tagListener = tagListener;
		this.logger = logger;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getMID() {
		return this.MID;
	}

	public String getMIDFromMultipleBlocks(String id) {
		// This method uses readSpecific block
		// to read four blocks which is enough
		// to get the MID on a bib tag.
		// Then the MID is created and returnen.
		String mid = "";
		String b0 = readSpecificBlock(id, (byte) 0);
		String b1 = readSpecificBlock(id, (byte) 1);
		String b2 = readSpecificBlock(id, (byte) 2);
		String b3 = readSpecificBlock(id, (byte) 3);
		mid = b0 + b1 + b2 + b3;
		String midSub = mid.substring(6, 26);
		String midReplaced = midSub.replaceAll(".(.)?", "$1");
		String s = mid.substring(0, 6) + midReplaced;

		return s;
	}

	public String readSpecificBlock(String id, byte dbAddress) {
		// This method is used when
		// reading a specific block (dbAddress)
		// This method is used in the getMIDFromMultipleBlocks method
		byte[] dataBlock = null;
		String dataBlockString = null;

		fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_UID, id);
		fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, (byte) 0x23);
		fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, (byte) 0x00);
		fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_ADR, (byte) 0x01);
		fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_DBN, (byte) 0x01);
		fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_DB_ADR, dbAddress);

		try {
			fedm.sendProtocol((byte) 0xB0);

			int idx = fedm.findTableIndex(0, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_SNR, id);
			if (idx < 0) {
				return "";
			}
			byte blockSize = fedm.getByteTableData(idx, FedmIscReaderConst.ISO_TABLE,
					FedmIscReaderConst.DATA_BLOCK_SIZE);

			dataBlockString = fedm.getStringTableData(idx, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_RxDB,
					dbAddress);

			return dataBlockString;
		} catch (FePortDriverException ex) {
			ex.printStackTrace();
			logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
		} catch (FeReaderDriverException ex) {
			ex.printStackTrace();
			logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
		} catch (FedmException ex) {
			ex.printStackTrace();
			logger.log("Error code: " + ex.getErrorcode() + "\n" + ex.toString());
		}
		return "";
	}

	public void writeSpecificBlock(String data, String id, byte dbAddress) {
		// This is method is used to write
		// any string data to a specific block
		// on a tag(dbAddress). This is used in the
		// writeMIDToMultipleBlocks method.

		byte[] dataBlock = null;
		dataBlock = getByteArrayFromString(data);

		try {
			int idx = fedm.findTableIndex(0, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_SNR, id);
			if (idx < 0) {
				return;
			}
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_UID, id);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, (byte) 0x24);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, (byte) 0x00);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_ADR, (byte) 0x01);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_DBN, (byte) 0x01);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_DB_ADR, dbAddress);

			fedm.setTableData(idx, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_BLOCK_SIZE, (byte) 0x04);
			fedm.setTableData(idx, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_TxDB, dbAddress, dataBlock);

			fedm.sendProtocol((byte) 0xB0);

		} catch (FedmException ex) {
			ex.printStackTrace();
			logger.log("Error code: " + ex.getErrorcode() + "\n" + ex.toString());
		} catch (FePortDriverException ex) {
			ex.printStackTrace();
			logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
		} catch (FeReaderDriverException ex) {
			ex.printStackTrace();
			logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
		}
	}

	public byte[] getByteArrayFromString(String large) {
		// this method can be used to retrieve a
		// byte array from any string.
		byte[] bytes = new byte[large.length() / 2];

		int before = 0;
		int after = 2;

		for (int i = 0; i < large.length() / 2; i++) {
			Byte current = Byte.valueOf(large.substring(before, after));
			bytes[i] = current;
			before += 2;
			after += 2;
		}
		return bytes;
	}

	public void writeMIDToMultipleBlocks(String id) {
		// this method uses writeSpecific block
		// to write the MID to tags. The MID fills
		// up multiple blocks, which is why we must
		// split up the MID into blocks and the
		// write to each specific block

		String midDec = getMID();
		String m1 = midDec.substring(0, 6);
		String m2 = midDec.substring(6, midDec.length());

		ArrayList<String> mid1Ascii = new ArrayList<String>();
		ArrayList<String> mid2Ascii = new ArrayList<String>();

		for (int i = 0; i < m2.length(); i++) {
			mid2Ascii.add(m2.charAt(i) + "");
		}

		mid2Ascii.set(0, Integer.parseInt(mid2Ascii.get(0)) + 48 + "");
		mid2Ascii.set(1, Integer.parseInt(mid2Ascii.get(1)) + 48 + "");
		mid2Ascii.set(2, Integer.parseInt(mid2Ascii.get(2)) + 48 + "");
		mid2Ascii.set(3, Integer.parseInt(mid2Ascii.get(3)) + 48 + "");
		mid2Ascii.set(4, Integer.parseInt(mid2Ascii.get(4)) + 48 + "");
		mid2Ascii.set(5, Integer.parseInt(mid2Ascii.get(5)) + 48 + "");
		mid2Ascii.set(6, Integer.parseInt(mid2Ascii.get(6)) + 48 + "");
		mid2Ascii.set(7, Integer.parseInt(mid2Ascii.get(7)) + 48 + "");
		mid2Ascii.set(8, Integer.parseInt(mid2Ascii.get(8)) + 48 + "");
		mid2Ascii.set(9, Integer.parseInt(mid2Ascii.get(9)) + 48 + "");

		mid1Ascii.add(0, Integer.parseInt(m1.substring(0, 2)) + 6 + "");
		mid1Ascii.add(1, m1.substring(2, 6));

		String x = mid1Ascii.get(0) + mid1Ascii.get(1);

		writeSpecificBlock(x + mid2Ascii.get(0), id, (byte) 0);
		writeSpecificBlock(mid2Ascii.get(1) + mid2Ascii.get(2) + mid2Ascii.get(3) + mid2Ascii.get(4), id, (byte) 1);
		writeSpecificBlock(mid2Ascii.get(5) + mid2Ascii.get(6) + mid2Ascii.get(7) + mid2Ascii.get(8), id, (byte) 2);
		writeSpecificBlock(mid2Ascii.get(9) + "000000", id, (byte) 3);
	}

	public void writeAFI(String id, String afi) {
		// This method can be called whenever you
		// want to write the AFI on a tag.
		byte byteAfi = (byte) 0x07;
		if (afi.equals("194")) {
			byteAfi = (byte) 0xC2;
		} else if (afi.equals("7") || afi.equals("07")) {
			byteAfi = (byte) 0x07;
		}
		fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_UID, id);
		try {
			int idx = fedm.findTableIndex(0, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_SNR, id);
			if (idx < 0) {
				return;
			}

			fedm.setTableData(idx, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_AFI, byteAfi);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, (byte) 0x27);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, (byte) 0x00);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_ADR, (byte) 0x01);

			fedm.sendProtocol((byte) 0xB0);

		} catch (FedmException ex) {
			ex.printStackTrace();
			logger.log("Error code: " + ex.getErrorcode() + "\n" + ex.toString());
		} catch (FePortDriverException ex) {
			ex.printStackTrace();
			logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
		} catch (FeReaderDriverException ex) {
			// if something happens when sending protocol
			ex.printStackTrace();
			logger.log("Error code: " + ex.getErrorCode() + "\n" + ex.toString());
		}
	}

	public ArrayList<BibTag> getCurrentBibTagsOnScanner() {
		return bibTags;
	}

	public void setAFI(String AFI) {
		this.AFI = AFI;
	}

	public String getAFI() {
		return this.AFI;
	}

	public void setMID(String MID) {
		this.MID = MID;
	}

	public void setUidToWriteAfiTo(String uid) {
		this.uidToWriteTo = uid;
	}

	public void setCallbackMessage(String message) {
		this.callbackMessage = message;
	}

	public String getCallbackMessage() {
		return this.callbackMessage;
	}

	public void run() {
		while (running) {
			String[] tagType;
			String[] serialNumber;
			byte[] data = null;
			byte blockSize = 0;
			int idx = 0;
			int back = 0;

			try {
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, 0x01);
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, 0x00);
				fedm.deleteTable(FedmIscReaderConst.ISO_TABLE);

				fedm.sendProtocol((byte) 0x69); // RFReset
				fedm.sendProtocol((byte) 0xB0); // ISOCmd

				// @TODO - Review: Possibility of infinite loop?
				while (fedm.getLastStatus() == 0x94) { // more flag set?
					fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_MORE, 0x01);
					fedm.sendProtocol((byte) 0xB0);
				}

				serialNumber = new String[fedm.getTableLength(FedmIscReaderConst.ISO_TABLE)];
				tagType = new String[fedm.getTableLength(FedmIscReaderConst.ISO_TABLE)];

				bibTags.clear();
								
				// Register tags on device.
				for (int i = 0; i < fedm.getTableLength(FedmIscReaderConst.ISO_TABLE); i++) {
					serialNumber[i] = fedm.getStringTableData(i, FedmIscReaderConst.ISO_TABLE,
							FedmIscReaderConst.DATA_SNR);
				    if (state.equals("tagSet")) {
						// set MID and AFI on all tags
						BibTag b = new BibTag(serialNumber[i], getMIDFromMultipleBlocks(serialNumber[i]));
						writeMIDToMultipleBlocks(serialNumber[i]);
						writeAFI(serialNumber[i], getAFI());
						b.setAFI(getAFI());
						bibTags.add(b);
					} 
					else if (state.equals("tagSetAFI")) {
						// set AFI on all tags
						BibTag b = new BibTag(serialNumber[i], getMIDFromMultipleBlocks(serialNumber[i]));
						writeAFI(serialNumber[i], getAFI());
						b.setAFI(getAFI());
						bibTags.add(b);

					} 
					else if (state.equals("tagSetAFIOnUID")) {
						// set AFI on specific UID
						String bookUID = serialNumber[i];
						if (bookUID.equals(uidToWriteTo)) {
							BibTag b = new BibTag(serialNumber[i], getMIDFromMultipleBlocks(serialNumber[i]));
							writeAFI(serialNumber[i], getAFI());
							b.setAFI(getAFI());
							bibTags.add(b);
						} 
						else {
							setCallbackMessage("UID: " + serialNumber[i] + ", could not be found on reader");
						}
					}
					else {
						// For every tag on scanner > create a BibTag object
						// with the UID and MID.
						bibTags.add(new BibTag(serialNumber[i], getMIDFromMultipleBlocks(serialNumber[i])));
					}

					tagType[i] = fedm.getStringTableData(i, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_TRTYPE);

					if (tagType[i].equals("00"))
						tagType[i] = "Philips I-Code1";
					if (tagType[i].equals("01"))
						tagType[i] = "Texas Instruments Tag-it HF";
					if (tagType[i].equals("03"))
						tagType[i] = "ISO15693 Transponder";
					if (tagType[i].equals("04"))
						tagType[i] = "ISO14443-A";
					if (tagType[i].equals("05"))
						tagType[i] = "ISO14443-B";
					if (tagType[i].equals("06"))
						tagType[i] = "I-CODE EPC";
					if (tagType[i].equals("07"))
						tagType[i] = "I-CODE UID";
					if (tagType[i].equals("09"))
						tagType[i] = "EPC Class1 Gen2 HF";
					if (tagType[i].equals("81"))
						tagType[i] = "ISO18000-6-B";
					if (tagType[i].equals("84"))
						tagType[i] = "EPC Class1 Gen2 UHF";
				}

				// Compare current tags with tags detected.
				// Emit events if changes.
				for (int i = 0; i < bibTags.size(); i++) {
					Boolean contains = false;
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
					Boolean contains = false;
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

				// If requested current tags.
				if (state.equals("detectTags")) {
					tagListener.tagsDetected(currentTags);
				}

				// Reset state to default = detecting tags.
				state = "";
				
				logger.log("--------------------------");

				logger.log(currentTags.toString());
				
				try {
					Thread.sleep(200); // how often the reader scans for tags
				} catch (InterruptedException e) {
					logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
				}
			} catch (FedmException e) {
				logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
			} catch (FePortDriverException e) {
				logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
			} catch (FeReaderDriverException e) {
				logger.log("Error message: " + e.getMessage() + "\n" + e.toString());
			}
		}
	}
}
