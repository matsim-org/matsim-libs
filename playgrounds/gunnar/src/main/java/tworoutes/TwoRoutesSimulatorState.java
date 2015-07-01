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

	private final int flow1;

	private final int flow2;

	private final double tt1;

	private final double tt2;

	TwoRoutesSimulatorState(final TwoRoutes twoRoutes, final int flow1,
			final int flow2, final double tt1, final double tt2) {
		this.twoRoutes = twoRoutes;
		this.flow1 = flow1;
		this.flow2 = flow2;
		this.tt1 = tt1;
		this.tt2 = tt2;
	}

	int getFlow1() {
		return this.flow1;
	}

	int getFlow2() {
		return this.flow2;
	}

	double getTT1() {
		return this.tt1;
	}

	double getTT2() {
		return this.tt2;
	}

	@Override
	public Vector getReferenceToVectorRepresentation() {
		return new Vector((double) this.flow1);
	}

	@Override
	public void implementInSimulation() {
		this.twoRoutes.setFlow1(this.flow1);
	}

}
