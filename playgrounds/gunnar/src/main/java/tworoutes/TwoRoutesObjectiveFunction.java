package tworoutes;

import optdyts.ObjectiveFunction;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TwoRoutesObjectiveFunction implements
		ObjectiveFunction<TwoRoutesSimulatorState> {

	private final double eta;

	TwoRoutesObjectiveFunction(final double eta) {
		this.eta = eta;
	}

	@Override
	public double evaluateState(final TwoRoutesSimulatorState state) {
		return state.getFlow1() * state.getTT1() + state.getFlow2()
				* state.getTT2() + state.getFlow1() * this.eta;
	}

}
