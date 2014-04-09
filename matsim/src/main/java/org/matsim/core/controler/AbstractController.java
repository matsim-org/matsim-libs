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

package org.matsim.core.controler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

public abstract class AbstractController {

	private static Logger log = Logger.getLogger(AbstractController.class);

	private OutputDirectoryHierarchy controlerIO;

	/**
	 * This was  public in the design that I found. kai, jul'12
	 */
	public final IterationStopWatch stopwatch = new IterationStopWatch();

	/*
	 * Strings used to identify the operations in the IterationStopWatch.
	 */
	public static final String OPERATION_ITERATION = "iteration";
	public static final String OPERATION_MOBSIM = "mobsim";
	public static final String OPERATION_REPLANNING = "replanning";
	public static final String OPERATION_SCORING = "scoring" ;

	public static final String OPERATION_ITERATION_STARTS_LISTENERS = "iterationStartsListeners";
	public static final String OPERATION_BEFORE_MOBSIM_LISTENERS = "beforeMobsimListeners";
	public static final String OPERATION_AFTER_MOBSIM_LISTENERS = "afterMobsimListeners";
	public static final String OPERATION_ITERATION_ENDS_LISTENERS = "iterationEndsListeners";
	
	/**
	 * This is deliberately not even protected.  kai, jul'12
	 */
	ControlerListenerManager controlerListenerManager;

	// for tests
	protected volatile Throwable uncaughtException;

	private Thread shutdownHook = new Thread() {
		@SuppressWarnings("synthetic-access")
		@Override
		public void run() {
			shutdown(true);
		}
	};

	@Deprecated
	/*package*/ Integer thisIteration = null;


	protected AbstractController() {
		OutputDirectoryLogging.catchLogEntries();
		Gbl.printSystemInfo();
		Gbl.printBuildInfo();
		log.info("Used Controler-Class: " + this.getClass().getCanonicalName());

		// make sure we know about any exceptions that lead to abortion of the
		// program
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				log.error("Getting uncaught Exception in Thread " + t.getName(), e);
				uncaughtException = e;
			}
		});
		if (this instanceof Controler) {
			// Extrawurst for Controler
			this.controlerListenerManager = new ControlerListenerManager((Controler) this);
		} else {
			this.controlerListenerManager = new ControlerListenerManager(null);
		}
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
	}


	private void resetRandomNumbers(long seed, int iteration) {
		MatsimRandom.reset(seed + iteration);
		MatsimRandom.getRandom().nextDouble(); // draw one because of strange
		// "not-randomness" is the first
		// draw...
		// Fixme [kn] this should really be ten thousand draws instead of just
		// one
	}

	protected final void setupOutputDirectory(final String outputDirectory, String runId, final boolean overwriteFiles) {
		this.controlerIO = new OutputDirectoryHierarchy(outputDirectory, runId, overwriteFiles); // output dir needs to be before logging
		OutputDirectoryLogging.initLogging(this.getControlerIO()); // logging needs to be early
	}

	protected final void run(Config config) {
		loadCoreListeners();
		this.controlerListenerManager.fireControlerStartupEvent();
		checkConfigConsistencyAndWriteToLog(config, "config dump before iterations start" ) ;
		prepareForSim();
		doIterations(config.controler().getFirstIteration(), config.global().getRandomSeed(), config.controler().isCreateGraphs());
		shutdown(false);
	}

	final void shutdown(final boolean unexpected) {
		if (unexpected) {
			log.error("S H U T D O W N   ---   received unexpected shutdown request.");
		} else {
			log.info("S H U T D O W N   ---   start regular shutdown.");
		}
		if (this.uncaughtException != null) {
			log.error(
					"Shutdown probably caused by the following Exception.", this.uncaughtException);
		}
		this.controlerListenerManager.fireControlerShutdownEvent(unexpected);
		try {
			Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
		} catch (IllegalStateException e) {
			log.info("Cannot remove shutdown hook. " + e.getMessage());
		}
		this.shutdownHook = null; // important for test cases to free the memory
		if (unexpected) {
			log.error("ERROR --- MATSim terminated with some error. Please check the output or the logfile with warnings and errors for hints.");
			log.error("ERROR --- results should not be used for further analysis.");
			log.error("S H U T D O W N   ---   unexpected shutdown request completed. ");
		} else {
			log.info("S H U T D O W N   ---   regular shutdown completed.");
		}
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	protected abstract void loadCoreListeners();

	protected abstract void runMobSim(int iteration);

	protected abstract void prepareForSim();

	/**
	 * Stopping criterion for iterations.  Design thoughts:<ul>
	 * <li> AbstractController only controls process, not content.  Stopping iterations controls process based on content.
	 * All such coupling methods are abstract; thus this one has to be abstract, too.
	 * <li> One can see this confirmed in the KnSimplifiedControler use case, where the function is delegated to a static
	 * method in the SimplifiedControllerUtils class ... as with all other abstract methods.
	 * </ul>
	 */
	protected abstract boolean continueIterations(int iteration);

	private void doIterations(int firstIteration, long rndSeed, boolean createOutputGraphs) {

		String divider = "###################################################";
		String marker = "### ";

		for (int iteration = firstIteration; continueIterations(iteration) ; iteration++ ) {

			// setting the other iteration counter to the same value:
			this.thisIteration = iteration ;
			// (this other iteration counter may have its own ++ !?!?)

			this.stopwatch.setCurrentIteration(iteration) ;

			log.info(divider);
			log.info(marker + "ITERATION " + iteration + " BEGINS");
//			this.stopwatch.setCurrentIteration(iteration);
			this.stopwatch.beginOperation(OPERATION_ITERATION);
			this.getControlerIO().createIterationDirectory(iteration);
			resetRandomNumbers(rndSeed, iteration);

			this.stopwatch.beginOperation(OPERATION_ITERATION_STARTS_LISTENERS) ;
			this.controlerListenerManager.fireControlerIterationStartsEvent(iteration);
			this.stopwatch.endOperation(OPERATION_ITERATION_STARTS_LISTENERS) ;

			if (iteration > firstIteration) {
				this.stopwatch.beginOperation(OPERATION_REPLANNING);
				this.controlerListenerManager.fireControlerReplanningEvent(iteration);
				this.stopwatch.endOperation(OPERATION_REPLANNING);
			}

			this.stopwatch.beginOperation(OPERATION_BEFORE_MOBSIM_LISTENERS);
			this.controlerListenerManager.fireControlerBeforeMobsimEvent(iteration);
			this.stopwatch.endOperation(OPERATION_BEFORE_MOBSIM_LISTENERS);

			this.stopwatch.beginOperation(OPERATION_MOBSIM);
			resetRandomNumbers(rndSeed, iteration);
			runMobSim(iteration);
			this.stopwatch.endOperation(OPERATION_MOBSIM);
	
			this.stopwatch.beginOperation(OPERATION_AFTER_MOBSIM_LISTENERS) ;
			log.info(marker + "ITERATION " + iteration + " fires after mobsim event");
			this.controlerListenerManager.fireControlerAfterMobsimEvent(iteration);
			this.stopwatch.endOperation(OPERATION_AFTER_MOBSIM_LISTENERS) ;

			this.stopwatch.beginOperation(OPERATION_SCORING);
			log.info(marker + "ITERATION " + iteration + " fires scoring event");
			this.controlerListenerManager.fireControlerScoringEvent(iteration);
			this.stopwatch.endOperation(OPERATION_SCORING);
			
			this.stopwatch.beginOperation(OPERATION_ITERATION_ENDS_LISTENERS) ;
			log.info(marker + "ITERATION " + iteration + " fires iteration end event");
			this.controlerListenerManager.fireControlerIterationEndsEvent(iteration);
			this.stopwatch.endOperation(OPERATION_ITERATION_ENDS_LISTENERS) ;

			this.stopwatch.endOperation(OPERATION_ITERATION);
			this.stopwatch.writeTextFile(this.getControlerIO().getOutputFilename("stopwatch"));
			if (createOutputGraphs) this.stopwatch.writeGraphFile(this.getControlerIO().getOutputFilename("stopwatch"));
			log.info(marker + "ITERATION " + iteration + " ENDS");
			log.info(divider);
		}
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
	 * @param config TODO
	 * @param message
	 *            the message that is written just before the config dump
	 */
	protected static final void checkConfigConsistencyAndWriteToLog(Config config,
			final String message) {
				log.info(message);
				String newline = System.getProperty("line.separator");// use native line endings for logfile
				StringWriter writer = new StringWriter();
				new ConfigWriter(config).writeStream(new PrintWriter(writer), newline);
				log.info(newline + newline + writer.getBuffer().toString());
				log.info("Complete config dump done.");
				log.info("Checking consistency of config...");
				config.checkConsistency();
				log.info("Checking consistency of config done.");
			}

	/**
	 * Design comments:<ul>
	 * <li> This is such that ControlerListenerManager does not need to be exposed.  One may decide otherwise ...  kai, jul'12
	 * </ul>
	 */
	public final void addControlerListener( ControlerListener l ) {
		this.controlerListenerManager.addControlerListener(l) ;
	}

	protected final void addCoreControlerListener( ControlerListener l ) {
		this.controlerListenerManager.addCoreControlerListener(l) ;
	}


	public final OutputDirectoryHierarchy getControlerIO() {
		return controlerIO;
	}


}
