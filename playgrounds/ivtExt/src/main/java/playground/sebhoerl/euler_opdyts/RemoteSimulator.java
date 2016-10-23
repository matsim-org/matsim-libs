package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import playground.sebhoerl.remote_exec.RemoteSimulation;
import playground.sebhoerl.remote_exec.RemoteUtils;

import java.util.Map;

public class RemoteSimulator implements Simulator<RemoteDecisionVariable> {
    final private RemoteSimulationFactory factory;
    final private RemoteStateHandler handler;

    public RemoteSimulator(RemoteSimulationFactory simulationFactory, RemoteStateHandler handler) {
        this.factory = simulationFactory;
        this.handler = handler;
    }

    @Override
    public SimulatorState run(TrajectorySampler<RemoteDecisionVariable> trajectorySampler) {
        return this.run(trajectorySampler, null);
    }

    private RemoteSimulatorState processIteration(RemoteSimulation simulation, TrajectorySampler<RemoteDecisionVariable> trajectorySampler, long iteration) {
        handler.reset((int) iteration);

        EventsManager events = new EventsManagerImpl();
        events.addHandler(handler);

        simulation.getEvents(events, (int) iteration);

        RemoteSimulatorState state = new RemoteSimulatorState(simulation, handler.getState());
        trajectorySampler.afterIteration(state);

        return state;
    }

    @Override
    public SimulatorState run(TrajectorySampler<RemoteDecisionVariable> trajectorySampler, SimulatorState simulatorState) {
        RemoteSimulatorState state = (RemoteSimulatorState) simulatorState;
        RemoteObjectiveFunction objectiveFunction = (RemoteObjectiveFunction) trajectorySampler.getObjectiveFunction();

        trajectorySampler.initialize();

        System.out.println("NEW SIMULATION");

        RemoteSimulation simulation = factory.createSimulation(state == null ? null : state.getSimulation(), trajectorySampler.getCurrentDecisionVariable());
        simulation.start();

        long temporaryIteration;
        long activeIteration = -1;
        long nextTransitionIteration = 0;

        RemoteSimulation.Status status;

        do {
            status = simulation.getStatus();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            temporaryIteration = simulation.getIteration();

            if (temporaryIteration > activeIteration) {
                activeIteration = temporaryIteration;
                System.out.println("MATSIM ITERATION " + activeIteration + "(" + factory.getNumberOfIterationsPerTransition() + " iterations per transition)");
            }

            while (nextTransitionIteration < activeIteration) {
                state = processIteration(simulation, trajectorySampler, nextTransitionIteration);
                nextTransitionIteration += factory.getNumberOfIterationsPerTransition();
            }
        } while(!RemoteUtils.isFinished(status));

        if (simulation.getStatus() == RemoteSimulation.Status.ERROR) {
            throw new RuntimeException("Error in simulation " + simulation.getId());
        }

        activeIteration += 1;
        state = processIteration(simulation, trajectorySampler, nextTransitionIteration);

        // TODO: not sure if this is necessary... only one iteration should be left here
        /*while (nextTransitionIteration < activeIteration) {
            state = processIteration(simulation, trajectorySampler, nextTransitionIteration);
            nextTransitionIteration += factory.getNumberOfIterationsPerTransition();
        }*/

        if (!trajectorySampler.foundSolution()) {
            long transitions = activeIteration / factory.getNumberOfIterationsPerTransition();
            throw new RuntimeException(String.format("TrajectorySampler is not finished. (MATSim iteration: %d, MATSim transitions: %d, Sampler transitions: %d",
                    activeIteration, transitions, trajectorySampler.getTotalTransitionCnt()));
        }

        return state;
    }
}
