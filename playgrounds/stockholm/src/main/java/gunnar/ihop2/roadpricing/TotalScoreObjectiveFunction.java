package gunnar.ihop2.roadpricing;

import opdytsintegration.MATSimState;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * Returns the negative sum of the scores of the selected plans of all agents.
 * 
 * @author Gunnar Flötteröd
 *
 */
class TotalScoreObjectiveFunction implements ObjectiveFunction {

	@Override
	public double value(final SimulatorState state) {
		final MATSimState matsimState = (MATSimState) state;
		double result = 0;
		for (Id<Person> personId : matsimState.getPersonIdView()) {
			result -= matsimState.getSelectedPlan(personId).getScore();
		}
		return result;
	}

}
