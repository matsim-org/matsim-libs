package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;

public class RemoteSimulator implements Simulator<RemoteDecisionVariable> {
    final private ParallelSimulation simulation;

    public RemoteSimulator(ParallelSimulation simulation) {
        this.simulation = simulation;
    }

    @Override
    public SimulatorState run(TrajectorySampler<RemoteDecisionVariable> trajectorySampler) {
        return this.run(trajectorySampler, null);
    }

    @Override
    public SimulatorState run(TrajectorySampler<RemoteDecisionVariable> trajectorySampler, SimulatorState simulatorState) {
        simulation.reset();
        trajectorySampler.initialize();

        if (simulatorState != null) {
            simulatorState.implementInSimulation();
        }

        RemoteSimulatorState state = null;

        while (!trajectorySampler.foundSolution()) {
            state = simulation.sampleNextState();
            trajectorySampler.afterIteration(state);
        }

        return state;
    }
}
