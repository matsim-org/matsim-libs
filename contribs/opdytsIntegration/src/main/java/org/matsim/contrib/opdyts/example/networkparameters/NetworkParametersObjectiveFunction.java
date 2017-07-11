package org.matsim.contrib.opdyts.example.networkparameters;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class NetworkParametersObjectiveFunction implements ObjectiveFunction {

	// -------------------- CONSTRUCTION --------------------

	NetworkParametersObjectiveFunction() {
	}

	// --------------- IMPLEMENTATION of ObjectiveFunction ---------------

	@Override
	public double value(SimulatorState state) {
		final NetworkParametersState roadpricingState = (NetworkParametersState) state;
		double result = 0.0;
		for (Id<Person> personId : roadpricingState.getPersonIdView()) {
			final Plan selectedPlan = roadpricingState.getSelectedPlan(personId);
			result -= selectedPlan.getScore();
		}
		result /= roadpricingState.getPersonIdView().size();
		return result;
	}

}
