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

public abstract class AbstractController {

    private static Logger log = Logger.getLogger(AbstractController.class);

    private OutputDirectoryHierarchy controlerIO;

    private final IterationStopWatch stopwatch;

    /*
     * Strings used to identify the operations in the IterationStopWatch.
     */
    public static final String OPERATION_ITERATION = "iteration";

    /**
     * This is deliberately not even protected.  kai, jul'12
     */
    ControlerListenerManagerImpl controlerListenerManagerImpl;


    private Integer thisIteration = null;

    private boolean dirtyShutdown = false;

    protected AbstractController() {
        this(new IterationStopWatch(), null);
    }

    AbstractController(IterationStopWatch stopWatch, MatsimServices matsimServices) {
        OutputDirectoryLogging.catchLogEntries();
        Gbl.printSystemInfo();
        Gbl.printBuildInfo();
        log.info("Used Controler-Class: " + this.getClass().getCanonicalName());
        this.controlerListenerManagerImpl = new ControlerListenerManagerImpl();
        this.controlerListenerManagerImpl.setControler(matsimServices);
        this.stopwatch = stopWatch;
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

    final void setupOutputDirectory(OutputDirectoryHierarchy controlerIO) {
        this.controlerIO = controlerIO;
        OutputDirectoryLogging.initLogging(this.getControlerIO()); // logging needs to be early
    }

    protected final void run(final Config config) {
        MatsimRuntimeModifications.MyRunnable runnable = new MatsimRuntimeModifications.MyRunnable() {
            @Override
            public void run() throws MatsimRuntimeModifications.UnexpectedShutdownException {
                loadCoreListeners();
                controlerListenerManagerImpl.fireControlerStartupEvent();
                ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "config dump before iterations start");
                prepareForSim();
                doIterations(config);
            }

            @Override
            public void shutdown(boolean unexpected) {
                controlerListenerManagerImpl.fireControlerShutdownEvent(unexpected);
            }
        };
        MatsimRuntimeModifications.run(runnable);
        OutputDirectoryLogging.closeOutputDirLogging();
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

    private void doIterations(Config config) throws MatsimRuntimeModifications.UnexpectedShutdownException {
        for (int iteration = config.controler().getFirstIteration(); continueIterations(iteration); iteration++) {
            iteration(config, iteration);
        }
    }


    public static final String DIVIDER = "###################################################";
    final String MARKER = "### ";

    private void iteration(final Config config, final int iteration) throws MatsimRuntimeModifications.UnexpectedShutdownException {
        this.thisIteration = iteration;
        this.getStopwatch().beginIteration(iteration);

        log.info(DIVIDER);
        log.info(MARKER + "ITERATION " + iteration + " BEGINS");
        this.getControlerIO().createIterationDirectory(iteration);
        resetRandomNumbers(config.global().getRandomSeed(), iteration);

        iterationStep("iterationStartsListeners", new Runnable() {
            @Override
            public void run() {
                controlerListenerManagerImpl.fireControlerIterationStartsEvent(iteration);
            }
        });

        if (iteration > config.controler().getFirstIteration()) {
            iterationStep("replanning", new Runnable() {
                @Override
                public void run() {
                    controlerListenerManagerImpl.fireControlerReplanningEvent(iteration);
                }
            });
        }

        mobsim(config, iteration);

        iterationStep("scoring", new Runnable() {
            @Override
            public void run() {
                log.info(MARKER + "ITERATION " + iteration + " fires scoring event");
                controlerListenerManagerImpl.fireControlerScoringEvent(iteration);
            }
        });

        iterationStep("iterationEndsListeners", new Runnable() {
            @Override
            public void run() {
                log.info(MARKER + "ITERATION " + iteration + " fires iteration end event");
                controlerListenerManagerImpl.fireControlerIterationEndsEvent(iteration);
            }
        });

        this.getStopwatch().endIteration();
        this.getStopwatch().writeTextFile(this.getControlerIO().getOutputFilename("stopwatch"));
        if (config.controler().isCreateGraphs()) {
            this.getStopwatch().writeGraphFile(this.getControlerIO().getOutputFilename("stopwatch"));
        }
        log.info(MARKER + "ITERATION " + iteration + " ENDS");
        log.info(DIVIDER);
    }

    private void mobsim(final Config config, final int iteration) throws MatsimRuntimeModifications.UnexpectedShutdownException {
        // ControlerListeners may create managed resources in
        // beforeMobsim which need to be cleaned up in afterMobsim.
        // Hence the finally block.
        // For instance, ParallelEventsManagerImpl leaves Threads waiting if we don't do this
        // and an Exception occurs in the Mobsim.
        try {
            iterationStep("beforeMobsimListeners", new Runnable() {
                @Override
                public void run() {
                    controlerListenerManagerImpl.fireControlerBeforeMobsimEvent(iteration);
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
                    controlerListenerManagerImpl.fireControlerAfterMobsimEvent(iteration);
                }
            });
        }
    }

    private void iterationStep(String iterationStepName, Runnable iterationStep) throws MatsimRuntimeModifications.UnexpectedShutdownException {
        this.getStopwatch().beginOperation(iterationStepName);
        iterationStep.run();
        this.getStopwatch().endOperation(iterationStepName);
        if (Thread.interrupted()) {
            throw new MatsimRuntimeModifications.UnexpectedShutdownException();
        }
    }


    /**
     * Design comments:<ul>
     * <li> This is such that ControlerListenerManager does not need to be exposed.  One may decide otherwise ...  kai, jul'12
     * </ul>
     */
    public final void addControlerListener(ControlerListener l) {
        this.controlerListenerManagerImpl.addControlerListener(l);
    }

    protected final void addCoreControlerListener(ControlerListener l) {
        this.controlerListenerManagerImpl.addCoreControlerListener(l);
    }


    public final OutputDirectoryHierarchy getControlerIO() {
        return controlerIO;
    }


	public final Integer getIterationNumber() {
		return this.thisIteration;
	}

    public IterationStopWatch getStopwatch() {
        return stopwatch;
    }

    public void setDirtyShutdown(boolean dirtyShutdown) {
		this.dirtyShutdown = dirtyShutdown;
	}

}
