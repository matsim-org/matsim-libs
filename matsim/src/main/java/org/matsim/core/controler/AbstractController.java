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

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractController {
	private static boolean dirtyShutdown = false ;
	
	private static class InterruptAndJoin extends Thread {
        private final Thread controllerThread;
        private AtomicBoolean unexpectedShutdown;

        public InterruptAndJoin(Thread controllerThread, AtomicBoolean unexpectedShutdown) {
            this.controllerThread = controllerThread;
            this.unexpectedShutdown = unexpectedShutdown;
        }

        @Override
        public void run() {
            log.error("received unexpected shutdown request.");
            unexpectedShutdown.set(true);
            log.info("sending innterrupt request to controllerThread");
            controllerThread.interrupt();
            
            if (!dirtyShutdown) {
                // for me, the following code just makes the shutdown hang indefinitely (on mac, tested mostly with otfvis).
            	// Therefore, now providing a switch.  kai, mar'15
            	try {
            		// Wait until it has shut down.
            		log.info("waiting for controllerThread to terminate") ;
            		controllerThread.join();
            		log.info("controllerThread terminated") ;
            	} catch (InterruptedException e) {
            		throw new RuntimeException(e);
            	}
            	// The JVM will exit when this method returns.
            }
        }
    }

    class UnexpectedShutdownException extends Exception {
    }

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

    /**
     * This is deliberately not even protected.  kai, jul'12
     */
    ControlerListenerManager controlerListenerManager;

    // for tests
    protected volatile Throwable uncaughtException;

    private AtomicBoolean unexpectedShutdown = new AtomicBoolean(false);

    private Integer thisIteration = null;


    protected AbstractController() {
        OutputDirectoryLogging.catchLogEntries();
        Gbl.printSystemInfo();
        Gbl.printBuildInfo();
        log.info("Used Controler-Class: " + this.getClass().getCanonicalName());
        this.controlerListenerManager = new ControlerListenerManager();
    }


    private void resetRandomNumbers(long seed, int iteration) {
        MatsimRandom.reset(seed + iteration);
        MatsimRandom.getRandom().nextDouble(); // draw one because of strange
        // "not-randomness" is the first
        // draw...
        // Fixme [kn] this should really be ten thousand draws instead of just
        // one
    }

    protected final void setupOutputDirectory(final String outputDirectory, String runId, final OverwriteFileSetting overwriteFiles) {
        this.controlerIO = new OutputDirectoryHierarchy(outputDirectory, runId, overwriteFiles); // output dir needs to be there before logging
        OutputDirectoryLogging.initLogging(this.getControlerIO()); // logging needs to be early
    }

    protected final void run(Config config) {
        UncaughtExceptionHandler previousDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        final Thread controllerThread = Thread.currentThread();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                // We want to shut down when any Thread dies with an Exception.
                logMemorizeAndRequestShutdown(t, e);
                controllerThread.interrupt();
            }
        });
        Thread shutdownHook = new InterruptAndJoin(controllerThread, unexpectedShutdown);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            loadCoreListeners();
            this.controlerListenerManager.fireControlerStartupEvent();
            ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "config dump before iterations start");
            prepareForSim();
            doIterations(config);
        } catch (UnexpectedShutdownException e) {
            // Doesn't matter. Just shut down.
        } catch (Throwable e) {
            // Don't let it fall through to the UncaughtExceptionHandler. We want to first log the Exception,
            // then shut down.
            logMemorizeAndRequestShutdown(Thread.currentThread(), e);
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // Doesn't matter. Only means that we are already shutting down.
                // But if we are not, this is neccessary because otherwise the
                // shutdown hook will try to interrupt and join this thread later where
                // it may already be used for something else.
                // I still think all this JVM-stuff should go somewhere outside,
                // where it is only used when e.g. run from the command line,
                // and not e.g. from tests or scripts, where multiple
                // instances of Controler can be run.
            }
            shutdown();
            Thread.setDefaultUncaughtExceptionHandler(previousDefaultUncaughtExceptionHandler);
            // Propagate Exception in case Controler.run is called by someone who wants to catch
            // it. It is probably not strictly correct to wrap the exception here.
            // But otherwise, this method would have to declare "throws Throwable".
            // In theory, a run method for test cases would probably need to
            // be different from the run method of the "MATSim platform" which
            // takes control of the JVM by installing hooks and exception
            // handlers.
            if (uncaughtException != null) {
                throw new RuntimeException(uncaughtException);
            }
        }
    }

    private void logMemorizeAndRequestShutdown(Thread t, Throwable e) {
        log.error("Getting uncaught Exception in Thread " + t.getName(), e);
        uncaughtException = e;
        unexpectedShutdown.set(true);
    }

    protected abstract void loadCoreListeners();

    protected abstract void runMobSim();

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

    private void doIterations(Config config) throws UnexpectedShutdownException {
        for (int iteration = config.controler().getFirstIteration(); continueIterations(iteration); iteration++) {
            iteration(config, iteration);
        }
    }

    private void shutdown() {
        log.info("S H U T D O W N   ---   start shutdown.");
        if (this.unexpectedShutdown.get()) {
        	log.error("ERROR --- This is an unexpected shutdown!");
        }
        if (this.uncaughtException != null) {
        	log.error("Shutdown possibly caused by the following Exception:", this.uncaughtException);
        }
        try {
            this.controlerListenerManager.fireControlerShutdownEvent(unexpectedShutdown.get());
        } catch (Exception e) {
            unexpectedShutdown.set(true);
            log.error("Exception during shutdown:", e);
            if (this.uncaughtException == null) {
                // If there has been a previous exception, it is likely more important
                // than this new one. This new one may just be a consequence of the
                // previous one.
                this.uncaughtException = e;
            }
        }
        if (this.unexpectedShutdown.get()) {
            log.error("ERROR --- MATSim unexpectedly terminated. Please check the output or the logfile with warnings and errors for hints.");
            log.error("ERROR --- results should not be used for further analysis.");
        }
        log.info("S H U T D O W N   ---   shutdown completed.");
        if (this.unexpectedShutdown.get()) {
        	log.error("ERROR --- This was an unexpected shutdown! See the log file for a possible reason.");
        }
        OutputDirectoryLogging.closeOutputDirLogging();
    }

    final String DIVIDER = "###################################################";
    final String MARKER = "### ";

    private void iteration(final Config config, final int iteration) throws UnexpectedShutdownException {
        this.thisIteration = iteration;
        this.stopwatch.beginIteration(iteration);

        log.info(DIVIDER);
        log.info(MARKER + "ITERATION " + iteration + " BEGINS");
        this.getControlerIO().createIterationDirectory(iteration);
        resetRandomNumbers(config.global().getRandomSeed(), iteration);

        iterationStep("iterationStartsListeners", new Runnable() {
            @Override
            public void run() {
                controlerListenerManager.fireControlerIterationStartsEvent(iteration);
            }
        });

        if (iteration > config.controler().getFirstIteration()) {
            iterationStep("replanning", new Runnable() {
                @Override
                public void run() {
                    controlerListenerManager.fireControlerReplanningEvent(iteration);
                }
            });
        }

        mobsim(config, iteration);

        iterationStep("scoring", new Runnable() {
            @Override
            public void run() {
                log.info(MARKER + "ITERATION " + iteration + " fires scoring event");
                controlerListenerManager.fireControlerScoringEvent(iteration);
            }
        });

        iterationStep("iterationEndsListeners", new Runnable() {
            @Override
            public void run() {
                log.info(MARKER + "ITERATION " + iteration + " fires iteration end event");
                controlerListenerManager.fireControlerIterationEndsEvent(iteration);
            }
        });

        this.stopwatch.endIteration();
        this.stopwatch.writeTextFile(this.getControlerIO().getOutputFilename("stopwatch"));
        if (config.controler().isCreateGraphs()) {
            this.stopwatch.writeGraphFile(this.getControlerIO().getOutputFilename("stopwatch"));
        }
        log.info(MARKER + "ITERATION " + iteration + " ENDS");
        log.info(DIVIDER);
    }

    private void mobsim(final Config config, final int iteration) throws UnexpectedShutdownException {
        // ControlerListeners may create managed resources in
        // beforeMobsim which need to be cleaned up in afterMobsim.
        // Hence the finally block.
        // For instance, ParallelEventsManagerImpl leaves Threads waiting if we don't do this
        // and an Exception occurs in the Mobsim.
        try {
            iterationStep("beforeMobsimListeners", new Runnable() {
                @Override
                public void run() {
                    controlerListenerManager.fireControlerBeforeMobsimEvent(iteration);
                }
            });

            iterationStep("mobsim", new Runnable() {
                @Override
                public void run() {
                    resetRandomNumbers(config.global().getRandomSeed(), iteration);
                    runMobSim();
                }
            });
        }
        catch ( Throwable t ) {
			// I had problems with an exception being thrown in my MobsimFactory: when the after mobsim
			// listeners were called from the finally block, the finishProcessing() method of the events
			// manager also resulted in an exception (because the mobsim crashed before initProcessing() was
			// ever called), "hidding" the actual source of the problem.
			// To avoid this, we log anything thrown during mobsim before executing after mobsim listeners.
			// td, oct'14
			log.error(  "Mobsim did not complete normally! afterMobsimListeners will be called anyway." , t  );

			// Java 7 seems able to detect which throwables this can be, thus no
			// need to wrap or anything... Nice!
			// If an exception occurs in the finally bloc, this exception will be
			// suppressed, but at least we logged it.
			throw t;
        }
        finally {
            iterationStep("afterMobsimListeners", new Runnable() {
                @Override
                public void run() {
                    log.info(MARKER + "ITERATION " + iteration + " fires after mobsim event");
                    controlerListenerManager.fireControlerAfterMobsimEvent(iteration);
                }
            });
        }
    }

    private void iterationStep(String iterationStepName, Runnable iterationStep) throws UnexpectedShutdownException {
        this.stopwatch.beginOperation(iterationStepName);
        iterationStep.run();
        this.stopwatch.endOperation(iterationStepName);
        if (Thread.interrupted()) {
            throw new UnexpectedShutdownException();
        }
    }


    /**
     * Design comments:<ul>
     * <li> This is such that ControlerListenerManager does not need to be exposed.  One may decide otherwise ...  kai, jul'12
     * </ul>
     */
    public final void addControlerListener(ControlerListener l) {
        this.controlerListenerManager.addControlerListener(l);
    }

    protected final void addCoreControlerListener(ControlerListener l) {
        this.controlerListenerManager.addCoreControlerListener(l);
    }


    public final OutputDirectoryHierarchy getControlerIO() {
        return controlerIO;
    }


	public final Integer getIterationNumber() {
		return this.thisIteration;
	}


	@SuppressWarnings("static-method")
	public final void setDirtyShutdown(boolean dirtyShutdown) {
		AbstractController.dirtyShutdown = dirtyShutdown;
	}


}
