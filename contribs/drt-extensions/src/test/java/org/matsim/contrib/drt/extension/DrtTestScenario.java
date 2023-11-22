package org.matsim.contrib.drt.extension;

import com.google.common.collect.Sets;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpModeLimitedMaxSpeedTravelTimeModule;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A test scenario based on kelheim example.
 */
public class DrtTestScenario extends MATSimApplication {


	private final Consumer<Controler> prepareControler;
	private final Consumer<Config> prepareConfig;

	public static void main(String[] args) {
		run(DrtTestScenario.class, args);
	}

	public DrtTestScenario() {
		this(controler -> {}, config -> {});
	}

	public DrtTestScenario(Consumer<Controler> prepareControler, Consumer<Config> prepareConfig) {
		this.prepareControler = prepareControler;
		this.prepareConfig = prepareConfig;
	}

	public DrtTestScenario(@Nullable Config config) {
		super(config);
		this.prepareControler = controler -> {};
		this.prepareConfig = c -> {};
	}

	public static Config loadConfig(MatsimTestUtils utils) {

		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config-with-drt.xml"));
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		return config;
	}

	@Override
	protected Config prepareConfig(Config config) {

		SnzActivities.addScoringParams(config);

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("car interaction").setTypicalDuration(60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("other").setTypicalDuration(600 * 3));

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_start").setTypicalDuration(60 * 15));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_end").setTypicalDuration(60 * 15));

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		imc.setModes(Set.of("drt", "av", "car", "pt", "bike", "walk"));

		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);

		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());

		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		prepareConfig.accept(config);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		// Freight needs to be added manually
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> modes = link.getAllowedModes();

			// allow freight traffic together with cars
			if (modes.contains("car")) {
				HashSet<String> newModes = Sets.newHashSet(modes);
				newModes.add("freight");

				link.setAllowedModes(newModes);
			}
		}

		scenario.getPopulation()
				.getFactory()
				.getRouteFactories()
				.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
	}

	@Override
	protected void prepareControler(Controler controler) {

		prepareControler.accept(controler);

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

		controler.addOverridingModule(new SimWrapperModule());

	}
}
