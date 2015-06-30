package tworoutes;

import optdyts.ObjectiveFunction;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TwoRoutesObjectiveFunction implements
		ObjectiveFunction<TwoRoutesSimulatorState> {

	private final double vot = 1.0;
	
	private final double damage1;

	TwoRoutesObjectiveFunction(final double damage1) {
		this.damage1 = damage1;
	}

	@Override
	public double evaluateState(final TwoRoutesSimulatorState state) {
		return -(this.vot * state.getTotalTT() + (this.damage1 + state
				.getToll1()) * state.getFlow1());
	}

}
