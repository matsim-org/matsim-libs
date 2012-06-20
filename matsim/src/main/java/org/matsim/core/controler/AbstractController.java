package org.matsim.core.controler;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

public abstract class AbstractController {
	
	private static Logger log = Logger.getLogger(AbstractController.class);

	private Config config;
	
	protected OutputDirectoryHierarchy controlerIO;

	public final IterationStopWatch stopwatch = new IterationStopWatch();
	
	protected ControlerListenerManager controlerListenerManager;
	
	// for tests
	protected volatile Throwable uncaughtException;
	
	private Thread shutdownHook = new Thread() {
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
	

	private void resetRandomNumbers(int iteration) {
		MatsimRandom.reset(config.global().getRandomSeed() + iteration);
		MatsimRandom.getRandom().nextDouble(); // draw one because of strange
		// "not-randomness" is the first
		// draw...
		// Fixme [kn] this should really be ten thousand draws instead of just
		// one
	}
	
	public final void run(Config config) {
		run(config, false);
	}
	
	public final void run(Config config, boolean overwriteFiles, Controler controler) {
		run(config, overwriteFiles);
	}


	public final void run(Config config, boolean overwriteFiles) {
		this.config = config;
		this.controlerIO = new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), overwriteFiles); // output dir needs to be before logging
		OutputDirectoryLogging.initLogging(this.controlerIO); // logging needs to be early
		loadCoreListeners();
		this.controlerListenerManager.fireControlerStartupEvent();
		// make sure all routes are calculated.
		prepareForSim();
		doIterations();
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
	
	private void doIterations() {

		int firstIteration = config.controler().getFirstIteration();
		int lastIteration = config.controler().getLastIteration();
		String divider = "###################################################";
		String marker = "### ";

		for (int iteration = firstIteration; iteration <= lastIteration; iteration++ ) {
			this.stopwatch.setCurrentIteration(iteration) ;

			log.info(divider);
			log.info(marker + "ITERATION " + iteration + " BEGINS");
			this.stopwatch.setCurrentIteration(iteration);
			this.stopwatch.beginOperation("iteration");
			this.controlerIO.createIterationDirectory(iteration);
			resetRandomNumbers(iteration);

			this.controlerListenerManager.fireControlerIterationStartsEvent(iteration);
			if (iteration > firstIteration) {
				this.stopwatch.beginOperation("replanning");
				this.controlerListenerManager.fireControlerReplanningEvent(iteration);
				this.stopwatch.endOperation("replanning");
			}
			this.controlerListenerManager.fireControlerBeforeMobsimEvent(iteration);
			this.stopwatch.beginOperation("mobsim");
			resetRandomNumbers(iteration);
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


	
	
}
