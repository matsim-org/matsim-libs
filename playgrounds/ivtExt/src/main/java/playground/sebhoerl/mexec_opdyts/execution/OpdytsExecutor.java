package playground.sebhoerl.mexec_opdyts.execution;

import org.apache.log4j.Logger;
import playground.sebhoerl.mexec.Controller;
import playground.sebhoerl.mexec.Environment;
import playground.sebhoerl.mexec.Scenario;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec_opdyts.optimization.InitialState;
import playground.sebhoerl.mexec_opdyts.optimization.IterationState;
import playground.sebhoerl.mexec_opdyts.optimization.Proposal;

import java.util.*;

public class OpdytsExecutor {
    private Logger logger = Logger.getLogger(OpdytsExecutor.class);

    final private Environment environment;
    final private Scenario scenario;
    final private Controller controller;

    private Collection<SimulationRun> formerSimulationRuns = new LinkedList<>();
    private Collection<SimulationRun> activeSimulationRuns = new LinkedList<>();

    private long simulationIndex = 0;
    private long sampleIterations;

    private IterationState implementedState = null;
    private Proposal implementedProposal = null;

    private IterationState baseState = null;
    private boolean awaitingInitialSampling = true;

    private String simulationPrefix;

    public OpdytsExecutor(Environment environment, Controller controller, Scenario scenario, long sampleIterations, String simulationPrefix) {
        this.environment = environment;
        this.scenario = scenario;
        this.controller = controller;
        this.sampleIterations = sampleIterations;
        this.simulationPrefix = simulationPrefix;

        Set<Simulation> remove = new HashSet<>();

        for (Simulation simulation : environment.getSimulations()) {
            if (simulation.getId().startsWith(simulationPrefix)) { // TODO: Make the prefix configurable!
                remove.add(simulation);
            }
        }

        for (Simulation simulation : remove) {
            if (simulation.isActive()) simulation.stop();
            environment.removeSimulation(simulation);
        }
    }

    public void initializeSimulations(IterationState baseState) {
        logger.info("Base state initialized to " + baseState);

        for (SimulationRun run : activeSimulationRuns) {
            run.getSimulation().stop();
        }

        for (SimulationRun run : formerSimulationRuns) {
            environment.removeSimulation(run.getSimulation());
        }

        formerSimulationRuns.clear();
        formerSimulationRuns.addAll(activeSimulationRuns);

        activeSimulationRuns.clear();
        awaitingInitialSampling = true;

        implementedProposal = null;
        implementedState = null;
        this.baseState = baseState;
    }

    private SimulationRun createSimulationRun(IterationState parent) {
        Simulation simulation = environment.createSimulation(simulationPrefix + String.valueOf(simulationIndex++), scenario, controller);

        SimulationRun run = new SimulationRun(simulation, parent, sampleIterations);
        activeSimulationRuns.add(run);

        return run;
    }

    public void implementProposal(Proposal proposal) {
        logger.info("Implementing " + proposal);
        this.implementedProposal = proposal;
    }

    public void implementState(IterationState state) {
        logger.info("Implementing " + state);
        if (!(state instanceof InitialState || state.equals(baseState))) {
            this.implementedState = state;
        }
    }

    public IterationState sampleIteration() {
        if (awaitingInitialSampling) {
            logger.info("Sampling Initial State ...");
            awaitingInitialSampling = false;
            return new InitialState();
        }

        logger.info("Sampling ...");
        logger.info("    State: " + implementedState);
        logger.info("    Proposal: " + implementedProposal);

        SimulationRun simulationRun;

        if (implementedState == null) {
            if (implementedProposal == null) {
                throw new IllegalStateException("New simulation requested by no proposal implemented");
            }

            simulationRun = createSimulationRun(baseState);
            simulationRun.implementProposal(implementedProposal);

            logger.info("Created new simulation " + simulationRun);
            logger.info("    State: " + baseState);
            logger.info("    Proposal: " + implementedProposal);
        } else {
            simulationRun = implementedState.getSimulationRun();
        }

        if (!simulationRun.getProposal().equals(implementedProposal)) {
            throw new IllegalStateException("Mismatch between implemented simulation and proposal");
        }

        List<IterationState> iterations = simulationRun.getIterations();
        Simulation simulation = simulationRun.getSimulation();

        long nextIteration = 0;

        if (iterations.size() > 0) {
            IterationState current = iterations.get(iterations.size() - 1);
            nextIteration = current.getIteration() + sampleIterations;
        } else {
            simulation.start();
        }

        logger.info("Sampling iteration " + nextIteration + " from " + simulationRun);

        while (simulation.getIteration() == null || simulation.getIteration() <= nextIteration) {
            if (!simulation.isActive()) {
                throw new RuntimeException("Simulation died unexpectedly. " + simulationRun);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }

        IterationState newState = new IterationState(this, simulationRun, nextIteration);
        simulationRun.addIteration(newState);

        implementedProposal = null;
        implementedState = null;

        return newState;
    }
}
