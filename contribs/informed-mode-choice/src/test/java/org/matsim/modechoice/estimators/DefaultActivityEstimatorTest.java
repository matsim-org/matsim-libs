package org.matsim.modechoice.estimators;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ScenarioTest;
import org.matsim.modechoice.TestScenario;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class DefaultActivityEstimatorTest extends ScenarioTest {

	@Test
	void person() {


		ScoringParametersForPerson p = injector.getInstance(ScoringParametersForPerson.class);
		Person person = controler.getScenario().getPopulation().getPersons().get(TestScenario.Agents.get(0));
		EstimatorContext context = new EstimatorContext(person, p.getScoringParameters(person));

		DefaultActivityEstimator est = new DefaultActivityEstimator(injector.getInstance(TimeInterpretation.class));

		List<Activity> act = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

		System.out.println(act.get(1));

		assertThat(est.estimate(context, 6 * 3600, act.get(1)))
				.isEqualTo(7.15, Offset.offset(0.1));

		assertThat(est.estimate(context, 3 * 3600, act.get(1)))
				.isEqualTo(7.15, Offset.offset(0.1));

		assertThat(est.estimate(context, 8 * 3600, act.get(1)))
				.isEqualTo(-4, Offset.offset(0.1));

		assertThat(est.estimate(context, 12 * 3600, act.get(1)))
				.isEqualTo(-4, Offset.offset(0.1));

	}
}
