package org.matsim.contrib.parking.parkingproxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationUtilsTest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class InitialLoadGeneratorWithConstantShareTest {

	@RegisterExtension
	MatsimTestUtils testUtils = new MatsimTestUtils();

	@BeforeEach
	void setUp() {
		MatsimRandom.reset();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 10})
	void initialPositionsHalf(int scale) {
		Collection<? extends Person> people = getPeople();

		InitialLoadGeneratorWithConstantShare initialLoadGenerator = new InitialLoadGeneratorWithConstantShare(people, scale, 500);
		Collection<Tuple<Coord, Integer>> tuples = initialLoadGenerator.calculateInitialCarPositions();

		//with the matsim random seed, the result is deterministic 52
		assertEquals(52, tuples.size());
		tuples.stream().map(Tuple::getSecond).forEach(weight -> assertEquals(scale, weight));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 10})
	void initialPositionsFull(int scale) {
		Collection<? extends Person> people = getPeople();

		InitialLoadGeneratorWithConstantShare initialLoadGenerator = new InitialLoadGeneratorWithConstantShare(people, scale, 1000);
		Collection<Tuple<Coord, Integer>> tuples = initialLoadGenerator.calculateInitialCarPositions();

		assertEquals(100, tuples.size());
		tuples.stream().map(Tuple::getSecond).forEach(weight -> assertEquals(scale, weight));
	}

	private static Collection<? extends Person> getPeople() {
		// create population with 100 persons at 0,i
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		for (int i = 0; i < 100; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId(i));
			Plan plan = PopulationUtils.createPlan(person);
			plan.addActivity(PopulationUtils.getFactory().createActivityFromCoord("home", new Coord(0, i)));
			person.addPlan(plan);
			person.setSelectedPlan(plan);
			scenario.getPopulation().addPerson(person);
		}
		return scenario.getPopulation().getPersons().values();
	}
}
