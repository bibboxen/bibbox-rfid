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
import de.feig.TagHandler.FedmIscTagHandler_ISO15693;
import de.feig.TagHandler.FedmIscTagHandler_Result;
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
	 *            The logger implementation.
	 * @param tagListener
	 *            The tag listener where reader events are passed to.
	 */
	public FeigReader(LoggerImpl logger, TagListenerInterface tagListener, int successfulReadsThreshold,
			int threadSleepInMillis) {
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

		// Read important reader properties and set reader type in reader
		// object.
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
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_REQ_UID, id);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_CMD, (byte) 0x2B);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE, (byte) 0x00);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_B0_MODE_ADR, (byte) 0x01);

			fedm.sendProtocol((byte) 0xB0);

			int idx = fedm.findTableIndex(0, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_SNR, id);
			if (idx < 0) {
				return -1;
			}
			byte afi = fedm.getByteTableData(idx, FedmIscReaderConst.ISO_TABLE, FedmIscReaderConst.DATA_AFI);

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
		} catch (Exception e) {
			logger.error("Error code: " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	protected HashMap<String, BibTag> getTags() throws FedmException, FePortDriverException, FeReaderDriverException {
		HashMap<String, BibTag> tags = new HashMap<String, BibTag>();

		HashMap<String, FedmIscTagHandler> inventory = fedm.tagInventory(true, (byte) 0, (byte) 1);

		FedmIscTagHandler th = null;
		FedmIscTagHandler_Result res = new FedmIscTagHandler_Result();
		int back = 0;
		int tagDriver = 0;

		// Read tags with data.
		for (Map.Entry<String, FedmIscTagHandler> e : inventory.entrySet()) {
			String uid = (String) e.getKey();
			th = (FedmIscTagHandler) e.getValue();
			th = fedm.tagSelect(th, tagDriver);

			if (th instanceof FedmIscTagHandler_ISO15693) {
				FedmIscTagHandler_ISO15693 thIso = (FedmIscTagHandler_ISO15693) th;
				back = thIso.readMultipleBlocksWithSecStatus(0, 8, res);
				
				// 0 = success.
				if (back == 0) {
					tags.put(uid, new BibTag(uid, DatatypeConverter.printHexBinary(res.data), null));
				}
			} else {
				logger.error("TagHandler not instanceof FedmIscTagHandler_ISO15693 for UID: " + uid);
			}
		}

		// Read AFI values.
		for (Map.Entry<String, BibTag> entry : tags.entrySet()) {
			BibTag tag = entry.getValue();

			String afi = Integer.toString(readAFI(tag.getUID()));

			tag.setAFI(afi);
		}

		return tags;
	}
}
