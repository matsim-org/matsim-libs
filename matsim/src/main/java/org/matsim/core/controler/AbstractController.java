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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler.ControlerState;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.knowledges.Knowledges;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.population.algorithms.PlanAlgorithm;

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
	protected ControlerState state = ControlerState.Init;
	/**
	 * This instance encapsulates all behavior concerning the
	 * ControlerEvents/Listeners
	 */
	protected ControlerListenerManager controlerListenerManager = new ControlerListenerManager(null);
	protected ControlerIO controlerIO;
	protected boolean dumpDataAtEnd = true;
	protected boolean overwriteFiles = false;
	String outputPath = null;
	
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
	 * yy Example that is is also possible to do this statically.  kai, jun'12
	 */
	static protected final void initLogging(CollectLogMessagesAppender appender, 
			ControlerConfigGroup controlerConfig ) {
		Logger.getRootLogger().removeAppender(appender);
		try {
			IOUtils.initOutputDirLogging(controlerConfig.getOutputDirectory(), 
					appender.getLogEvents(), controlerConfig.getRunId());
			appender.close();
			appender = null;
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

	protected void shutdown(final boolean unexpected) {
		ControlerState oldState = this.state;
		this.state = ControlerState.Shutdown;
		if (oldState == ControlerState.Running) {
			if (unexpected) {
				log.warn("S H U T D O W N   ---   received unexpected shutdown request.");
			} else {
				log.info("S H U T D O W N   ---   start regular shutdown.");
			}
			if (this.uncaughtException != null) {
				log.warn(
						"Shutdown probably caused by the following Exception.", this.uncaughtException);
			}
			this.controlerListenerManager.fireControlerShutdownEvent(unexpected);
			if (this.dumpDataAtEnd) {
				// dump plans
				Knowledges kk ;
				if ( this.scenarioData.getConfig().scenario().isUseKnowledges()) {
					kk = (this.scenarioData).getKnowledges();
				} else {
					kk = this.scenarioData.retrieveNotEnabledKnowledges() ;
				}
				new PopulationWriter(this.scenarioData.getPopulation(), this.scenarioData.getNetwork(), kk).write(this.controlerIO.getOutputFilename(Controler.FILENAME_POPULATION));
				// dump network
				new NetworkWriter(this.scenarioData.getNetwork()).write(this.controlerIO.getOutputFilename(Controler.FILENAME_NETWORK));
				// dump config
				new ConfigWriter(this.scenarioData.getConfig()).write(this.controlerIO.getOutputFilename(Controler.FILENAME_CONFIG));
				// dump facilities
				ActivityFacilities facilities = this.scenarioData.getActivityFacilities();
				if (facilities != null) {
					new FacilitiesWriter((ActivityFacilitiesImpl) facilities)
					.write(this.controlerIO.getOutputFilename("output_facilities.xml.gz"));
				}
				if (((NetworkFactoryImpl) this.scenarioData.getNetwork().getFactory()).isTimeVariant()) {
					new NetworkChangeEventsWriter().write(this.controlerIO.getOutputFilename("output_change_events.xml.gz"),
							((NetworkImpl) this.scenarioData.getNetwork()).getNetworkChangeEvents());
				}
				if (this.scenarioData.getConfig().scenario().isUseHouseholds()) {
					new HouseholdsWriterV10(this.scenarioData.getHouseholds())
					.writeFile(this.controlerIO.getOutputFilename(Controler.FILENAME_HOUSEHOLDS));
				}
				if (this.scenarioData.getConfig().scenario().isUseLanes()) {
					new LaneDefinitionsWriter20(
							this.scenarioData.getScenarioElement(LaneDefinitions20.class)).write(this.controlerIO.getOutputFilename(Controler.FILENAME_LANES));
				}
				if (!unexpected	&& this.scenarioData.getConfig().vspExperimental().isWritingOutputEvents()) {
					File toFile = new File(	this.controlerIO.getOutputFilename("output_events.xml.gz"));
					File fromFile = new File(this.controlerIO.getIterationFilename(this.scenarioData.getConfig().controler().getLastIteration(), "events.xml.gz"));
					IOUtils.copyFile(fromFile, toFile);
				}
			}
			if (unexpected) {
				log.info("S H U T D O W N   ---   unexpected shutdown request completed.");
			} else {
				log.info("S H U T D O W N   ---   regular shutdown completed.");
			}
			try {
				Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
			} catch (IllegalStateException e) {
				log.info("Cannot remove shutdown hook. " + e.getMessage());
			}
			this.shutdownHook = null; // important for test cases to free the
			// memory
			this.collectLogMessagesAppender = null;
			IOUtils.closeOutputDirLogging();
		}
	}

	protected AbstractMultithreadedModule wrapPlanAlgo(final PlanAlgorithm planAlgo) {
		// wrap it into the AbstractMultithreadedModule:
		final AbstractMultithreadedModule router = new AbstractMultithreadedModule(this.scenarioData.getConfig().global().getNumberOfThreads()) {
			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return planAlgo ;
			}
		};
		return router;
	}

	/**
	 * Design decisions:
	 * <ul>
	 * <li>I extracted this method since it is now called <i>twice</i>: once
	 * directly after reading, and once before the iterations start. The second
	 * call seems more important, but I wanted to leave the first one there in
	 * case the program fails before that config dump. Might be put into the
	 * "unexpected shutdown hook" instead. kai, dec'10
	 * </ul>
	 * 
	 * @param message
	 *            the message that is written just before the config dump
	 */
	protected void checkConfigConsistencyAndWriteToLog(final String message) {
		log.info(message);
		String newline = System.getProperty("line.separator");// use native line endings for logfile
		StringWriter writer = new StringWriter();
		new ConfigWriter(this.scenarioData.getConfig()).writeStream(new PrintWriter(writer), newline);
		log.info(newline + newline + writer.getBuffer().toString());
		log.info("Complete config dump done.");
		log.info("Checking consistency of config...");
		this.scenarioData.getConfig().checkConsistency();
		log.info("Checking consistency of config done.");
	}

	/**
	 * Creates the path where all iteration-related data should be stored.
	 * 
	 * @param iteration
	 */
	protected void makeIterationPath(final int iteration) {
		File dir = new File(this.controlerIO.getIterationPath(iteration));
		if (!dir.mkdir()) {
			if (this.overwriteFiles && dir.exists()) {
				log.info("Iteration directory "
						+ this.controlerIO.getIterationPath(iteration)
						+ " exists already.");
			} else {
				log.warn("Could not create iteration directory "
						+ this.controlerIO.getIterationPath(iteration) + ".");
			}
		}
	}

	protected void resetRandomNumbers(int iteration) {
		MatsimRandom.reset(this.scenarioData.getConfig().global().getRandomSeed()
				+ iteration);
		MatsimRandom.getRandom().nextDouble(); // draw one because of strange
		// "not-randomness" is the first
		// draw...
		// Fixme [kn] this should really be ten thousand draws instead of just
		// one
	}

	protected final void setUpOutputDir() {
		this.outputPath = this.scenarioData.getConfig().controler().getOutputDirectory();
		if (this.outputPath.endsWith("/")) {
			this.outputPath = this.outputPath.substring(0, this.outputPath.length() - 1);
		}
		if (this.scenarioData.getConfig().controler().getRunId() != null) {
			this.controlerIO = new ControlerIO(this.outputPath, this.scenarioData.createId(this.scenarioData.getConfig().controler().getRunId()));
		} else {
			this.controlerIO = new ControlerIO(this.outputPath);
		}
	
		// make the tmp directory
		File outputDir = new File(this.outputPath);
		if (outputDir.exists()) {
			if (outputDir.isFile()) {
				throw new RuntimeException("Cannot create output directory. "
						+ this.outputPath + " is a file and cannot be replaced by a directory.");
			}
			if (outputDir.list().length > 0) {
				if (this.overwriteFiles) {
					System.out.flush();
					log.warn("###########################################################");
					log.warn("### THE CONTROLER WILL OVERWRITE FILES IN:");
					log.warn("### " + this.outputPath);
					log.warn("###########################################################");
					System.err.flush();
				} else {
					// the directory is not empty, we do not overwrite any
					// files!
					throw new RuntimeException(
							"The output directory " + this.outputPath
							+ " exists already but has files in it! Please delete its content or the directory and start again. We will not delete or overwrite any existing files.");
				}
			}
		} else {
			if (!outputDir.mkdirs()) {
				throw new RuntimeException(
						"The output directory path " + this.outputPath
						+ " could not be created. Check pathname and permissions!");
			}
		}
	
		File tmpDir = new File(this.controlerIO.getTempPath());
		if (!tmpDir.mkdir() && !tmpDir.exists()) {
			throw new RuntimeException("The tmp directory "
					+ this.controlerIO.getTempPath() + " could not be created.");
		}
		File itersDir = new File(this.outputPath + "/" + Controler.DIRECTORY_ITERS);
		if (!itersDir.mkdir() && !itersDir.exists()) {
			throw new RuntimeException("The iterations directory "
					+ (this.outputPath + "/" + Controler.DIRECTORY_ITERS)
					+ " could not be created.");
		}
	}

}
