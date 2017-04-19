/* 
 * Based on software from FEIG. 
 * 
 * You can redistribute this software and/or modify it under the terms of 
 * the FEIG Licensing Agreement.
 */

package readers;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import de.feig.FeIscListener;
import de.feig.FePortDriverException;
import de.feig.FeReaderDriverException;
import de.feig.FedmException;
import de.feig.FedmIscReader;
import de.feig.FedmIscReaderConst;
import de.feig.FedmIscReaderID;
import de.feig.TagHandler.FedmIscTagHandler;
import de.feig.TagHandler.FedmIscTagHandler_Result;
import middleware.AbstractTagReader;
import middleware.BibTag;
import middleware.LoggerImpl;
import middleware.TagListenerInterface;

public class FeigReader extends AbstractTagReader implements FeIscListener {
	private FedmIscReader reader;

	/**
	 * Constructor.
	 * 
	 * @param logger The logger implementation.
	 * @param tagListener The tag listener where reader events are passed to.
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
			reader = new FedmIscReader();
		} catch (Exception ex) {
			// @TODO: Handle.
			ex.printStackTrace();
			logger.error("Error message: " + ex.getMessage() + "\n" + ex.getStackTrace());
			return false;
		}

		if (reader == null) {
			return false;
		}

		// Set the table size of the reader.
		// As of now it has been set to 20
		// which means the reader can read an inventory of max 20.
		// The reader will therefore work best when 20 tags on reader.
		try {
			reader.setTableSize(FedmIscReaderConst.ISO_TABLE, 20);
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
		reader.connectUSB(0);
		reader.addEventListener(this, FeIscListener.SEND_STRING_EVENT);
		reader.addEventListener(this, FeIscListener.RECEIVE_STRING_EVENT);
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
			if (reader.isConnected()) {
				reader.removeEventListener(this, FeIscListener.SEND_STRING_EVENT);
				reader.removeEventListener(this, FeIscListener.RECEIVE_STRING_EVENT);
				reader.disConnect();
			}
			return true;
		} catch (Exception e) {
			logger.error("FeigReader closeConnection error: " + e.getMessage() + " --- " + e.getStackTrace());

			return false;
		}
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
			} catch (Exception e) {
				logger.error("USB Connection Error: " + e.getMessage());
				running = false;
				connected = false;
				return false;
			}
		}
		return true;
	}

	@Override
	public void onReceiveProtocol(FedmIscReader arg0, String arg1) {
	}

	@Override
	public void onReceiveProtocol(FedmIscReader arg0, byte[] arg1) {
	}

	@Override
	public void onSendProtocol(FedmIscReader arg0, String arg1) {
	}

	@Override
	public void onSendProtocol(FedmIscReader arg0, byte[] arg1) {
	}

	@Override
	/**
	 * Read AFI.
	 *
	 * @param id
	 *
	 * @return AFI -1 == error
	 */
	public int readAFI(String id) {
		try {
			// Read table.
			reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_UID, id);
			reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, (byte) 0x2B);
			reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, (byte) 0x00);
			reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_ADR, (byte) 0x01);

			reader.sendProtocol((byte) 0xB0);

			int idx = reader.findTableIndex(0, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_SNR, id);
			if (idx < 0) {
				return -1;
			}
			byte afi = reader.getByteTableData(idx, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_AFI);

			return (afi & 0xFF);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return -1;
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

		reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_UID, id);

		try {
			int idx = reader.findTableIndex(0, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_SNR, id);
			if (idx < 0) {
				return false;
			}

			reader.setTableData(idx, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_AFI, byteAfi);
			reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, (byte) 0x27);
			reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, (byte) 0x00);
			reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_ADR, (byte) 0x01);

			reader.sendProtocol((byte) 0xB0);

			return true;
		} catch (Exception e) {
			logger.error("Error code: " + e.getMessage());
			return false;
		}
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
			reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, 0x01);
			reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, 0x00);
			reader.deleteTable(FedmIscReaderConst.ISO_TABLE);
			reader.sendProtocol((byte) 0x69); // RFReset
			reader.sendProtocol((byte) 0xB0); // ISOCmd

			while (reader.getLastStatus() == 0x94) { // more flag set?
				reader.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_MORE, 0x01);
				reader.sendProtocol((byte) 0xB0);
			}
		} catch (Exception e) {
			logger.error("Error code: " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	/**
	 * Get the tags currently reachable.
	 */
	protected HashMap<String, BibTag> getTags() throws FedmException, FePortDriverException, FeReaderDriverException {
		HashMap<String, BibTag> tags = new HashMap<String, BibTag>();
		FedmIscTagHandler th = null;
		FedmIscTagHandler_Result res = new FedmIscTagHandler_Result();
		int dataReadSuccess = 0;
		int tagDriver = 0;
		BibTag tag;
		String uid;
		String afi;

		HashMap<String, FedmIscTagHandler> inventory = reader.tagInventory(true, (byte) 0, (byte) 1);

		// Read tags with data.
		for (Map.Entry<String, FedmIscTagHandler> entry : inventory.entrySet()) {
			// Get the UID.
			uid = (String) entry.getKey();

			// Select tag handler.
			th = reader.tagSelect(entry.getValue(), tagDriver);

			// Read the data blocks.
			dataReadSuccess = th.readMultipleBlocksWithSecStatus(0, 8, res);

			// 0 = success. If success, the result is in the res variable.
			if (dataReadSuccess == 0) {
				tags.put(uid, new BibTag(uid, DatatypeConverter.printHexBinary(res.data), null));
			}
			else {
				logger.error("Could not read data for UID: " + uid + ", ignoring tag.");
			}
		}

		// Read AFI values.
		for (Map.Entry<String, BibTag> entry : tags.entrySet()) {
			tag = entry.getValue();

			afi = Integer.toString(readAFI(tag.getUID()));

			tag.setAFI(afi);
		}

		return tags;
	}
}
