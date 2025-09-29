package org.matsim.simwrapper.dashboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.simwrapper.*;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;


public class CommercialTrafficDashboardTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testCommercialViewer() {


		Config config = utils.createConfigWithTestInputFilePathAsContext();

		config.global().setCoordinateSystem(null);
		config.network().setInputFile("output_network.xml.gz");
		config.plans().setInputFile("output_plans.xml.gz");
		config.vehicles().setVehiclesFile("output_vehicles.xml.gz");
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);
		config.global().setCoordinateSystem(TransformationFactory.ATLANTIS);
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_start").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_end").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("service").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("pickup").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("delivery").setTypicalDuration(30 * 60));

		for (String subpopulation : List.of("commercialPersonTraffic", "commercialPersonTraffic_service", "goodsTraffic")) {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta).setWeight(
					0.85).setSubpopulation(subpopulation));

			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute).setWeight(
					0.1).setSubpopulation(subpopulation));
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);

		SimWrapper sw = SimWrapper.create(config);
		sw.getConfigGroup().defaultParams().setShp("shp/testRegions.shp");
		sw.getConfigGroup().setSampleSize(1.0);
		sw.getConfigGroup().setDefaultDashboards(SimWrapperConfigGroup.Mode.disabled);
		sw.addDashboard(new CommercialTrafficDashboard());

		controler.addOverridingModule(new SimWrapperModule(sw));

		controler.run();

//		new CreateSingleSimWrapperDashboard().execute(
//			utils.getInputDirectory(), "--type",
//			"commercialTraffic"
//		);

	}

}
