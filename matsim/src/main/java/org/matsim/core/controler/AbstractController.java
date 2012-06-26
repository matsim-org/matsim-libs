package org.matsim.core.controler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

public abstract class AbstractController {
	
	private static Logger log = Logger.getLogger(AbstractController.class);

//	private Config config;
	
	/**Helper class to wrap a standard plan algo into a multithreaded plan algo.
	 * 
	 * @param planAlgo
	 * @param numberOfThreads
	 * @return
	 */
	protected static final AbstractMultithreadedModule wrapPlanAlgo(final PlanAlgorithm planAlgo, int numberOfThreads) {
		// wrap it into the AbstractMultithreadedModule:
		final AbstractMultithreadedModule router = new AbstractMultithreadedModule(numberOfThreads) {
			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return planAlgo ;
			}
		};
		return router;
	}


	protected OutputDirectoryHierarchy controlerIO;

	/**
	 * This was  public in the design that I found. kai, jul'12
	 */
	public final IterationStopWatch stopwatch = new IterationStopWatch();
	
	protected ControlerListenerManager controlerListenerManager;
	
	// for tests
	protected volatile Throwable uncaughtException;
	
	private Thread shutdownHook = new Thread() {
		@SuppressWarnings("synthetic-access")
		@Override
		public void run() {
			log.warn("S H U T D O W N   ---   received unexpected shutdown request.");
			shutdown(true);
			log.info("S H U T D O W N   ---   unexpected shutdown request completed.");
		}
	};
	

	public AbstractController() {
		Gbl.printSystemInfo();
		Gbl.printBuildInfo();
		log.info("Used Controler-Class: " + this.getClass().getCanonicalName());

		// make sure we know about any exceptions that lead to abortion of the
		// program
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				log.warn("Getting uncaught Exception in Thread " + t.getName(), e);
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
		OutputDirectoryLogging.catchLogEntries();
	}


	private void resetRandomNumbers(long seed, int iteration) {
		MatsimRandom.reset(seed + iteration);
		MatsimRandom.getRandom().nextDouble(); // draw one because of strange
		// "not-randomness" is the first
		// draw...
		// Fixme [kn] this should really be ten thousand draws instead of just
		// one
	}

	public final void setupOutputDirectory(final String outputDirectory, final boolean overwriteFiles) {
		this.controlerIO = new OutputDirectoryHierarchy(outputDirectory, overwriteFiles); // output dir needs to be before logging
		OutputDirectoryLogging.initLogging(this.controlerIO); // logging needs to be early
	}

	public final void run(Config config) {
		loadCoreListeners();
		this.controlerListenerManager.fireControlerStartupEvent();
		// make sure all routes are calculated.
		prepareForSim();
		doIterations(config.controler().getFirstIteration(), config.controler().getLastIteration(), config.global().getRandomSeed());
		shutdown(false);
	}
	
	public final void shutdown(final boolean unexpected) {
		this.controlerListenerManager.fireControlerShutdownEvent(unexpected);
		try {
			Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
		} catch (IllegalStateException e) {
			log.info("Cannot remove shutdown hook. " + e.getMessage());
		}
		this.shutdownHook = null; // important for test cases to free the memory
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	protected abstract void loadCoreListeners();
	
	protected abstract void runMobSim(int iteration);
	
	protected abstract void prepareForSim();
	
	private void doIterations(int firstIteration, int lastIteration, long rndSeed) {

		String divider = "###################################################";
		String marker = "### ";

		for (int iteration = firstIteration; iteration <= lastIteration; iteration++ ) {
			this.stopwatch.setCurrentIteration(iteration) ;

			log.info(divider);
			log.info(marker + "ITERATION " + iteration + " BEGINS");
			this.stopwatch.setCurrentIteration(iteration);
			this.stopwatch.beginOperation("iteration");
			this.controlerIO.createIterationDirectory(iteration);
			resetRandomNumbers(rndSeed, iteration);

			this.controlerListenerManager.fireControlerIterationStartsEvent(iteration);
			if (iteration > firstIteration) {
				this.stopwatch.beginOperation("replanning");
				this.controlerListenerManager.fireControlerReplanningEvent(iteration);
				this.stopwatch.endOperation("replanning");
			}
			this.controlerListenerManager.fireControlerBeforeMobsimEvent(iteration);
			this.stopwatch.beginOperation("mobsim");
			resetRandomNumbers(rndSeed, iteration);
			runMobSim(iteration);
			this.stopwatch.endOperation("mobsim");
			log.info(marker + "ITERATION " + iteration + " fires after mobsim event");
			this.controlerListenerManager.fireControlerAfterMobsimEvent(iteration);
			log.info(marker + "ITERATION " + iteration + " fires scoring event");
			this.controlerListenerManager.fireControlerScoringEvent(iteration);
			log.info(marker + "ITERATION " + iteration + " fires iteration end event");
			this.controlerListenerManager.fireControlerIterationEndsEvent(iteration);
			this.stopwatch.endOperation("iteration");
			this.stopwatch.write(this.controlerIO.getOutputFilename("stopwatch.txt"));
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
	protected final void checkConfigConsistencyAndWriteToLog(Config config,
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


	
	
}
