/* *********************************************************************** *
 * project: org.matsim.*
 * OutputDirectoryLogging.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.controler;

import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.apache.log4j.spi.LoggingEvent;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;

/**
 * 
 * Put MATSim logs into the output directory.
 * This is static, like Loggers.
 * Call initLogging with either an OutputDirectoryHierarchy or simply with a pathname.
 * Don't forget to call closeOutputDirLogging before quitting the JVM.
 * Call catchLogEntries() before either of the initLogging methods to memorize log entries between that call and the init
 * call and put them into the log file. Useful if creating the log directory only happens after some set-up which
 * allready produces log messages.
 * 
 * @author dgrether, michaz
 *
 */
public final class OutputDirectoryLogging {
	private OutputDirectoryLogging(){} // do not instantiate

	public static final String LOGFILE = "logfile.log";

	public static final String WARNLOGFILE = "logfileWarningsErrors.log";

	private static Logger log = Logger.getLogger(OutputDirectoryLogging.class);

	/** initializes Log4J */
	static {
		final String logProperties = "log4j.xml";
		URL url = Loader.getResource(logProperties);
		if (url != null) {
			PropertyConfigurator.configure(url);
		} else {
			Logger root = Logger.getRootLogger();
			root.setLevel(Level.INFO);
			ConsoleAppender consoleAppender = new ConsoleAppender(Controler.DEFAULTLOG4JLAYOUT, "System.out");
			consoleAppender.setName("A1");
			root.addAppender(consoleAppender);
			consoleAppender.setLayout(Controler.DEFAULTLOG4JLAYOUT);
			log.error("");
			log.error("Could not find configuration file " + logProperties + " for Log4j in the classpath.");
			log.error("A default configuration is used, setting log level to INFO with a ConsoleAppender.");
			log.error("");
			log.error("");
		}
	}


	/**
	 * This variable is used to store the log4j output before it can be written
	 * to a file. This is needed to set the output directory before logging.
	 */
	private static CollectLogMessagesAppender collectLogMessagesAppender = null;

	public static void catchLogEntries() {
		if ( collectLogMessagesAppender != null ) {
			// create a new instance only if there is not one yet, to allow
			// collecting log messages issued _before_ controller construction.
			// Otherwise, all log messages before the last call to this method
			// are lost.
			// td, mar. 2013
			return;
		}

		collectLogMessagesAppender = new CollectLogMessagesAppender();
		Logger.getRootLogger().addAppender(collectLogMessagesAppender);
	}

	/**
	 * Initializes log4j to write log output to files in output directory.
	 * @param outputDirectoryHierarchy TODO
	 */
	public final static void initLogging(OutputDirectoryHierarchy outputDirectoryHierarchy) {
		if (collectLogMessagesAppender != null) {
			Logger.getRootLogger().removeAppender(collectLogMessagesAppender);
		}
		try {
			String outputFilename = outputDirectoryHierarchy.getOutputFilename(LOGFILE);
			String warnLogfileName = outputDirectoryHierarchy.getOutputFilename(WARNLOGFILE);
			initLogging(outputFilename, warnLogfileName);
		} catch (IOException e) {
			log.error("Cannot create logfiles: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Call this method to create 2 log4j logfiles in the output directory specified as parameter.
	 * The first logfile contains all messages the second only those above log Level.WARN (Priority.WARN).
	 * After the end of the programm run it is strongly recommended to close the file logger by calling
	 * the method closeOutputDirLogging().
	 *
	 * @param outputDirectory the outputdirectory to create the files, whithout seperator at the end.
	 * @throws IOException
	 * @see IOUtils#closeOutputDirLogging()
	 * @author dgrether
	 */
	public static void initLoggingWithOutputDirectory(final String outputDirectory) throws IOException {
		if (collectLogMessagesAppender != null) {
			Logger.getRootLogger().removeAppender(collectLogMessagesAppender);
		}
		String logfilename = outputDirectory + System.getProperty("file.separator") + LOGFILE;
		String warnlogfilename = outputDirectory + System.getProperty("file.separator") + WARNLOGFILE;
		initLogging(logfilename, warnlogfilename);
	}

	private static void initLogging(String outputFilename, String warnLogfileName) throws IOException {
		Logger root = Logger.getRootLogger();
		final boolean appendToExistingFile = false; 
		FileAppender appender = new FileAppender(Controler.DEFAULTLOG4JLAYOUT, outputFilename, appendToExistingFile);
		appender.setName(LOGFILE);
		root.addAppender(appender);
		FileAppender warnErrorAppender = new FileAppender(Controler.DEFAULTLOG4JLAYOUT, warnLogfileName, appendToExistingFile);
		warnErrorAppender.setName(WARNLOGFILE);
		warnErrorAppender.setThreshold(Level.WARN);
		root.addAppender(warnErrorAppender);
		if (collectLogMessagesAppender != null) {
			for (LoggingEvent e : collectLogMessagesAppender.getLogEvents()) {
				appender.append(e);
				if (e.getLevel().isGreaterOrEqual(Level.WARN)) {
					warnErrorAppender.append(e);
				}
			}
			collectLogMessagesAppender.close();
			collectLogMessagesAppender = null;
		}
	}

	/**
	 * Call this method to close the log file streams opened by a call of initOutputDirLogging().
	 * This avoids problems concerning open streams after the termination of the program.
	 * @see IOUtils#initLoggingWithOutputDirectory(String)
	 */
	public static void closeOutputDirLogging() {
		//might also be sent to the warn logstream but then you end up with a warning even if everything is alright
		String endLoggingInfo = "closing the logfile, i.e. messages sent to the logger after this message are not written to the logfile.";
		log.info(endLoggingInfo);
		Logger root = Logger.getRootLogger();
		Appender app = root.getAppender(LOGFILE);
		if (app != null) {
			root.removeAppender(app);
			app.close();
		}
		app = root.getAppender(WARNLOGFILE);
		if (app != null) {
			root.removeAppender(app);
			app.close();
		}
	}

}
