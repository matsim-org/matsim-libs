package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

public class RemoteObjectiveFunction implements ObjectiveFunction {
    public RemoteObjectiveFunction() {}

    @Override
    public double value(SimulatorState simulatorState) {
        double carModeShare = simulatorState.getReferenceToVectorRepresentation().get(0);
        return -Math.pow(carModeShare - 0.5, 2.0);
    }
}
