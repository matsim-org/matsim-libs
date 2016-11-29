package playground.sebhoerl.mexec_opdyts.optimization;

import playground.sebhoerl.mexec_opdyts.execution.OpdytsExecutor;
import playground.sebhoerl.mexec_opdyts.execution.SimulationRun;

public class InitialState extends IterationState {
    public InitialState() {
        super(null, null, 0);
    }

    @Override
    public void implementInSimulation() {}

    @Override
    public String toString() {
        return "Null Iteration";
    }
}
