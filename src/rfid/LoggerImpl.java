
package rfid;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerImpl {

	/*
	 * This class is the implementation of the Logger class. This is used to log
	 * error message etc. into the LogFile.log which will be automatically
	 * located in C:\Users\YOUR_USERNAME
	 */

	private Logger logger = Logger.getLogger(LoggerImpl.class.getName());
	private FileHandler fh;

	public LoggerImpl(String path) {
		try {
			fh = new FileHandler(path, true);// when the second parameter is set
												// to true, the logger will
												// append to the logfile. Remove
												// the true statement to make
												// the logger overwrite the
												// logfile.
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

		} catch (IOException ex) {
			Logger.getLogger(LoggerImpl.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SecurityException ex) {
			Logger.getLogger(LoggerImpl.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void log(String message) {
		logger.info(message);
	}
}
