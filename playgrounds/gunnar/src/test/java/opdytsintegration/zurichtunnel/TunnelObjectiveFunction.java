package opdytsintegration.zurichtunnel;

import optdyts.ObjectiveFunction;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class TunnelObjectiveFunction implements ObjectiveFunction<TunnelState> {

	// private final double betaPayScale = 0.04;

	@Override
	public double evaluateState(final TunnelState state) {
		// final double avgIncome = (1.0 - this.betaPayScale *
		// state.getBetaPay()) * 100.0;
		// return ((-1.0) * (state.getAvgScore() + avgIncome));
		return (-1.0) * state.getAvgScore();
	}

}
