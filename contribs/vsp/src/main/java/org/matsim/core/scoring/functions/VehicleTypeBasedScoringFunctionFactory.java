package org.matsim.core.scoring.functions;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Map;

/**
 * Standard Charypar-Nagel scoring, but with a leg scoring component that can resolve
 * scoring parameters via vehicle type instead of only the leg mode.
 */
public class VehicleTypeBasedScoringFunctionFactory implements ScoringFunctionFactory {

	private final Scenario scenario;
	private final ScoringParametersForPerson params;

	@Inject
	VehicleTypeBasedScoringFunctionFactory(Scenario scenario) {
		this.scenario = scenario;
		this.params = new SubpopulationScoringParameters(scenario);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		ScoringParameters parameters = params.getScoringParameters(person);
		Map<String, Id<VehicleType>> vehicleTypes = VehicleUtils.getVehicleTypes(person);
		double personSpecificMarginalUtilityOfTime;

		// for the commercial agents the specific vehicle of this person is set. The time costs of this vehicle are the time costs of the person.
		// That's why we are using these costs in combination with the marginalUtilityOfMoney are used for the performing and waiting costs
		if (vehicleTypes == null || vehicleTypes.size() != 1) {
			throw new IllegalArgumentException("Person has multiple or no vehicle types and in the current implementation, this is not supported. Person: " + person.getId() + ", vehicle types: " + vehicleTypes);
		}
		else {
			personSpecificMarginalUtilityOfTime = (-1) * scenario.getVehicles().getVehicleTypes().get(vehicleTypes.values().iterator().next()).getCostInformation().getCostsPerSecond() * parameters.marginalUtilityOfMoney;
		}
		var scoringParameterSet = scenario.getConfig().scoring().getScoringParametersOrDefault(PopulationUtils.getSubpopulation(person));

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new ActivityScoringForCommercialActivities(parameters, personSpecificMarginalUtilityOfTime));
		sumScoringFunction.addScoringFunction(new VehicleTypeBasedLegScoring(scenario.getVehicles(), parameters, scoringParameterSet, scenario.getConfig().transit().getTransitModes()));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));
		sumScoringFunction.addScoringFunction(new ScoreEventScoring());
		return sumScoringFunction;
	}
}
