package org.matsim.contrib.drt.estimator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.DrtTestScenario;
import org.matsim.contrib.drt.estimator.run.DrtEstimatorConfigGroup;
import org.matsim.contrib.drt.estimator.run.DrtEstimatorModule;
import org.matsim.contrib.drt.estimator.run.MultiModeDrtEstimatorConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiModalDrtLegEstimatorTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private Controler controler;

	@Before
	public void setUp() throws Exception {

		Config config = DrtTestScenario.loadConfig(utils);

		config.controler().setLastIteration(3);

		controler = MATSimApplication.prepare(new DrtTestScenario(MultiModalDrtLegEstimatorTest::prepare, MultiModalDrtLegEstimatorTest::prepare), config);
	}

	private static void prepare(Controler controler) {
		InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder()
				.withFixedCosts(FixedCostsEstimator.DailyConstant.class, "car")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "bike", "walk", "pt")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderYesAndNo.class, "car")
				.withLegEstimator(MultiModalDrtLegEstimator.class, ModeOptions.AlwaysAvailable.class, "drt", "av");

		controler.addOverridingModule(builder.build());
		controler.addOverridingModule(new DrtEstimatorModule());
	}

	private static void prepare(Config config) {

		MultiModeDrtEstimatorConfigGroup estimators = ConfigUtils.addOrGetModule(config, MultiModeDrtEstimatorConfigGroup.class);

		estimators.addParameterSet(new DrtEstimatorConfigGroup("drt"));
		estimators.addParameterSet(new DrtEstimatorConfigGroup("av"));

		// Set subtour mode selection as strategy
		List<StrategyConfigGroup.StrategySettings> strategies = config.strategy().getStrategySettings().stream()
				.filter(s -> !s.getStrategyName().toLowerCase().contains("mode")
				).collect(Collectors.toList());

		strategies.add(new StrategyConfigGroup.StrategySettings()
				.setStrategyName(InformedModeChoiceModule.SELECT_SUBTOUR_MODE_STRATEGY)
						.setSubpopulation("person")
						.setWeight(0.2));

		config.strategy().clearStrategySettings();
		strategies.forEach(s -> config.strategy().addStrategySettings(s));

	}

	@Test
	public void run() {

		String out = utils.getOutputDirectory();

		controler.run();

		assertThat(new File(out, "kelheim-mini-drt.drt_estimates_drt.csv"))
				.exists()
				.isNotEmpty();


	}
}
