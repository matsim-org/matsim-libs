package playground.sebhoerl.mexec_opdyts.execution;

import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import playground.sebhoerl.mexec_opdyts.optimization.IterationState;

public class OpdytsSimulator implements Simulator<ProposalDecisionVariable> {
    final private OpdytsExecutor executor;

    public OpdytsSimulator(OpdytsExecutor executor) {
        this.executor = executor;
    }

    @Override
    public SimulatorState run(TrajectorySampler<ProposalDecisionVariable> trajectorySampler) {
        return run(trajectorySampler, null);
    }

    @Override
    public SimulatorState run(TrajectorySampler<ProposalDecisionVariable> trajectorySampler, SimulatorState simulatorState) {
        executor.initializeSimulations((IterationState) simulatorState);
        trajectorySampler.initialize();

        IterationState state = null;

        while (!trajectorySampler.foundSolution()) {
            state = executor.sampleIteration();
            trajectorySampler.afterIteration(state);
        }

        return state;
    }
}
