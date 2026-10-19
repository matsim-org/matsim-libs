package org.matsim.contrib.pseudosimulation.distributed.instrumentation.scorestats;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.distributed.scoring.PlanScoreComponent;
import org.matsim.contrib.pseudosimulation.distributed.scoring.ScoreComponentType;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreStatsCharacterizationTest {

	@Test
	void calculatesWorstBestAndExecutedAveragesWhileAverageSlotMirrorsExecuted() {
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		addPerson(population, "one", 1.0, 5.0, 5.0);
		addPerson(population, "two", -2.0, 2.0, -2.0);

		double[] result = new SlaveScoreStatsCalculator().calculateScoreStats(population);

		assertArrayEquals(new double[]{-0.5, 3.5, 1.5, 1.5}, result, 1e-12);
	}

	@Test
	void emptyPopulationProducesNanInEverySlot() {
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());

		double[] result = new SlaveScoreStatsCalculator().calculateScoreStats(population);

		for (double value : result) {
			assertTrue(Double.isNaN(value));
		}
	}

	@Test
	void nullPlanScoresAreIgnored() {
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("null-score"));
		Plan plan = PopulationUtils.createPlan(person);
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		population.addPerson(person);

		double[] result = new SlaveScoreStatsCalculator().calculateScoreStats(population);

		for (double value : result) {
			assertTrue(Double.isNaN(value));
		}
	}

	@Test
	void combinesSlaveHistoryByPopulationWeightAndExposesBackingArray() {
		Config config = ConfigUtils.createConfig();
		config.controller().setFirstIteration(2);
		config.controller().setLastIteration(3);
		SlaveScoreStats stats = new SlaveScoreStats(config);

		stats.insertEntry(2, 1, 4, new double[]{4, 8, 12, 16});
		stats.insertEntry(2, 3, 4, new double[]{8, 12, 16, 20});
		double[][] history = stats.getScoreHistoryAsArray();

		assertEquals(7.0, history[SlaveScoreStats.INDEX_WORST][0]);
		assertEquals(11.0, history[SlaveScoreStats.INDEX_BEST][0]);
		assertEquals(15.0, history[SlaveScoreStats.INDEX_AVERAGE][0]);
		assertEquals(19.0, history[SlaveScoreStats.INDEX_EXECUTED][0]);
		history[0][0] = 99;
		assertEquals(99, stats.getScoreHistoryAsArray()[0][0]);
	}

	@Test
	void mapBasedScoreHistoryApiIsDeliberatelyUnimplemented() {
		RuntimeException error = assertThrows(RuntimeException.class,
				() -> new SlaveScoreStats(ConfigUtils.createConfig()).getScoreHistory());
		assertEquals("not implemented", error.getMessage());
	}

	@Test
	void planScoreComponentFormatsToThreeDecimals() {
		PlanScoreComponent component = new PlanScoreComponent(ScoreComponentType.Activity, 1.23456, "home");

		assertEquals(ScoreComponentType.Activity, component.getType());
		assertEquals(1.23456, component.getScore());
		assertEquals("home", component.getDescription());
		assertEquals("Activity\thome\t1.235", component.toString());
	}

	private static void addPerson(Population population, String id, double firstScore, double secondScore,
			double selectedScore) {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId(id));
		Plan first = PopulationUtils.createPlan(person);
		first.setScore(firstScore);
		Plan second = PopulationUtils.createPlan(person);
		second.setScore(secondScore);
		person.addPlan(first);
		person.addPlan(second);
		person.setSelectedPlan(firstScore == selectedScore ? first : second);
		population.addPerson(person);
	}
}
