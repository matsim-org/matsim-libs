package floetteroed.opdyts.example;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinearSystemObjectiveFunction implements ObjectiveFunction {

	public LinearSystemObjectiveFunction() {
	}

	@Override
	public double value(SimulatorState state) {
		final Vector x = ((VectorState) state).getX();
		final Vector u = ((VectorState) state).getPrevU();
		return -x.get(0) + 0.1 * u.innerProd(u);
	}
}
