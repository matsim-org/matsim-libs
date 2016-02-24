package floetteroed.opdyts.trajectorysampling;

import java.util.ArrayList;
import java.util.List;

import floetteroed.opdyts.DecisionVariable;

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
}
