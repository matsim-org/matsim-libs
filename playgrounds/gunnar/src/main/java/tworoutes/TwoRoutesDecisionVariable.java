package tworoutes;

import optdyts.DecisionVariable;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TwoRoutesDecisionVariable implements DecisionVariable {

	private final TwoRoutes twoRoutes;

	private final double theta;

	TwoRoutesDecisionVariable(final TwoRoutes twoRoutes, final double theta) {
		this.twoRoutes = twoRoutes;
		this.theta = theta;
	}

	@Override
	public void implementInSimulation() {
		this.twoRoutes.setToll(this.theta);
	}

	@Override
	public String toString() {
		return "toll=" + this.theta;
	}

}
