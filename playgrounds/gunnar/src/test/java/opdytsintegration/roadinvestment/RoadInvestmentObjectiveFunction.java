package opdytsintegration.roadinvestment;

import optdyts.ObjectiveFunction;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class RoadInvestmentObjectiveFunction implements
		ObjectiveFunction<RoadInvestmentState> {

	private final double betaPayScale = 0.04;
	
	@Override
	public double evaluateState(final RoadInvestmentState state) {
		final double avgIncome = (1.0 - this.betaPayScale * state.getBetaPay()) * 100.0;
		return ((-1.0) * (state.getAvgScore() + avgIncome));
	}

}
