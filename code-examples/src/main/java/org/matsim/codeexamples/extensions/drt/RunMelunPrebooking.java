package org.matsim.codeexamples.extensions.drt;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.extension.insertion.DrtInsertionModule;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.prebooking.PrebookingParams;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogic;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

public class RunMelunPrebooking {
	private final static Logger logger = LogManager.getLogger(RunMelunPrebooking.class);

	public static class RunSettings {
		String outputName = "unknown";

		// scenario
		int seed = 0;
		double samplingRate = 0.1;

		// fleet
		int vehicles = 20;
		int capacity = 8;

		// prebooking
		double prebookingShare = 0.0;
		double submissionSlack = 0.0;
		boolean scheduleWaitBeforeDrive = false;

		// service level
		double maxWaitTime = 300.0;
		double maxTravelTimeAlpha = 1.3;

		// constraints
		double maximumVehicleRange = Double.POSITIVE_INFINITY;
		double euclideanDistanceFactor = 1.5;

		boolean enableExclusivity = false;
	}

	public static void runSingle(File populationPath, File networkPath, File outputPath, RunSettings settings) {
		// configuration
		Config config = ConfigUtils.createConfig(new MultiModeDrtConfigGroup(), new DvrpConfigGroup());

		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setStartTime(0.0);
		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);

		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.global().setRandomSeed(settings.seed);

		File specificOutputPath = new File(outputPath, settings.outputName);
		config.controller().setOutputDirectory(specificOutputPath.toString());

		if (new File(specificOutputPath, "output_events.xml.gz").exists()) {
			logger.warn("Skipping " + settings.outputName);
			return;
		}

		ActivityParams activityParams = new ActivityParams("generic");
		activityParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(activityParams);

		ModeParams modeParams = new ModeParams("drt");
		config.scoring().addModeParams(modeParams);

		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		multiModeDrtConfig.addParameterSet(drtConfig);

		drtConfig.mode = "drt";
		drtConfig.operationalScheme = OperationalScheme.door2door;
		drtConfig.stopDuration = 60.0;
		drtConfig.maxWaitTime = settings.maxWaitTime;
		drtConfig.maxTravelTimeAlpha = settings.maxTravelTimeAlpha;
		drtConfig.maxTravelTimeBeta = settings.maxWaitTime;

		DrtInsertionSearchParams insertionSearchParams = new ExtensiveInsertionSearchParams();
		drtConfig.addDrtInsertionSearchParams(insertionSearchParams);

		PrebookingParams prebookingParams = new PrebookingParams();
		drtConfig.addParameterSet(prebookingParams);
		prebookingParams.scheduleWaitBeforeDrive = settings.scheduleWaitBeforeDrive;

		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());

		// scenario
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
				new DrtRouteFactory());

		new PopulationReader(scenario).readFile(populationPath.toString());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());

		// sampling
		Random random = new Random(1000 * settings.seed);
		if (settings.samplingRate < 1.0) {
			IdSet<Person> removeIds = new IdSet<>(Person.class);

			for (Person person : scenario.getPopulation().getPersons().values()) {
				if (random.nextDouble() > settings.samplingRate) {
					removeIds.add(person.getId());
				}
			}

			removeIds.forEach(scenario.getPopulation()::removePerson);
		}

		// controller
		Controler controller = new Controler(scenario);
		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new MultiModeDrtModule());
		controller.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		// fleet
		List<Id<Link>> linkIds = new ArrayList<>(scenario.getNetwork().getLinks().keySet());
		Collections.sort(linkIds);

		FleetSpecification fleetSpecification = new FleetSpecificationImpl();
		for (int i = 0; i < settings.vehicles; i++) {
			fleetSpecification.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder() //
					.id(Id.create("veh" + i, DvrpVehicle.class)) //
					.capacity(settings.capacity) //
					.serviceBeginTime(0.0) //
					.serviceEndTime(30 * 3600.0) //
					.startLinkId(linkIds.get(random.nextInt(linkIds.size()))) //
					.build());
		}

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				bindModal(FleetSpecification.class).toInstance(fleetSpecification);
				bindModal(TravelTime.class).toInstance(new QSimFreeSpeedTravelTime(config.qsim()));
			}
		});

		// prebooking logic
		ProbabilityBasedPrebookingLogic.install(controller, drtConfig, settings.prebookingShare,
				settings.submissionSlack);

		// constraints
		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig) //
				.withSingleRequestPerPerson();

		if (Double.isFinite(settings.maximumVehicleRange)) {
			insertionModule //
					.withVehicleRange(settings.maximumVehicleRange) //
					.withEuclideanDistanceApproximator(settings.euclideanDistanceFactor);
		}

		if (settings.enableExclusivity) {
			insertionModule //
					.withExclusivity(request -> request.getId().toString().contains("prebooked"));
		}

		controller.addOverridingQSimModule(insertionModule);

		// run
		controller.run();
	}

	static public void runPrebookingExperiments(File populationPath, File networkPath, File outputPath) {
		RunSettings settings = new RunSettings();
		int seeds = 10;

		for (int seed = 0; seed < seeds; seed++) {
			for (boolean scheduleWaitBeforeDrive : Arrays.asList(false, true)) {
				for (double share : Arrays.asList(0.0, 0.25, 0.5, 0.75, 1.0)) {
					for (double submissionSlack : Arrays.asList(0.0, 600.0, 3600.0, 4.0 * 3600.0, 48.0 * 3600.0)) {
						if (submissionSlack == 0.0 && share > 0.0) {
							continue;
						}

						settings.outputName = "main_share" + share + "_slack" + submissionSlack + "_seed" + seed
								+ "_wait" + scheduleWaitBeforeDrive;
						settings.seed = seed;
						settings.scheduleWaitBeforeDrive = scheduleWaitBeforeDrive;
						settings.prebookingShare = share;
						settings.submissionSlack = submissionSlack;

						runSingle(populationPath, networkPath, outputPath, settings);
					}
				}
			}
		}
	}

	static public void runRangeExperiments(File populationPath, File networkPath, File outputPath) {
		RunSettings settings = new RunSettings();
		int seeds = 10;

		for (int seed = 0; seed < seeds; seed++) {
			for (double share : Arrays.asList(0.0, 0.25, 0.5, 0.75, 1.0)) {
				for (double maximumRange : Arrays.asList(50.0, 100.0, 150.0, 200.0, 500.0)) {
					settings.outputName = "range_share" + share + "_constraint" + maximumRange + "_seed" + seed;
					settings.seed = seed;
					settings.scheduleWaitBeforeDrive = true;
					settings.prebookingShare = share;
					settings.submissionSlack = 30.0 * 3600.0;
					settings.maximumVehicleRange = maximumRange * 1e3;

					runSingle(populationPath, networkPath, outputPath, settings);
				}
			}
		}
	}

	static public void runExclusivityExperiments(File populationPath, File networkPath, File outputPath) {
		RunSettings settings = new RunSettings();
		int seeds = 10;

		for (int seed = 0; seed < seeds; seed++) {
			for (double share : Arrays.asList(0.0, 0.25, 0.5, 0.75, 1.0)) {
				for (boolean enableExclusivity : Arrays.asList(false, true)) {
					settings.outputName = "exclusivity_share" + share + "_exclusive" + enableExclusivity + "_seed"
							+ seed;
					settings.seed = seed;
					settings.scheduleWaitBeforeDrive = true;
					settings.prebookingShare = share;
					settings.submissionSlack = 30.0 * 3600.0;
					settings.enableExclusivity = enableExclusivity;

					runSingle(populationPath, networkPath, outputPath, settings);
				}
			}
		}
	}

	static public void runAll(File populationPath, File networkPath, File outputPath) {
		runPrebookingExperiments(populationPath, networkPath, outputPath);
		runRangeExperiments(populationPath, networkPath, outputPath);
		runExclusivityExperiments(populationPath, networkPath, outputPath);
	}

	static public final String DEFAULT_POPULATION_PATH = RunMelunPrebooking.class
			.getResource("melun/melun_drt_population.xml.gz").toString();

	static public final String DEFAULT_NETWORK_PATH = RunMelunPrebooking.class
			.getResource("melun/melun_drt_network.xml.gz").toString();

	static public final String DEFAULT_OUTPUT_PATH = "melun_prebooking_output";

	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.allowOptions("population-path", "network-path", "output-path") //
				.build();

		File populationPath = new File(cmd.getOption("population-path").orElse(DEFAULT_POPULATION_PATH));
		File networkPath = new File(cmd.getOption("network-path").orElse(DEFAULT_NETWORK_PATH));
		File outputPath = new File(cmd.getOption("output-path").orElse(DEFAULT_OUTPUT_PATH));

		runAll(populationPath, networkPath, outputPath);
	}
}