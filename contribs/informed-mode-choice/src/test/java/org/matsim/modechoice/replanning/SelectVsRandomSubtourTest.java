package org.matsim.modechoice.replanning;

import com.google.inject.Key;
import com.google.inject.name.Names;
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleSortedMap;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForMobsim;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks if {@link RandomSubtourModeStrategy} and {@link SelectSubtourModeStrategy} are equivalent under specific configuration.
 */
public class SelectVsRandomSubtourTest extends ScenarioTest {

	@Override
	protected String[] getArgs() {
		return new String[]{"--mc"};
	}


	@Override
	protected void prepareConfig(Config config) {
		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		imc.setTopK(0);
		imc.setInvBeta(Double.POSITIVE_INFINITY);
	}


	@Test
	void replanning() {

		PrepareForMobsim prepare = injector.getInstance(PrepareForMobsim.class);
		prepare.run();

		PlanStrategy select = injector.getInstance(Key.get(PlanStrategy.class, Names.named(InformedModeChoiceModule.SELECT_SUBTOUR_MODE_STRATEGY)));
		PlanStrategy random = injector.getInstance(Key.get(PlanStrategy.class, Names.named(InformedModeChoiceModule.RANDOM_SUBTOUR_MODE_STRATEGY)));

		List<Plan> plans = TestScenario.Agents.stream()
			.map(agent -> controler.getScenario().getPopulation().getPersons().get(agent))
			.map(Person::getSelectedPlan)
			.toList();

		double[] randomModes = sampleModes(plans, random);
		double[] selectModes = sampleModes(plans, select);

		assertThat(selectModes)
			.containsExactly(randomModes, Offset.offset(1700d));

	}

	/**
	 * Run the strategy and count obtained modes.
	 */
	private double[] sampleModes(List<Plan> plans, PlanStrategy strategy) {

		Object2DoubleSortedMap<String> modes = new Object2DoubleAVLTreeMap<>();

		for (Plan plan : plans) {

			Person person = plan.getPerson();
			person.getPlans().clear();

			person.addPlan(plan);

		}

		for (int i = 0; i < 500; i++) {
			int finalI = i;
			strategy.init(() -> finalI);

			for (Plan plan : plans)
				strategy.run(plan.getPerson());

			strategy.finish();

			for (Plan plan : plans) {

				Plan selected = plan.getPerson().getSelectedPlan();
				plan.getPerson().getPlans().removeIf(p -> selected != p);
				for (Leg leg : TripStructureUtils.getLegs(selected)) {
					modes.mergeDouble(leg.getMode(), 1, Double::sum);
				}

			}
		}

		System.out.println("Modes: " + modes.keySet());

		return modes.values().toDoubleArray();
	}
}
