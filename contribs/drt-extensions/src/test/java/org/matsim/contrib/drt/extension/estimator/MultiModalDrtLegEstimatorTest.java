package org.matsim.contrib.drt.extension.estimator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.estimator.DrtEstimatorModule;
import org.matsim.contrib.drt.estimator.impl.ExampleDrtEstimator;
import org.matsim.contrib.drt.extension.DrtTestScenario;
import org.matsim.contrib.drt.extension.modechoice.MultiModalDrtLegEstimator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
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

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	private Controler controler;

	private static void prepare(Controler controler) {
		InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder()
			.withFixedCosts(FixedCostsEstimator.DailyConstant.class, "car")
			.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "bike", "walk", "pt")
			.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderYesAndNo.class, "car")
			.withLegEstimator(MultiModalDrtLegEstimator.class, ModeOptions.AlwaysAvailable.class, "drt", "av");

		controler.addOverridingModule(builder.build());

		MultiModeDrtConfigGroup drtConfig = ConfigUtils.addOrGetModule(controler.getConfig(), MultiModeDrtConfigGroup.class);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				for (DrtConfigGroup el : drtConfig.getModalElements()) {
					install(new DrtEstimatorModule(el.mode, el, el.getDrtEstimatorParams().get()));
					DrtEstimatorModule.bindEstimator(binder(), el.mode).toInstance(new ExampleDrtEstimator(1.05, 300));
				}
			}
		});
	}

	private static void prepare(Config config) {

		// Set subtour mode selection as strategy
		List<ReplanningConfigGroup.StrategySettings> strategies = config.replanning().getStrategySettings().stream()
			.filter(s -> !s.getStrategyName().toLowerCase().contains("mode")
			).collect(Collectors.toList());

		strategies.add(new ReplanningConfigGroup.StrategySettings()
			.setStrategyName(InformedModeChoiceModule.SELECT_SUBTOUR_MODE_STRATEGY)
			.setSubpopulation("person")
			.setWeight(0.2));

		config.replanning().clearStrategySettings();
		strategies.forEach(s -> config.replanning().addStrategySettings(s));
	}

	@BeforeEach
	public void setUp() throws Exception {

		Config config = DrtTestScenario.loadConfig(utils);

		config.controller().setLastIteration(3);

		controler = MATSimApplication.prepare(new DrtTestScenario(MultiModalDrtLegEstimatorTest::prepare, MultiModalDrtLegEstimatorTest::prepare), config);
	}

	@Test
	void run() {

		String out = utils.getOutputDirectory();

		controler.run();

		assertThat(new File(out, "kelheim-mini-drt.drt_estimates_drt.csv"))
			.exists()
			.isNotEmpty();


	}
}
