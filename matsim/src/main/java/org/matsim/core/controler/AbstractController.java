/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.core.controler;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author nagel
 *
 */
public abstract class AbstractController {

	protected static final Logger log = Logger.getLogger(Controler.class);
	/**
	 * This variable is used to store the log4j output before it can be written
	 * to a file. This is needed to set the output directory before logging.
	 */
	protected CollectLogMessagesAppender collectLogMessagesAppender = null;
	/** The Config instance the Controler uses. */
	protected ScenarioImpl scenarioData = null ;
	protected EventsManagerImpl events = null ;
	protected volatile Throwable uncaughtException;
	protected Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			shutdown(true);
		}
	};
	
	protected abstract void shutdown( boolean unexpected ) ;
	
	AbstractController() {
		// catch logs before doing something
		this.collectLogMessagesAppender = new CollectLogMessagesAppender();
		Logger.getRootLogger().addAppender(this.collectLogMessagesAppender);
		Gbl.printSystemInfo();
		Gbl.printBuildInfo();
		log.info("Used Controler-Class: " + this.getClass().getCanonicalName());

		// make sure we know about any exceptions that lead to abortion of the
		// program
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				log.warn("Getting uncaught Exception in Thread " + t.getName(), e);
				AbstractController.this.uncaughtException = e;
			}
		});
		
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);

	}

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
	 * Initializes log4j to write log output to files in output directory.
	 */
	protected final void initLogging() {
		Logger.getRootLogger().removeAppender(this.collectLogMessagesAppender);
		try {
			IOUtils.initOutputDirLogging(this.scenarioData.getConfig().controler().getOutputDirectory(), 
					this.collectLogMessagesAppender.getLogEvents(), this.scenarioData.getConfig().controler().getRunId());
			this.collectLogMessagesAppender.close();
			this.collectLogMessagesAppender = null;
		} catch (IOException e) {
			log.error("Cannot create logfiles: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * select if single cpu handler to use or parallel
	 */
	protected void initEvents() {
		final String PARALLEL_EVENT_HANDLING = "parallelEventHandling";
		final String NUMBER_OF_THREADS = "numberOfThreads";
		final String ESTIMATED_NUMBER_OF_EVENTS = "estimatedNumberOfEvents";
		String numberOfThreads = this.scenarioData.getConfig().findParam(PARALLEL_EVENT_HANDLING, NUMBER_OF_THREADS);
		String estimatedNumberOfEvents = this.scenarioData.getConfig().findParam(PARALLEL_EVENT_HANDLING, ESTIMATED_NUMBER_OF_EVENTS);
	
		if (numberOfThreads != null) {
			int numOfThreads = Integer.parseInt(numberOfThreads);
			// the user wants to user parallel events handling
			if (estimatedNumberOfEvents != null) {
				int estNumberOfEvents = Integer.parseInt(estimatedNumberOfEvents);
				this.events = new ParallelEventsManagerImpl(numOfThreads, estNumberOfEvents);
			} else {
				this.events = new ParallelEventsManagerImpl(numOfThreads);
			}
		} else {
			this.events = (EventsManagerImpl) EventsUtils.createEventsManager();
		}
	}

}
