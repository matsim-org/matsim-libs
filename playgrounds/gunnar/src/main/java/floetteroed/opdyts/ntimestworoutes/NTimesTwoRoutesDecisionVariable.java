package floetteroed.opdyts.ntimestworoutes;

import java.util.List;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class NTimesTwoRoutesDecisionVariable implements DecisionVariable {

	private final NTimesTwoRoutesSimulator system;

	private final List<Double> tolls;

	NTimesTwoRoutesDecisionVariable(final NTimesTwoRoutesSimulator system,
			final Vector tolls) {
		this.system = system;
		this.tolls = tolls.copy().asList();
	}

	Vector getTolls() {
		return new Vector(this.tolls);
	}

	@Override
	public void implementInSimulation() {
		this.system.implementTollsInSimulation(new Vector(this.tolls));
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (Double val : this.tolls) {
			result.append(MathHelpers.round(val, 2));
			result.append(", ");
		}
		return result.toString();
	}

	// TODO NEW
	
	@Override
	public int hashCode() {
		return this.tolls.hashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof NTimesTwoRoutesDecisionVariable) {
			return this.tolls
					.equals(((NTimesTwoRoutesDecisionVariable) other).tolls);
		} else {
			return false;
		}
	}
}
