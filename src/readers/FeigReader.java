/* 
 * Copyright (C) 2016 ID-advice
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can redistribute this software and/or modify it under the terms of 
 * the FEIG Licensing Agreement, which can be found in the OBID folder.
 */

package readers;

import java.util.ArrayList;
import java.util.HashMap;

import de.feig.FeIscListener;
import de.feig.FePortDriverException;
import de.feig.FeReaderDriverException;
import de.feig.FedmException;
import de.feig.FedmIscReader;
import de.feig.FedmIscReaderConst;
import de.feig.FedmIscReaderID;
import middleware.AbstractTagReader;
import middleware.BibTag;
import middleware.LoggerImpl;
import middleware.TagListenerInterface;

public class FeigReader extends AbstractTagReader implements FeIscListener {
	private FedmIscReader fedm;

	/**
	 * Constructor.
	 * 
	 * @param logger
	 *   The logger implementation.
	 * @param tagListener
	 *   The tag listener where reader events are passed to.
	 */
	public FeigReader(LoggerImpl logger, TagListenerInterface tagListener, int successfulReadsThreshold, int threadSleepInMillis) {
		this.logger = logger;
		this.tagListener = tagListener;
		this.successfulReadsThreshold = successfulReadsThreshold;
		this.threadSleepInMillis = threadSleepInMillis;
	}	

	/**
	 * Initialize FeigReader
	 * 
	 * @return boolean
	 */
	private boolean initiateFeigReader() {
		// Initiate the reader object.
		try {
			fedm = new FedmIscReader();
		} catch (Exception ex) {
			// @TODO: Handle.
			ex.printStackTrace();
			logger.error("Error message: " + ex.getMessage() + "\n" + ex.getStackTrace());
			return false;
		}

		if (fedm == null) {
			return false;
		}

		// Set the table size of the reader.
		// As of now it has been set to 20
		// which means the reader can read an inventory of max 20.
		// The reader will therefore work best when 20 tags on reader.
		try {
			fedm.setTableSize(FedmIscReaderConst.ISO_TABLE, 20);
			return true;
		} catch (FedmException ex) {
			logger.error("Error code: " + ex.getErrorcode() + "\n" + ex.getStackTrace());
		}
		return false;
	}
	
	/**
	 * Open USB port.
	 * 
	 * @throws FeReaderDriverException 
	 * @throws FePortDriverException 
	 * @throws FedmException 
	 */
	private void openUSBPort() throws FedmException, FePortDriverException, FeReaderDriverException {
		// Close connection if any has already been established.
		closeConnection();

		// Connect to USB.
		fedm.connectUSB(0);
		fedm.addEventListener(this, FeIscListener.SEND_STRING_EVENT);
		fedm.addEventListener(this, FeIscListener.RECEIVE_STRING_EVENT);

		// Read important reader properties and set reader type in reader object.
		fedm.readReaderInfo();
	}

	/**
	 * Close connection to FeigReader.
	 * 
	 * @throws FedmException 
	 * @throws FeReaderDriverException 
	 * @throws FePortDriverException 
	 */
	public boolean closeConnection() {
		try {
			// Close connection if there is any.
			if (fedm.isConnected()) {
				fedm.removeEventListener(this, FeIscListener.SEND_STRING_EVENT);
				fedm.removeEventListener(this, FeIscListener.RECEIVE_STRING_EVENT);
				fedm.disConnect();
			}
			return true;
		}
		catch (Exception e) {
			logger.error("FeigReader closeConnection error: " + e.getMessage() + " --- " + e.getStackTrace());
			
			return false;
		}
	}
	
	@Override
	public void onReceiveProtocol(FedmIscReader arg0, String arg1) {}

	@Override
	public void onReceiveProtocol(FedmIscReader arg0, byte[] arg1) {}

	@Override
	public void onSendProtocol(FedmIscReader arg0, String arg1) {}

	@Override
	public void onSendProtocol(FedmIscReader arg0, byte[] arg1) {}

	@Override
	/**
	 * Read AFI.
	 *
	 * @param id
	 *
	 * @return
	 *   AFI
	 *   -1 == error
	 */
	public int readAFI(String id) {
		try {
			// Read table.
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_UID, id);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, (byte)0x2B);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, (byte) 0x00);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_ADR, (byte) 0x01);

			fedm.sendProtocol((byte) 0xB0);

			int idx = fedm.findTableIndex(0, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_SNR, id);
			if (idx < 0) {
				return -1;
			}
			byte afi = fedm.getByteTableData(idx, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_AFI);

			return (afi & 0xFF);
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
		return -1;
	}
	
	/**
	 * Get the data block.
	 * 
	 * @param id
	 *   @TODO: What is this id?
	 * 
	 * @return
	 */
	private String getData(String id) {
		String b0 = readSpecificBlock(id, (byte) 0);
		String b1 = readSpecificBlock(id, (byte) 1);
		String b2 = readSpecificBlock(id, (byte) 2);
		String b3 = readSpecificBlock(id, (byte) 3);
		String b4 = readSpecificBlock(id, (byte) 4);
		String b5 = readSpecificBlock(id, (byte) 5);
		String b6 = readSpecificBlock(id, (byte) 6);
		String b7 = readSpecificBlock(id, (byte) 7);

		return b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7;
	}

	/**
	 * Read block.
	 * 
	 * This method is used when reading a specific block (dbAddress).
	 * 
	 * @param id
	 *   @TODO
	 * @param dbAddress
	 *   The block address.
	 *   
	 * @return String
	 *   @TODO
	 */
	private String readSpecificBlock(String id, byte dbAddress) {
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
			dataBlockString = fedm.getStringTableData(idx, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_RxDB, dbAddress);

			return dataBlockString;
		} catch (FePortDriverException ex) {
			logger.error("Error code: " + ex.getErrorCode() + "\n" + ex.getStackTrace());
		} catch (FeReaderDriverException ex) {
			logger.error("Error code: " + ex.getErrorCode() + "\n" + ex.getStackTrace());
		} catch (FedmException ex) {
			logger.error("Error code: " + ex.getErrorcode() + "\n" + ex.getStackTrace());
		}
		return "";
	}

	/**
	 * Write block.
	 * 
	 * @param data
	 * @param id
	 * @param dbAddress
	 */
	private void writeSpecificBlock(String data, String id, byte dbAddress) {
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
			logger.error("Error code: " + ex.getErrorcode() + "\n" + ex.getStackTrace());
		} catch (FePortDriverException ex) {
			logger.error("Error code: " + ex.getErrorCode() + "\n" + ex.getStackTrace());
		} catch (FeReaderDriverException ex) {
			logger.error("Error code: " + ex.getErrorCode() + "\n" + ex.getStackTrace());
		}
	}

	/**
	 * Get Byte array from string
	 * 
	 * @param large
	 * @return
	 */
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

	/**
	 * Write MID.
	 * 
	 * @param id
	 */
	public void writeMIDToMultipleBlocks(String id, String midDec) {
		// this method uses writeSpecific block
		// to write the MID to tags. The MID fills
		// up multiple blocks, which is why we must
		// split up the MID into blocks and the
		// write to each specific block

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

	@Override
	/**
	 * Write AFI.
	 * 
	 * @param id
	 * @param afi
	 */
	public boolean writeAFI(String id, String afi) {
		// Create byte for AFI.
		int dec = Integer.parseInt(afi);
		String afiHex = Integer.toHexString(dec);
		byte byteAfi = (byte) (Integer.parseInt(afiHex, 16) & 0xff);

		fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_UID, id);

		try {
			int idx = fedm.findTableIndex(0, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_SNR, id);
			if (idx < 0) {
				return false;
			}

			fedm.setTableData(idx, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_AFI, byteAfi);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, (byte) 0x27);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, (byte) 0x00);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_ADR, (byte) 0x01);

			fedm.sendProtocol((byte) 0xB0);

			return true;
		} catch (Exception e) {
			logger.error("Error code: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Get UIDs of the tags on the device.
	 * 
	 * @return
	 * @throws FedmException
	 * @throws FePortDriverException
	 * @throws FeReaderDriverException
	 */
	public String[] getUIDs() throws FedmException, FePortDriverException, FeReaderDriverException {
		// Clear ISO table.
		clearReader();

		// Get number of entries in the ISO_TABLE.
		int tableLength = fedm.getTableLength(FedmIscReaderConst.ISO_TABLE);
		
		String[] uids = new String[tableLength];
		
		// Register tags on device.
		for (int i = 0; i < tableLength; i++) {
			String UID = fedm.getStringTableData(i, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_SNR);
			uids[i] = UID;
		}
		
		return uids;
	}
	
	@Override
	/**
	 * Clear data table.
	 *
	 * @return boolean Success?
	 */
	public boolean clearReader() {
		try {
			// Clear for new read.
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, 0x01);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, 0x00);
			fedm.deleteTable(FedmIscReaderConst.ISO_TABLE);
			fedm.sendProtocol((byte) 0x69); // RFReset
			fedm.sendProtocol((byte) 0xB0); // ISOCmd

			while (fedm.getLastStatus() == 0x94) { // more flag set?
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_MORE, 0x01);
				fedm.sendProtocol((byte) 0xB0);
			}
		}
		catch (Exception e) {
			logger.error("Error code: " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public boolean connect() {
		logger.info("Connecting to FEIG reader.");
		
		// Initialize FEIG Reader.
		if (!initiateFeigReader()) {
			logger.error("FEIG Reader: Error - CANNOT INITIALIZE");
			running = false;
			connected = false;
			return false;
		} else {
			try {
				openUSBPort();
				logger.error("USB Connection: ESTABLISHED");
				
				connected = true;
			}
			catch (Exception e) {
				logger.error("USB Connection Error: " + e.getMessage());
				running = false;
				connected = false;
				return false;
			}
		}
		return true;
	}

	@Override
	protected HashMap<String, BibTag> getTags() {
		try {
			String[] uids = getUIDs();
			
			// Clear registered tags.
			HashMap<String, BibTag> tags = new HashMap<String, BibTag>();

			
			// Register tags on device.
			for (int i = 0; i < uids.length; i++) {
				String data = getData(uids[i]);
				String afi = "" + readAFI(uids[i]);

				// Make sure the tag has been read correctly.
				tags.put(uids[i],
					new BibTag(
						uids[i],
						data,
						afi
					)
				);
			}
			
			return tags;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
