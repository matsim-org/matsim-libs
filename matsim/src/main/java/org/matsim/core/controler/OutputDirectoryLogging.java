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
import java.nio.file.FileSystems;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.CollectLogMessagesAppender;

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

	private static Logger log = LogManager.getLogger(OutputDirectoryLogging.class);

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
		collectLogMessagesAppender.start();
		{
			final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			ctx.getConfiguration().getRootLogger().addAppender(collectLogMessagesAppender, Level.ALL, null);
			ctx.updateLoggers();
		}
	}

	/**
	 * Initializes log4j to write log output to files in output directory.
	 * @param outputDirectoryHierarchy
	 */
	public final static void initLogging(OutputDirectoryHierarchy outputDirectoryHierarchy) {
		if (collectLogMessagesAppender != null) {
			final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			ctx.getConfiguration().getRootLogger().removeAppender(collectLogMessagesAppender.getName());
			ctx.updateLoggers();
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
	 * @see OutputDirectoryLogging#closeOutputDirLogging()
	 * @author dgrether
	 */
	public static void initLoggingWithOutputDirectory(final String outputDirectory) throws IOException {
		if (collectLogMessagesAppender != null) {
			final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			ctx.getConfiguration().getRootLogger().removeAppender(collectLogMessagesAppender.getName());
			ctx.updateLoggers();
		}
		String logfilename = outputDirectory + FileSystems.getDefault().getSeparator() + LOGFILE;
		String warnlogfilename = outputDirectory + FileSystems.getDefault().getSeparator() + WARNLOGFILE;
		initLogging(logfilename, warnlogfilename);
	}

	private static void initLogging(String outputFilename, String warnLogfileName) throws IOException {
		// code inspired from http://logging.apache.org/log4j/2.x/manual/customconfig.html#AddingToCurrent

		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		final boolean appendToExistingFile = false;

		FileAppender appender;
		{ // the "all" logfile
			appender = FileAppender.newBuilder().setName(LOGFILE).setLayout(Controler.DEFAULTLOG4JLAYOUT).withFileName(outputFilename).withAppend(appendToExistingFile).build();
			appender.start();
			config.getRootLogger().addAppender(appender, Level.ALL, null);
		}

		FileAppender warnErrorAppender;
		{ // the "warnings and errors" logfile
			warnErrorAppender = FileAppender.newBuilder().setName(WARNLOGFILE).setLayout(Controler.DEFAULTLOG4JLAYOUT).withFileName(warnLogfileName).withAppend(appendToExistingFile).build();
			warnErrorAppender.start();
			config.getRootLogger().addAppender(warnErrorAppender, Level.WARN, null);
		}

		ctx.updateLoggers();

		if (collectLogMessagesAppender != null) {
			for (LogEvent e : collectLogMessagesAppender.getLogEvents()) {
				appender.append(e);
				if (e.getLevel().isMoreSpecificThan(Level.WARN)) {
					warnErrorAppender.append(e);
				}
			}
			collectLogMessagesAppender.stop();
			collectLogMessagesAppender = null;
		}
		Gbl.printSystemInfo();
		Gbl.printBuildInfo();
		Gbl.printRunCommand();
	}

	/**
	 * Call this method to close the log file streams opened by a call of initOutputDirLogging().
	 * This avoids problems concerning open streams after the termination of the program.
	 * @see OutputDirectoryLogging#initLoggingWithOutputDirectory(String)
	 */
	public static void closeOutputDirLogging() {
		//might also be sent to the warn logstream but then you end up with a warning even if everything is alright
		String endLoggingInfo = "closing the logfile, i.e. messages sent to the logger after this message are not written to the logfile.";
		log.info(endLoggingInfo);
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		org.apache.logging.log4j.core.Logger root = LoggerContext.getContext(false).getRootLogger();
		Appender app = root.getAppenders().get(LOGFILE);
		if (app != null) {
			root.removeAppender(app);
			app.stop();
		}
		app = root.getAppenders().get(WARNLOGFILE);
		if (app != null) {
			root.removeAppender(app);
			app.stop();
		}
		ctx.updateLoggers();
	}
}
