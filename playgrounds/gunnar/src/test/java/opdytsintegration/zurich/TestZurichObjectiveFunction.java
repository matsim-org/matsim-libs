package opdytsintegration.zurich;

import optdyts.ObjectiveFunction;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class TestZurichObjectiveFunction implements
		ObjectiveFunction<TestZurichState> {

	@Override
	public double evaluateState(final TestZurichState state) {
		final double avgIncome = (1.0 - state.getBetaPay()) * 100.0;
		return ((-1.0) * (state.getAvgScore() + avgIncome));
	}

}
