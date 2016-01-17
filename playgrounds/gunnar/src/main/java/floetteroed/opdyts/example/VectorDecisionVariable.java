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

	public Vector getU() {
		return this.u;
	}

	@Override
	public void implementInSimulation() {
		this.system.setDecisionVariable(this.u);
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < this.u.size(); i++) {
			result.append(this.u.get(i));
			if (i < this.u.size() - 1) {
				result.append("\t");
			}
		}
		return result.toString();
	}
}
