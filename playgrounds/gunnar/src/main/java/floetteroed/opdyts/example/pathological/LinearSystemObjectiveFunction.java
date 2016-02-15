package floetteroed.opdyts.example.pathological;

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
	public double value(final SimulatorState state) {
		final Vector x = ((VectorState) state).getX();
		// final Vector dx = Vector.diff(x, new Vector(10.0, 10.0));
		// final double _Q = dx.innerProd(dx);
		final double _Q = -x.get(0);
		return _Q;
	}
}
