package org.matsim.modechoice;

import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class EstimateRouterTest {

	private EstimateRouter router;
	private InformedModeChoiceConfigGroup group;
	private Controler controler;

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@BeforeEach
	public void setUp() throws Exception {

		Config config = TestScenario.loadConfig(utils);

		group = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		controler = MATSimApplication.prepare(TestScenario.class, config);

		Injector injector = controler.getInjector();
		router = injector.getInstance(EstimateRouter.class);

	}

	@Test
	void routing() {


		Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();

		Plan plan = persons.get(TestScenario.Agents.get(1)).getSelectedPlan();
		PlanModel planModel = PlanModel.newInstance(plan);

		router.routeModes(planModel, group.getModes());

		assertThat(planModel)
				.isNotNull();


	}
}
