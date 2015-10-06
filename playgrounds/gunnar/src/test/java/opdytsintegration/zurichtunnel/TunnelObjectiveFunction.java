package opdytsintegration.zurichtunnel;

import floetteroed.opdyts.ObjectBasedObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class TunnelObjectiveFunction implements ObjectBasedObjectiveFunction {

	// private final double betaPayScale = 0.04;

	@Override
	public double value(SimulatorState state) {
		return (-1.0) * ((TunnelState) state).getAvgScore();
	}

}
