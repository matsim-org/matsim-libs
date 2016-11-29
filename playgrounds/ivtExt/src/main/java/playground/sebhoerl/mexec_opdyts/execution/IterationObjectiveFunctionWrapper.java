package playground.sebhoerl.mexec_opdyts.execution;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import playground.sebhoerl.mexec_opdyts.optimization.IterationObjectiveFunction;
import playground.sebhoerl.mexec_opdyts.optimization.IterationState;

public class IterationObjectiveFunctionWrapper implements ObjectiveFunction {
    final private IterationObjectiveFunction objectiveFunction;

    public IterationObjectiveFunctionWrapper(IterationObjectiveFunction objectiveFunction) {
        this.objectiveFunction = objectiveFunction;
    }

    @Override
    public double value(SimulatorState simulatorState) {
        if (!(simulatorState instanceof IterationState)) {
            throw new IllegalStateException();
        }

        IterationState iterationState = (IterationState) simulatorState;
        return objectiveFunction.compute(iterationState.getSimulationRun().getSimulation(), iterationState);
    }
}
