package floetteroed.opdyts.convergencecriteria;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ConvergenceCriterionResult {

	public final Double finalObjectiveFunctionValue;

	public final Double finalObjectiveFunctionValueStddev;

	public final Double finalEquilbiriumGap;

	public final Double finalUniformityGap;

	public final Object lastDecisionVariable;

	public final Integer lastTransitionSequenceLength;

	public ConvergenceCriterionResult(final Double finalObjectiveFunctionValue,
			final Double finalObjectiveFunctionValueStddev,
			final Double finalEquilibiriumGap, final Double finalUniformityGap,
			final Object lastDecisionVariable,
			final Integer lastTransitionSequenceLength) {
		this.finalObjectiveFunctionValue = finalObjectiveFunctionValue;
		this.finalObjectiveFunctionValueStddev = finalObjectiveFunctionValueStddev;
		this.finalEquilbiriumGap = finalEquilibiriumGap;
		this.finalUniformityGap = finalUniformityGap;
		this.lastDecisionVariable = lastDecisionVariable;
		this.lastTransitionSequenceLength = lastTransitionSequenceLength;
	}
}
