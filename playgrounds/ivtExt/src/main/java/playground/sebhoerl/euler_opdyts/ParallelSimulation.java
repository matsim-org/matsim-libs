package playground.sebhoerl.euler_opdyts;

import floetteroed.utilities.math.Vector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.qsim.QSim;
import playground.sebhoerl.remote_exec.RemoteSimulation;
import playground.sebhoerl.remote_exec.RemoteUtils;

import java.util.HashMap;
import java.util.Map;

public class ParallelSimulation {
    final private static Logger log = Logger.getLogger(ParallelSimulation.class);

    final private RemoteSimulationFactory factory;
    final private RemoteStateHandler handler;

    final private Map<RemoteDecisionVariable, RemoteSimulation> runningSimulations = new HashMap<>();
    final private Map<RemoteDecisionVariable, Long> previousIterations = new HashMap<>();

    final private long matsimIterationsPerTransition;

    private RemoteDecisionVariable implementedDecisionVariable;
    private RemoteSimulatorState implementedSimulatorState;

    public ParallelSimulation(RemoteSimulationFactory factory, RemoteStateHandler handler, long matsimIterationsPerTransition) {
        this.factory = factory;
        this.handler = handler;
        this.matsimIterationsPerTransition = matsimIterationsPerTransition;
    }

    public void reset() {
        for (RemoteSimulation simulation : runningSimulations.values()) {
            if (RemoteUtils.isActive(simulation)) {
                simulation.stop();
            }

            while (RemoteUtils.isActive(simulation)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }

            // simulation.remove();
            // TODO: If this should be removed, wait until it really is stopped!
            // also, they might be needed later ... somehow track which ones can be deleted
        }

        implementedSimulatorState = new RemoteSimulatorState(this, null, null, 0, new Vector(new double[] { 0.0 }));
        implementedDecisionVariable = null;

        runningSimulations.clear();
        previousIterations.clear();

        log.info("Reset. All warmups stopped and dummy state implemented.");
    }

    private RemoteSimulation getOrCreateSimulation(RemoteDecisionVariable decisionVariable, RemoteSimulatorState state) {
        log.info("Get or create simulation for " + decisionVariable.toString() + " / " + state.toString());

        RemoteSimulation simulation = runningSimulations.get(decisionVariable);

        if (simulation == null) {
            simulation = factory.createSimulation(state, decisionVariable);
            simulation.start();

            runningSimulations.put(decisionVariable, simulation);
            previousIterations.put(decisionVariable, (long) 0);

            log.info("Created simulation " + simulation.getId() + " for decision variable " + decisionVariable.toString());
        }

        return simulation;
    }

    public void implementSimulatorState(RemoteSimulatorState state) {
        this.implementedSimulatorState = state;
        log.info("Implemented state: " + state.toString());
    }

    public void implementDecisionVariable(RemoteDecisionVariable decisionVariable) {
        this.implementedDecisionVariable = decisionVariable;
        log.info("Implemented decision variable: " + decisionVariable.toString());
    }

    private RemoteSimulation getImplementedSimulation() {
        if (implementedDecisionVariable == null) {
            throw new IllegalStateException("No decision variable implemented.");
        }

        if (implementedSimulatorState == null) {
            throw new IllegalStateException("No simulator state implemented.");
        }

        return getOrCreateSimulation(implementedDecisionVariable, implementedSimulatorState);
    }

    public boolean isSampleAvailable(long iteration, RemoteDecisionVariable decisionVariable, RemoteSimulation.Status status) {
        if (status != RemoteSimulation.Status.RUNNING && status != RemoteSimulation.Status.DONE) {
            return false;
        }

        return (iteration - previousIterations.get(decisionVariable) > matsimIterationsPerTransition);
    }

    private RemoteSimulatorState createState(RemoteSimulation simulation, long iteration, RemoteDecisionVariable decisionVariable) {
        handler.reset((int) iteration);

        EventsManager events = new EventsManagerImpl();
        events.addHandler(handler);

        Logger.getLogger(EventsManagerImpl.class).setLevel(Level.OFF);
        simulation.getEvents(events, (int) iteration);

        RemoteSimulatorState state = new RemoteSimulatorState(this, simulation, decisionVariable, iteration, handler.getState());
        log.info("Created state " + state.toString());

        return state;
    }

    public RemoteSimulatorState sampleNextState() {
        log.info("Start sampling next state");

        long interval = 10000;
        long nextTime = System.currentTimeMillis() + interval;

        RemoteSimulation simulation = getImplementedSimulation();
        RemoteSimulatorState state = implementedSimulatorState;
        RemoteDecisionVariable decisionVariable = implementedDecisionVariable;

        RemoteSimulation.Status status;
        long iteration;

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                reset();
            }

            status = simulation.getStatus();
            iteration = simulation.getIteration();

            if (System.currentTimeMillis() >= nextTime) {
                nextTime += interval;

                StringBuilder builder = new StringBuilder();

                for (RemoteSimulation logSimulation : runningSimulations.values()) {
                    builder.append(logSimulation.getId());
                    builder.append("@");
                    builder.append(logSimulation.getIteration());
                    builder.append(" [");
                    builder.append(logSimulation.getStatus());
                    builder.append("], ");
                }

                log.info("Running: " + simulation.getId() + "@" + iteration + " from " + state.toString() + " at " + iteration + " [" + status.toString() + "]; Running: " + builder.toString());
            }
        } while(!isSampleAvailable(iteration, implementedDecisionVariable, status) && !RemoteUtils.isFinished(status));

        log.info("Stopped: " + simulation.getId() + " at " + iteration + " [" + status.toString() + "]");

        if (status == RemoteSimulation.Status.ERROR) {
            throw new RuntimeException("Error in simulation " + simulation.getId());
        }

        if (status != RemoteSimulation.Status.RUNNING && status != RemoteSimulation.Status.DONE) {
            throw new RuntimeException("Premature exit of simulation " + simulation.getId());
        }

        long sampleIteration = previousIterations.get(decisionVariable) + matsimIterationsPerTransition;
        previousIterations.put(decisionVariable, sampleIteration);

        log.info("Sampling iteration " + sampleIteration + " of simulation " + simulation.getId());
        return createState(simulation, sampleIteration, decisionVariable);
    }
}
