package floetteroed.opdyts.convergencecriteria;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ConvergenceCriterionResult {

	public final boolean converged;

	public final Double finalObjectiveFunctionValue;

	public final Double finalObjectiveFunctionValueStddev;

	public final Double finalEquilibriumGap;

	public final Double finalUniformityGap;

	public final Object lastDecisionVariable;

	public final Integer lastTransitionSequenceLength;

	public final Double finalSquareTransitionLength;

	public ConvergenceCriterionResult(final boolean converged,
			final Double finalObjectiveFunctionValue,
			final Double finalObjectiveFunctionValueStddev,
			final Double finalEquilibiriumGap, final Double finalUniformityGap,
			final Object lastDecisionVariable,
			final Integer lastTransitionSequenceLength,
			final Double finalSquareTransitionLength) {
		this.converged = converged;
		this.finalObjectiveFunctionValue = finalObjectiveFunctionValue;
		this.finalObjectiveFunctionValueStddev = finalObjectiveFunctionValueStddev;
		this.finalEquilibriumGap = finalEquilibiriumGap;
		this.finalUniformityGap = finalUniformityGap;
		this.lastDecisionVariable = lastDecisionVariable;
		this.lastTransitionSequenceLength = lastTransitionSequenceLength;
		this.finalSquareTransitionLength = finalSquareTransitionLength;
	}
}
