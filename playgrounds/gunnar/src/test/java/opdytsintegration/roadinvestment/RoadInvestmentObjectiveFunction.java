package opdytsintegration.roadinvestment;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class RoadInvestmentObjectiveFunction implements
		ObjectiveFunction {

	private final double betaPayScale = 0.04;

	@Override
	public double value(SimulatorState state) {
		RoadInvestmentState roadInvestmentState = (RoadInvestmentState) state;
		final double avgIncome = (1.0 - this.betaPayScale * roadInvestmentState.getBetaPay()) * 100.0;
		return ((-1.0) * (roadInvestmentState.getAvgScore() + avgIncome));
	}

}
