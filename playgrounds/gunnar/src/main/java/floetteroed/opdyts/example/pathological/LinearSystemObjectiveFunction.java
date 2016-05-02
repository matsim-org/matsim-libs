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

	private final Vector coeffs;
	
	public LinearSystemObjectiveFunction(double... coeffs) {
		this.coeffs = new Vector(coeffs);
	}

	@Override
	public double value(final SimulatorState state) {
		final Vector x = ((VectorState) state).getX();
		return coeffs.innerProd(x);
	}
}
