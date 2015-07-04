package tworoutes;

import optdyts.logging.AbstractDecisionVariableAverage;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TwoRoutesAverageToll
		extends
		AbstractDecisionVariableAverage<TwoRoutesSimulatorState, TwoRoutesDecisionVariable> {

	public TwoRoutesAverageToll() {
		super();
	}

	@Override
	public String realValueLabel() {
		return "toll";
	}

	@Override
	public double realValue(final TwoRoutesDecisionVariable decisionVariable) {
		return decisionVariable.getTheta();
	}

}
