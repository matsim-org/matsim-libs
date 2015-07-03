package tworoutes;

import java.util.Set;

import optdyts.logging.AbstractDecisionVariableAverage;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TwoRoutesAverageToll
		extends
		AbstractDecisionVariableAverage<TwoRoutesSimulatorState, TwoRoutesDecisionVariable> {

	public TwoRoutesAverageToll(
			final Set<TwoRoutesDecisionVariable> allDecisionVariables) {
		super(allDecisionVariables, "\t");
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
