package org.matsim.core.scoring.functions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IndividualPersonScoringParametersTest {

	private Scenario scenario;
	private Config config;
	private Population population;
	private ScoringConfigGroup scoringConfig;
	private IndividualPersonScoringParameters scoringParametersForPerson;

	@BeforeEach
	public void setUp() {
		config = ConfigUtils.createConfig();
		scoringConfig = config.scoring();

		addDefaultParams(null);

		// Create scenario and population
		scenario = ScenarioUtils.createScenario(config);
		population = scenario.getPopulation();

		// Create the scoring parameters provider
		scoringParametersForPerson = new IndividualPersonScoringParameters(scenario);
	}

	private void addDefaultParams(String subpopulation) {
		// Create a default scoring parameter set
		ScoringConfigGroup.ScoringParameterSet defaultParams = scoringConfig.getOrCreateScoringParameters(subpopulation);
		defaultParams.setMarginalUtilityOfMoney(1.0);

		// Add mode parameters
		defaultParams.addModeParams(new ScoringConfigGroup.ModeParams("car")
			.setConstant(-1.0)
			.setMarginalUtilityOfTraveling(-6.0)
			.setMonetaryDistanceRate(-0.0002));

		defaultParams.addModeParams(new ScoringConfigGroup.ModeParams("pt")
			.setConstant(-0.4)
			.setMarginalUtilityOfTraveling(-4.0)
			.setMonetaryDistanceRate(-0.0001));

		// Add activity parameters
		ScoringConfigGroup.ActivityParams homeParams = new ScoringConfigGroup.ActivityParams("home");
		homeParams.setTypicalDuration(12 * 3600);
		defaultParams.addActivityParams(homeParams);
	}

	@Test
	public void testDefaultScoringParameters() {
		// Create a person with no special attributes
		Person person = PopulationUtils.getFactory().createPerson(Id.create("person1", Person.class));
		population.addPerson(person);

		// Get scoring parameters
		ScoringParameters params = scoringParametersForPerson.getScoringParameters(person);

		// Verify default parameters are used
		assertThat(params.marginalUtilityOfMoney).isEqualTo(1.0, within(1e-10));
		assertThat(params.modeParams.get("car").constant).isEqualTo(-1.0, within(1e-10));
		assertThat(params.modeParams.get("car").marginalUtilityOfTraveling_s).isEqualTo(-6.0 / 3600, within(1e-10));
	}

	@Test
	public void testIncomeBasedUtilityOfMoney() {
		// Enable income-based utility
		TasteVariationsConfigParameterSet tasteParams = new TasteVariationsConfigParameterSet();
		tasteParams.setIncomeExponent(0.5);
		scoringConfig.getScoringParameters(null).setTasteVariationsParams(tasteParams);

		// Create a few persons with different incomes
		Person person1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person1"));
		PersonUtils.setIncome(person1, 1000.0); // Below average
		population.addPerson(person1);

		Person person2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person2"));
		PersonUtils.setIncome(person2, 2000.0); // Average income
		population.addPerson(person2);

		Person person3 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person3"));
		PersonUtils.setIncome(person3, 3000.0); // Above average
		population.addPerson(person3);

		// Get scoring parameters for each person
		ScoringParameters params1 = scoringParametersForPerson.getScoringParameters(person1);
		ScoringParameters params2 = scoringParametersForPerson.getScoringParameters(person2);
		ScoringParameters params3 = scoringParametersForPerson.getScoringParameters(person3);

		// For person1 (lower income), marginal utility of money should be higher
		// For person3 (higher income), marginal utility of money should be lower
		assertThat(params1.marginalUtilityOfMoney).isGreaterThan(params2.marginalUtilityOfMoney);
		assertThat(params2.marginalUtilityOfMoney).isGreaterThan(params3.marginalUtilityOfMoney);

		// With income exponent 0.5, the formula is: baseValue * (avgIncome/personalIncome)^0.5
		// Assuming avg income of 2000, utility for person1 should be 1.0 * sqrt(2000/1000) = 1.414
		assertThat(params1.marginalUtilityOfMoney).isEqualTo(Math.sqrt(2000.0 / 1000.0), within(1e-3));

		// For person3: 1.0 * sqrt(3000/4000) = 0.707
		assertThat(params3.marginalUtilityOfMoney).isEqualTo(Math.sqrt(2000.0 / 3000.0), within(1e-3));
	}

	@Test
	public void testModeTasteVariations() {
		// Enable taste variations
		TasteVariationsConfigParameterSet tasteParams = new TasteVariationsConfigParameterSet();
		tasteParams.setVariationsOf(Set.of(
			ModeUtilityParameters.Type.constant,
			ModeUtilityParameters.Type.marginalUtilityOfTraveling_s
		));
		scoringConfig.getScoringParameters(null).setTasteVariationsParams(tasteParams);

		// Create a person with mode taste variations
		Person person = PopulationUtils.getFactory().createPerson(Id.create("person1", Person.class));

		// Set taste variations using Map.of
		Map<String, Map<ModeUtilityParameters.Type, Double>> modeTasteVariations = Map.of(
			"car", Map.of(
				ModeUtilityParameters.Type.constant, -0.5, // Make car constant even more negative
				ModeUtilityParameters.Type.marginalUtilityOfTraveling_s, -1.0 // Make travel time more negative
			),
			"pt", Map.of(
				ModeUtilityParameters.Type.constant, 0.2, // Make PT constant less negative
				ModeUtilityParameters.Type.marginalUtilityOfTraveling_s, 1.0 // Make travel time less negative
			)
		);

		PersonUtils.setModeTasteVariations(person, modeTasteVariations);
		population.addPerson(person);

		// Get scoring parameters
		ScoringParameters params = scoringParametersForPerson.getScoringParameters(person);

		// Verify parameters were adjusted correctly
		assertThat(params.modeParams.get("car").constant).isEqualTo(-1.5, within(1e-10)); // -1.0 - 0.5
		assertThat(params.modeParams.get("car").marginalUtilityOfTraveling_s).isEqualTo((-6.0 / 3600) - 1, within(1e-10));

		assertThat(params.modeParams.get("pt").constant).isEqualTo(-0.2, within(1e-10)); // -0.4 + 0.2
		assertThat(params.modeParams.get("pt").marginalUtilityOfTraveling_s).isEqualTo((-4.0 / 3600) + 1, within(1e-10));
	}

	@Test
	public void testCombinedIncomeAndTasteVariations() {
		// Enable both income and taste variations
		TasteVariationsConfigParameterSet tasteParams = new TasteVariationsConfigParameterSet();
		tasteParams.setIncomeExponent(0.5);
		tasteParams.setVariationsOf(Set.of(
			ModeUtilityParameters.Type.constant,
			ModeUtilityParameters.Type.monetaryDistanceCostRate
		));

		scoringConfig.getScoringParameters(null).setTasteVariationsParams(tasteParams);

		// Create a person with both income and taste variations
		Person person = PopulationUtils.getFactory().createPerson(Id.create("person1", Person.class));
		PersonUtils.setIncome(person, 1000.0); // Below average

		Person person2 = PopulationUtils.getFactory().createPerson(Id.create("person2", Person.class));
		PersonUtils.setIncome(person2, 3000.0); // Below average

		// Set taste variations
		Map<String, Map<ModeUtilityParameters.Type, Double>> modeTasteVariations = new HashMap<>();
		Map<ModeUtilityParameters.Type, Double> carVariations = new EnumMap<>(ModeUtilityParameters.Type.class);
		carVariations.put(ModeUtilityParameters.Type.constant, -0.5);
		carVariations.put(ModeUtilityParameters.Type.monetaryDistanceCostRate, -0.0001);
		modeTasteVariations.put("car", carVariations);

		PersonUtils.setModeTasteVariations(person, modeTasteVariations);
		population.addPerson(person);
		population.addPerson(person2);

		// Get scoring parameters
		ScoringParameters params = scoringParametersForPerson.getScoringParameters(person);

		// Verify both adjustments were applied
		assertThat(params.marginalUtilityOfMoney).isEqualTo(Math.sqrt(2000.0 / 1000.0), within(1e-3));
		assertThat(params.modeParams.get("car").constant).isEqualTo(-1.5, within(1e-10));
		assertThat(params.modeParams.get("car").monetaryDistanceCostRate).isEqualTo(-0.0003, within(1e-10));
	}

	@Test
	public void testPersonWithoutTasteVariationsWhenRequired() {
		// Enable taste variations requirement
		TasteVariationsConfigParameterSet tasteParams = new TasteVariationsConfigParameterSet();
		tasteParams.setVariationsOf(Set.of(ModeUtilityParameters.Type.constant));
		scoringConfig.getScoringParameters(null).setTasteVariationsParams(tasteParams);

		// Create a person without taste variations
		Person person = PopulationUtils.getFactory().createPerson(Id.create("person1", Person.class));
		population.addPerson(person);

		// This should throw an exception since variations are required but not provided
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			scoringParametersForPerson.getScoringParameters(person);
		});

		assertThat(exception.getMessage()).contains("does not have any mode taste variations");
	}

	@Test
	public void testCaching() {
		// Create a person
		Person person = PopulationUtils.getFactory().createPerson(Id.create("person1", Person.class));
		population.addPerson(person);

		// Get scoring parameters twice
		ScoringParameters params1 = scoringParametersForPerson.getScoringParameters(person);
		ScoringParameters params2 = scoringParametersForPerson.getScoringParameters(person);

		// Verify the same instance is returned (caching works)
		assertThat(params2).isSameAs(params1);
	}

	@Test
	public void testSubpopulationSpecificParameters() {
		// Create a subpopulation-specific scoring parameter set
		ScoringConfigGroup.ScoringParameterSet subpopParams = scoringConfig.getOrCreateScoringParameters("workers");
		subpopParams.setMarginalUtilityOfMoney(2.0);
		subpopParams.setMarginalUtlOfWaitingPt_utils_hr(-1.0);

		// Add subpopulation-specific mode parameters
		ScoringConfigGroup.ModeParams workerCarParams = new ScoringConfigGroup.ModeParams("car");
		workerCarParams.setConstant(-2.0);
		workerCarParams.setMarginalUtilityOfTraveling(-7.0);
		subpopParams.addModeParams(workerCarParams);

		subpopParams.addActivityParams(new ScoringConfigGroup.ActivityParams("work").setTypicalDuration(8 * 3600));
		scoringConfig.addParameterSet(subpopParams);

		// Create persons from different subpopulations
		Person defaultPerson = PopulationUtils.getFactory().createPerson(Id.create("default", Person.class));
		PopulationUtils.putSubpopulation(defaultPerson, "default");
		population.addPerson(defaultPerson);

		Person workerPerson = PopulationUtils.getFactory().createPerson(Id.create("worker", Person.class));
		PopulationUtils.putSubpopulation(workerPerson, "workers");
		population.addPerson(workerPerson);

		addDefaultParams("default");

		// Get scoring parameters
		ScoringParameters defaultParams = scoringParametersForPerson.getScoringParameters(defaultPerson);
		ScoringParameters workerParams = scoringParametersForPerson.getScoringParameters(workerPerson);

		// Verify subpopulation-specific parameters are used
		assertThat(defaultParams.marginalUtilityOfMoney).isEqualTo(1.0, within(1e-10));
		assertThat(workerParams.marginalUtilityOfMoney).isEqualTo(2.0, within(1e-10));

		assertThat(defaultParams.modeParams.get("car").constant).isEqualTo(-1.0, within(1e-10));
		assertThat(workerParams.modeParams.get("car").constant).isEqualTo(-2.0, within(1e-10));
	}
}
