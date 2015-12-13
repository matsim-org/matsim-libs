package gunnar.ihop2.roadpricing;

import opdytsintegration.MATSimState;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TotalScoreObjectiveFunction implements ObjectiveFunction {

	TotalScoreObjectiveFunction() {
	}

	@Override
	public double value(SimulatorState state) {
		final MATSimState matsimState = (MATSimState) state;
		double result = 0;
		for (Id<Person> personId : matsimState.getPersonIdView()) {
			result -= matsimState.getSelectedPlan(personId).getScore();
		}
		return result;
	}

}
