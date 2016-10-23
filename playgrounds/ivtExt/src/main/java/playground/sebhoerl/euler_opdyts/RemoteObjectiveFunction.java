package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

public class RemoteObjectiveFunction implements ObjectiveFunction {
    public RemoteObjectiveFunction() {}

    static final double reference = 0.7;

    @Override
    public double value(SimulatorState simulatorState) {
        double carModeShare = simulatorState.getReferenceToVectorRepresentation().get(0);
        double result = Math.pow(carModeShare - reference, 2.0);

        System.out.println("OBJECTIVE(" + simulatorState.getReferenceToVectorRepresentation().toString() + ") = " + String.valueOf(result));

        return result;
    }
}
