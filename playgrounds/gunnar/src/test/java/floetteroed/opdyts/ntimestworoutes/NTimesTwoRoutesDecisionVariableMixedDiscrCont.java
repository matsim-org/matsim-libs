package floetteroed.opdyts.ntimestworoutes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class NTimesTwoRoutesDecisionVariableMixedDiscrCont implements DecisionVariable {

	private final NTimesTwoRoutesSimulator system;

	private final int linkCnt;

	private final List<Integer> tollIndices;

	private final List<Double> tollValues;

	NTimesTwoRoutesDecisionVariableMixedDiscrCont(
			final NTimesTwoRoutesSimulator system, final int linkCnt,
			final List<Integer> tollIndices, final List<Double> tollValues) {
		this.system = system;
		this.linkCnt = linkCnt;
		this.tollIndices = Collections.unmodifiableList(new ArrayList<Integer>(
				tollIndices));
		this.tollValues = Collections.unmodifiableList(new ArrayList<Double>(
				tollValues));
	}

	List<Integer> getTollIndices() {
		return this.tollIndices;
	}

	List<Double> getTollValues() {
		return this.tollValues;
	}

	int getLinkCnt() {
		return this.linkCnt;
	}

	Vector getTolls() {
		final Vector result = new Vector(this.linkCnt);
		for (int i = 0; i < this.tollIndices.size(); i++) {
			result.set(this.tollIndices.get(i), this.tollValues.get(i));
		}
		return result;
	}

	@Override
	public void implementInSimulation() {
		this.system.implementTollsInSimulation(this.getTolls());
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (Double val : this.getTolls().asList()) {
			result.append(MathHelpers.round(val, 2));
			result.append(", ");
		}
		return result.toString();
	}

	// // TODO NEW
	//
	// @Override
	// public int hashCode() {
	// return this.tollVal.hashCode();
	// }
	//
	// @Override
	// public boolean equals(final Object other) {
	// if (other instanceof NTimesTwoRoutesDecisionVariableMixedDiscrCont) {
	// return this.tollVal
	// .equals(((NTimesTwoRoutesDecisionVariableMixedDiscrCont) other).tollVal);
	// } else {
	// return false;
	// }
	// }
}
