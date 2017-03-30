package middleware;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Logger
 * 
 * This class is the implementation of the Logger class. This is used to log
 * error message etc. into the LogFile.log.
 */
public class LoggerImpl {
	private Logger logger;
	private FileHandler fh;
	private ConsoleHandler ch;
	private LogManager lm;
	private Level logLevel;

	/**
	 * Constructor.
	 * 
	 * @param path Path to the log file.
	 * @param loglevel The log level.
	 * @param toFile Log to file?
	 */
	public LoggerImpl(String path, String loglevel, boolean toFile, boolean toConsole) {
		lm = LogManager.getLogManager();
		lm.reset();

		logger = Logger.getLogger(LoggerImpl.class.getName());
		SimpleFormatter formatter = new SimpleFormatter();

		logLevel = parseLogLevel(loglevel);
		
		try {
			if (toFile) {
				fh = new FileHandler(path, true);   
				fh.setFormatter(formatter);
				logger.addHandler(fh);
			}

			if (toConsole) {
				ch = new ConsoleHandler();
				ch.setFormatter(formatter);
				logger.addHandler(ch);
			}
			
			logger.setLevel(logLevel);
			
			info("Logger set up.");
		} catch (Exception ex) {
			System.out.println(ex.getStackTrace());
		}
	}
	
	/**
	 * Parse log level string.
	 * 
	 * off, info, error
	 * 
	 * defaults to ALL.
	 * 
	 * @param loglevel
	 * @return
	 */
	private static Level parseLogLevel(String loglevel) {
		if (loglevel.equals("off")) {
			return Level.OFF;
		}
		else if (loglevel.equals("info")) {
			return Level.INFO;
		}
		else if (loglevel.equals("error")) {
			return Level.SEVERE;
		}
		else {
			return Level.ALL;
		}
	}
	
	/**
	 * Log message.
	 * 
	 * @param message
	 */
	public void info(String message) {
		logger.log(Level.INFO, message);
	}
	
	/**
	 * Warning message.
	 * 
	 * @param message
	 */
	public void warning(String message) {
		logger.log(Level.WARNING, message);
	}
	
	/**
	 * Error message.
	 * 
	 * @param message
	 */
	public void error(String message) {
		logger.log(Level.SEVERE, message);
	}
}
