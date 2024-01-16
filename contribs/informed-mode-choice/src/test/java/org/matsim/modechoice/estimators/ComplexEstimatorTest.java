package org.matsim.modechoice.estimators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.modechoice.PlanModelService;
import org.matsim.modechoice.TestScenario;
import org.matsim.testcases.MatsimTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ComplexEstimatorTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void bindings() {

		Config config = TestScenario.loadConfig(utils);

		config.controller().setLastIteration(2);
		Controler controler = MATSimApplication.prepare(TestScenario.class, config, "--complex");

		controler.run();

		PlanModelService service = controler.getInjector().getInstance(PlanModelService.class);
		ComplexTripEstimator est = (ComplexTripEstimator) service.getTripEstimator("pt");

		assertThat(est.getIters())
				.isEqualTo(3);

		assertThat(est.getEvents())
				.isGreaterThan(500_000);

	}

}
