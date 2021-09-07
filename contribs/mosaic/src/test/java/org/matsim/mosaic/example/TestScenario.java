package org.matsim.mosaic.example;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

public class TestScenario extends MATSimApplication {

	private static final File PATH;

	static {
		try {
			PATH = new File(ExamplesUtils.getTestScenarioURL("dvrp-grid").toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Could not load scenario");
		}
	}

	public TestScenario() {
		super(new File(PATH, "eight_shared_taxi_config.xml").toString());
	}


	@Override
	protected List<ConfigGroup> getCustomModules() {
		return List.of(new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
	}

	@Override
	protected Config prepareConfig(Config config) {

		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
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

		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(controler.getConfig(), MultiModeDrtConfigGroup.class);
		controler.addOverridingModule(new DvrpModule());

		// DRT Module is not needed here
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

	}
}
