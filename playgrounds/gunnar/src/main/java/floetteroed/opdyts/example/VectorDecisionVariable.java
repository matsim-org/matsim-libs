package floetteroed.opdyts.example;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class VectorDecisionVariable implements DecisionVariable {

	private final Vector u;

	private final LinearSystemSimulator system;

	public VectorDecisionVariable(final Vector u,
			final LinearSystemSimulator system) {
		this.u = u.copy();
		this.system = system;
	}

	public Vector getVector() {
		return this.u;
	}

	@Override
	public void implementInSimulation() {
		this.system.setDecisionVariable(this.u);
	}
}
