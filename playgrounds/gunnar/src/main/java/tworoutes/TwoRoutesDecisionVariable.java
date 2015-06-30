package tworoutes;

import optdyts.DecisionVariable;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TwoRoutesDecisionVariable implements DecisionVariable {

	private final TwoRoutes twoRoutes;

	private final double toll1;

	TwoRoutesDecisionVariable(final TwoRoutes twoRoutes, final double toll1) {
		this.twoRoutes = twoRoutes;
		this.toll1 = toll1;
	}

	@Override
	public void implementInSimulation() {
		this.twoRoutes.setToll(this.toll1);
	}
	
	@Override
	public String toString() {
		return "toll=" + this.toll1;
	}

}
