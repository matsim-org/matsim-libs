package floetteroed.opdyts.trajectorysampling;

import java.util.ArrayList;
import java.util.List;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class SingleTrajectoryAnalyzer {

	private final List<Transition<? extends DecisionVariable>> optimalTransitions;

	SingleTrajectoryAnalyzer(
			final List<Transition<? extends DecisionVariable>> transitions,
			final Object optimalDecisionVariable) {
		this.optimalTransitions = new ArrayList<Transition<? extends DecisionVariable>>();
		for (Transition<? extends DecisionVariable> transition : transitions) {
			if (optimalDecisionVariable
					.equals(transition.getDecisionVariable())) {
				this.optimalTransitions.add(transition);
			}
		}
	}

	double surrogateObjectiveFunction(final double equilibriumGapWeight,
			final double uniformityGapWeight) {
		return 0.0;
//		final TransitionSequencesAnalyzer<DecisionVariable> analyzer = new TransitionSequencesAnalyzer<>(
//				this.optimalTransitions, equilibriumGapWeight,
//				uniformityGapWeight);
//		final Vector alphas = analyzer.optimalAlphas();
//		return analyzer.surrogateObjectiveFunctionValue(alphas);
	}

}
