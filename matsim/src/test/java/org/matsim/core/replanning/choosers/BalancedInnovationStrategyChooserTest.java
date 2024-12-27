package org.matsim.core.replanning.choosers;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import java.util.IntSummaryStatistics;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("rawtypes")
class BalancedInnovationStrategyChooserTest {

	private Population population;
	private BalancedInnovationStrategyChooser chooser;
	private StrategyChooser.Weights<Plan, Person> weights;
	private InnovationCounting count;

	@BeforeEach
	void setUp() {

		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		for (int i = 0; i < 10000; i++) {
			Person person = population.getFactory().createPerson(Id.createPersonId(Integer.toString(i)));
			person.addPlan(population.getFactory().createPlan());
			population.addPerson(person);
		}

		chooser = new BalancedInnovationStrategyChooser(population);
		count = new InnovationCounting();
		weights = new StrategyChooser.Weights<>() {
			@Override
			public int size() {
				return 3;
			}

			@Override
			public double getWeight(int i) {
				return 0.15;
			}

			@Override
			public double getTotalWeights() {
				return 3 * 0.15;
			}

			@Override
			public PlanStrategy getStrategy(int i) {
				return switch (i) {
					case 0 -> new PlanStrategyImpl.Builder(new RandomPlanSelector<>()).build();
					case 1, 2 -> new PlanStrategyImpl.Builder(new RandomPlanSelector<>()).addStrategyModule(count).build();
					default -> throw new IllegalStateException("Unexpected value: " + i);
				};
			}
		};
	}

	@Test
	void innovationRates() {

		assertThat(count.getSum()).isEqualTo(0);

		runReplanning();
		assertThat(count.getSum()).isCloseTo(6666, Offset.offset(30));

		runReplanning();
		assertThat(count.getSum()).isCloseTo(6666 * 2, Offset.offset(1500));

		runReplanning();
		assertThat(count.getSum()).isCloseTo(6666 * 3, Offset.offset(1500));

		assertThat(count.getDifference()).isLessThanOrEqualTo(2);


		for (int i = 0; i < 500 -3; i++) {

			int before = count.getSum();

			runReplanning();

			// TODO: expected value per iteration is quite far off

			// Check the number of iterations per iteration
			assertThat(count.getSum() - before)
				.isCloseTo(6666, Offset.offset(3500));

		}

		assertThat(count.getSum()).isCloseTo(6666 * 500, Offset.offset(2500));
		assertThat(count.getDifference()).isLessThanOrEqualTo(2);
	}

	private void runReplanning() {
		for (Person person : population.getPersons().values()) {
			GenericPlanStrategy strategy = chooser.chooseStrategy(person, PopulationUtils.getSubpopulation(person), null, weights);
			strategy.run(person);
		}
	}

	private final static class InnovationCounting implements PlanStrategyModule {

		private final Int2IntMap count;

		private InnovationCounting() {
			count = new Int2IntOpenHashMap();
		}

		@Override
		public void prepareReplanning(ReplanningContext replanningContext) {

		}

		@Override
		public void handlePlan(Plan plan) {
			count.mergeInt(plan.getPerson().getId().index(), 1, Integer::sum);
		}

		@Override
		public void finishReplanning() {
		}

		int getSum() {
			return count.values().intStream().sum();
		}

		int getDifference() {
			IntSummaryStatistics stats = count.values().intStream().summaryStatistics();
			return stats.getMax() - stats.getMin();
		}

	}

}
