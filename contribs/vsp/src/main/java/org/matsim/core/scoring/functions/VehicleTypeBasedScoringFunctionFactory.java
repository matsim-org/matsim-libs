package org.matsim.core.scoring.functions;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;

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
		VehicleTypeBasedScoringUtils.addVehicleTypeModeParamsToScoringConfig(scenario.getConfig().scoring(), scenario.getVehicles());
		this.params = new SubpopulationScoringParameters(scenario);
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		ScoringParameters parameters = params.getScoringParameters(person);

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new ActivityScoringForCommercialActivities(parameters));
		sumScoringFunction.addScoringFunction(new VehicleTypeBasedLegScoring(scenario.getVehicles(), parameters, scenario.getConfig().transit().getTransitModes()));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));
		sumScoringFunction.addScoringFunction(new ScoreEventScoring());
		return sumScoringFunction;
	}
}
