package org.matsim.contrib.drt.extension.alonso_mora.example;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraConfigGroup;
import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraConfigurator;
import org.matsim.contrib.drt.extension.alonso_mora.MultiModeAlonsoMoraConfigGroup;
import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraConfigGroup.GlpkMpsAssignmentParameters;
import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraConfigGroup.GlpkMpsRelocationParameters;
import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraConfigGroup.MatrixEstimatorParameters;
import org.matsim.contrib.drt.extension.alonso_mora.AlonsoMoraConfigGroup.SequenceGeneratorType;
import org.matsim.contrib.drt.optimizer.insertion.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
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
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.referencing.FactoryException;

public class RunNewYork {
	static public void main(String[] args) throws CommandLine.ConfigurationException, FactoryException {
		CommandLine cmd = new CommandLine.Builder(args).requireOptions( //
				"demand-path", "network-path", "output-path", //
				"use-alonso-mora" //
		).allowOptions( //
				"fleet-size", "vehicle-capacity", //
				"maximum-waiting-time", "detour-factor", //
				"sampling-rate", "stop-duration", //
				"threads", "end-time" //
		).build();

		String demandPath = cmd.getOptionStrict("demand-path");
		String networkPath = cmd.getOptionStrict("network-path");
		String outputPath = cmd.getOptionStrict("output-path");

		int fleetSize = cmd.getOption("fleet-size").map(Integer::parseInt).orElse(1000);
		int vehicleCapacity = cmd.getOption("vehicle-capacity").map(Integer::parseInt).orElse(4);

		double maximumWaitingTime = cmd.getOption("maximum-waiting-time").map(Double::parseDouble).orElse(300.0);
		double detourFactor = cmd.getOption("detour-factor").map(Double::parseDouble).orElse(2.0);

		double samplingRate = cmd.getOption("sampling-rate").map(Double::parseDouble).orElse(1.0);
		double stopDuration = cmd.getOption("stop-duration").map(Double::parseDouble).orElse(60.0);

		boolean useAlonsoMora = Boolean.parseBoolean(cmd.getOptionStrict("use-alonso-mora"));

		int threads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkPath);
		config.plans().setInputFile(demandPath);

		double endTime = cmd.getOption("end-time").map(Double::parseDouble).orElse(24.0 * 3600.0 * 7.0);

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
				new DrtRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		{ // Demand downsampling
			Random random = new Random(0);
			Set<Id<Person>> removeIds = new HashSet<>();

			for (Person person : scenario.getPopulation().getPersons().values()) {
				if (random.nextDouble() > samplingRate) {
					removeIds.add(person.getId());
				}
			}

			removeIds.forEach(scenario.getPopulation()::removePerson);
		}

		FleetSpecification fleet = new FleetSpecificationImpl();
		{ // Create fleet
			for (Node node : scenario.getNetwork().getNodes().values()) {
				Integer nodeWeight = (Integer) node.getAttributes().getAttribute("weight");

				if (nodeWeight != null) {
					Set<Link> connectedLinks = new HashSet<>();
					connectedLinks.addAll(node.getInLinks().values());
					connectedLinks.addAll(node.getOutLinks().values());

					for (Link connectedLink : connectedLinks) {
						Double linkWeight = (Double) connectedLink.getAttributes().getAttribute("weight");

						if (linkWeight == null) {
							linkWeight = 0.0;
						}

						linkWeight += (double) nodeWeight / connectedLinks.size();
						connectedLink.getAttributes().putAttribute("weight", linkWeight);
					}
				}
			}

			List<Id<Link>> startLinkCandidates = new LinkedList<>();
			List<Double> startLinkWeight = new LinkedList<>();

			for (Link link : scenario.getNetwork().getLinks().values()) {
				if (link.getAllowedModes().contains("car")) {
					Double weight = (Double) link.getAttributes().getAttribute("weight");

					if (weight != null && weight > 0.0) {
						startLinkCandidates.add(link.getId());
						startLinkWeight.add(weight);
					}
				}
			}

			for (int k = 0; k < startLinkCandidates.size(); k++) {
				if (k > 0) {
					startLinkWeight.set(k, startLinkWeight.get(k - 1) + startLinkWeight.get(k));
				}
			}

			double maximum = startLinkWeight.get(startLinkWeight.size() - 1);

			for (int k = 0; k < startLinkCandidates.size(); k++) {
				startLinkWeight.set(k, startLinkWeight.get(k) / maximum);
			}

			Random random = new Random(0);

			for (int i = 0; i < fleetSize; i++) {
				double selector = random.nextDouble();
				int selectionIndex = (int) startLinkWeight.stream().filter(w -> w < selector).count();

				fleet.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder() //
						.id(Id.create("drt_" + i, DvrpVehicle.class)) //
						.capacity(vehicleCapacity) //
						.serviceBeginTime(0.0) //
						.serviceEndTime(endTime) //
						.startLinkId(startLinkCandidates.get(selectionIndex)) //
						.build());
			}
		}

		// Set up config
		config.controler().setOutputDirectory(outputPath);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);

		config.qsim().setNumberOfThreads(Math.min(12, threads));
		config.global().setNumberOfThreads(threads);

		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);

		config.qsim().setStartTime(0.0);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);

		config.qsim().setEndTime(endTime);

		ModeParams modeParams = new ModeParams("drt");
		config.planCalcScore().addModeParams(modeParams);

		ActivityParams genericParams = new ActivityParams("generic");
		genericParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(genericParams);

		ActivityParams interactionParams = new ActivityParams("drt interaction");
		interactionParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(interactionParams);

		StrategySettings keepSettings = new StrategySettings();
		keepSettings.setStrategyName("BestScore");
		keepSettings.setWeight(1.0);
		config.strategy().addStrategySettings(keepSettings);

		DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
		config.addModule(dvrpConfig);

		MultiModeDrtConfigGroup drtConfig = new MultiModeDrtConfigGroup();
		config.addModule(drtConfig);

		DrtConfigGroup modeConfig = new DrtConfigGroup() //
				.setMode(TransportMode.drt) //
				.setMaxTravelTimeAlpha(detourFactor) //
				.setMaxTravelTimeBeta(stopDuration) //
				.setMaxWaitTime(maximumWaitingTime + stopDuration) //
				.setStopDuration(stopDuration) //
				.setRejectRequestIfMaxWaitOrTravelTimeViolated(true) //
				.setUseModeFilteredSubnetwork(false) //
				.setIdleVehiclesReturnToDepots(false) //
				.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door) //
				.setPlotDetailedCustomerStats(true) //
				.setMaxWalkDistance(1000.) //
				.setNumberOfThreads(threads);

		modeConfig.addParameterSet(new ExtensiveInsertionSearchParams());
		drtConfig.addParameterSet(modeConfig);

		cmd.applyConfiguration(config);

		// Set up controller
		Controler controller = new Controler(scenario);

		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new MultiModeDrtModule());
		controller.configureQSimComponents(DvrpQSimComponents.activateAllModes(drtConfig));

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				bindModal(FleetSpecification.class).toInstance(fleet);
			}
		});

		// Alonso-Mora

		if (useAlonsoMora) {
			config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);

			MultiModeAlonsoMoraConfigGroup multiModeConfig = new MultiModeAlonsoMoraConfigGroup();
			config.addModule(multiModeConfig);

			AlonsoMoraConfigGroup amConfig = new AlonsoMoraConfigGroup();
			multiModeConfig.addParameterSet(amConfig);

			amConfig.setMaximumQueueTime(0.0);

			amConfig.setAssignmentInterval(30);
			amConfig.setRelocationInterval(30);

			amConfig.getCongestionMitigationParameters().setAllowBareReassignment(false);
			amConfig.getCongestionMitigationParameters().setAllowPickupViolations(true);
			amConfig.getCongestionMitigationParameters().setAllowPickupsWithDropoffViolations(true);
			amConfig.getCongestionMitigationParameters().setPreserveVehicleAssignments(true);

			amConfig.setRerouteDuringScheduling(false);

			amConfig.setCheckDeterminsticTravelTimes(true);

			amConfig.setSequenceGeneratorType(SequenceGeneratorType.Combined);

			GlpkMpsAssignmentParameters assignmentParameters = new GlpkMpsAssignmentParameters();
			amConfig.addParameterSet(assignmentParameters);

			GlpkMpsRelocationParameters relocationParameters = new GlpkMpsRelocationParameters();
			amConfig.addParameterSet(relocationParameters);

			MatrixEstimatorParameters estimator = new MatrixEstimatorParameters();
			amConfig.addParameterSet(estimator);

			AlonsoMoraConfigurator.configure(controller, amConfig.getMode());
		}

		controller.run();
	}
}
