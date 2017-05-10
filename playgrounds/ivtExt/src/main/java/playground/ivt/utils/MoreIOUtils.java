/* *********************************************************************** *
 * project: org.matsim.*
 * MoreIOUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ivt.utils;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Defines some useful i/o related methods, which are not part of the
 * core MATSim IOUtils class.
 * @author thibautd
 */
public class MoreIOUtils {
	private static final Logger log = Logger.getLogger( MoreIOUtils.class );
	private static final String LOGFILE = "MyLogFile.log";
	private static final String WARNLOGFILE = "MyWarnLogFile.log";

	private MoreIOUtils() {
		//no instanciation 
	}

	public static void deleteDirectoryIfExists( final String dir ) {
		final File file = new File( dir );
		if ( file.exists() ) IOUtils.deleteDirectoryRecursively(file.toPath());
	}

	public static File checkFile(final String file) {
		final File f = new File( file +"/" );

		log.info( "Check if file "+file+" does not exist" );
		if ( f.exists() ) {
			throw new IllegalStateException( "file "+file+" exists!" );
		}

		return f;
	}

	public static File checkDirectory(final String outputDir) {
		final File f = new File( outputDir +"/" );

		log.info( "Check if directory "+outputDir+" is empty or does not exist" );
		if ( f.exists() && f.list().length != 0 ) {
			throw new IllegalStateException( "directory "+outputDir+" exists and is not empty!" );
		}

		return f;
	}

	public static File createDirectory(final String outputDir) {
		final File f = checkDirectory( outputDir );

		log.info( "creating directory "+outputDir+" if necessary" );
		if (!f.exists() && !f.mkdirs()) {
			throw new UncheckedIOException( "could not create directory "+outputDir );
		}

		return f;
	}

	/**
	 * creates an output directory if it does not exists, and creates a logfile.
	 */
	public static AutoCloseable initOut( final String outputDir ) {
		return initOut( outputDir , null );
	}

	public static AutoCloseable initOut( final Config config ) {
		return initOut( config.controler().getOutputDirectory() , config );
	}

	public static AutoCloseable initOut( final String outputDir , final Config config ) {
		try {
			createDirectory( outputDir );

			// init logFile
			CollectLogMessagesAppender appender = new CollectLogMessagesAppender();
			Logger.getRootLogger().addAppender(appender);

			initOutputDirLogging(
				outputDir+"/",
				appender.getLogEvents());

			if ( config != null ) new ConfigWriter( config ).write( outputDir+"/output_config.xml" );

			return MoreIOUtils::closeOutputDirLogging;
		} catch (IOException e) {
			// do NOT continue without proper logging!
			throw new RuntimeException("error while creating log file",e);
		}
	}

	/**
	 * Redefine the IOUtils method, with a different file and appender name.
	 * Otherwise, runing a controler in a borader context stops the logging at
	 * the end of the controler shutdown.
	 */
	public static void initOutputDirLogging(
			final String outputDirectory,
			final List<LoggingEvent> logEvents) throws IOException {
		Logger root = Logger.getRootLogger();
		FileAppender appender = new FileAppender(Controler.DEFAULTLOG4JLAYOUT, outputDirectory +
				System.getProperty("file.separator") + LOGFILE);
		appender.setName(LOGFILE);
		root.addAppender(appender);
		FileAppender warnErrorAppender = new FileAppender(Controler.DEFAULTLOG4JLAYOUT, outputDirectory +
				System.getProperty("file.separator") +  WARNLOGFILE);
		warnErrorAppender.setName(WARNLOGFILE);
		warnErrorAppender.setThreshold(Level.WARN);
//		LevelRangeFilter filter = new LevelRangeFilter();
//		filter.setLevelMax(Level.ALL);
//		filter.setAcceptOnMatch(true);
//		filter.setLevelMin(Level.WARN);
//		warnErrorAppender.addFilter(filter);
		root.addAppender(warnErrorAppender);
		if (logEvents != null) {
			for (LoggingEvent e : logEvents) {
				appender.append(e);
				if (e.getLevel().isGreaterOrEqual(Level.WARN)) {
					warnErrorAppender.append(e);
				}
			}
		}
	}

	/**
	 * Redefine the IOUtils method, with a different file and appender name.
	 * Otherwise, runing a controler in a borader context stops the logging at
	 * the end of the controler shutdown.
	 */
	public static void closeOutputDirLogging() {
		Logger root = Logger.getRootLogger();
		Appender app = root.getAppender(LOGFILE);
		root.removeAppender(app);
		app.close();
		app = root.getAppender(WARNLOGFILE);
		root.removeAppender(app);
		app.close();
	}

	public static void writeLines(
			final BufferedWriter writer,
			final String... lines) {
		try {
			for ( String l : lines ) {
				writer.write( l );
				writer.newLine();
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}
}

