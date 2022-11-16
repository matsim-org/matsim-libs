package org.matsim.contrib.drt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.estimator.BasicDRTLegEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpModeLimitedMaxSpeedTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.examples.ExamplesUtils;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * A test scenario based on kelheim example.
 */
public class DrtTestScenario extends MATSimApplication {

	public static void main(String[] args) {
		MATSimApplication.run(DrtTestScenario.class, args);
	}

	public DrtTestScenario() {
	}

	public DrtTestScenario(@Nullable Config config) {
		super(config);
	}

	public static Config loadConfig(MatsimTestUtils utils) {

		File f;
		try {
			f = new File(ExamplesUtils.getTestScenarioURL("kelheim/config-with-drt.xml").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		Config config = ConfigUtils.loadConfig(f.toString());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		return config;
	}

	@Override
	protected Config prepareConfig(Config config) {

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		imc.setModes(Set.of("drt", "av", "car", "pt", "bike", "walk"));

		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);

		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		scenario.getPopulation()
				.getFactory()
				.getRouteFactories()
				.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
	}

	@Override
	protected void prepareControler(Controler controler) {

		InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder()
				.withFixedCosts(FixedCostsEstimator.DailyConstant.class, "car")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "bike", "walk", "pt")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderYesAndNo.class, "car")
				.withLegEstimator(BasicDRTLegEstimator.class, ModeOptions.AlwaysAvailable.class, "drt", "av");

		controler.addOverridingModule(builder.build());

		// DRT specific

		Config config = controler.getConfig();

		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		// Add speed limit to av vehicle
		double maxSpeed = controler.getScenario()
				.getVehicles()
				.getVehicleTypes()
				.get(Id.create("autonomous_vehicle", VehicleType.class))
				.getMaximumVelocity();

		controler.addOverridingModule(
				new DvrpModeLimitedMaxSpeedTravelTimeModule("av", config.qsim().getTimeStepSize(),
						maxSpeed));

	}
}
