package tworoutes;

import optdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TwoRoutesSimulatorState implements SimulatorState {

	private final TwoRoutes twoRoutes;

	private final double flow1;

	private final double totalTT;

	private final double toll1;

	TwoRoutesSimulatorState(final TwoRoutes twoRoutes, final double flow1,
			final double totalTT, final double toll1) {
		this.twoRoutes = twoRoutes;
		this.flow1 = flow1;
		this.totalTT = totalTT;
		this.toll1 = toll1;
	}

	double getFlow1() {
		return this.flow1;
	}

	double getTotalTT() {
		return this.totalTT;
	}

	double getToll1() {
		return this.toll1;
	}

	@Override
	public Vector getReferenceToVectorRepresentation() {
		return new Vector(this.flow1);
	}

	@Override
	public void implementInSimulation() {
		this.twoRoutes.setFlow1(this.flow1);
	}

}
