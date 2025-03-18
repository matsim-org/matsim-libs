package org.matsim.contrib.ev.strategic.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.charging.QueuedAtChargerEvent;
import org.matsim.contrib.ev.charging.QueuedAtChargerEventHandler;
import org.matsim.contrib.ev.charging.QuitQueueAtChargerEvent;
import org.matsim.contrib.ev.charging.QuitQueueAtChargerEventHandler;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup.SelectionStrategy;
import org.matsim.contrib.ev.strategic.StrategicChargingModule;
import org.matsim.contrib.ev.strategic.infrastructure.FacilityChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.PersonChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.PublicChargerProvider;
import org.matsim.contrib.ev.strategic.replanning.StrategicChargingReplanningStrategy;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoringParameters;
import org.matsim.contrib.ev.withinday.WithinDayEvConfigGroup;
import org.matsim.contrib.ev.withinday.WithinDayEvEngine;
import org.matsim.contrib.ev.withinday.WithinDayEvModule;
import org.matsim.contrib.ev.withinday.events.AbortChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.AbortChargingAttemptEventHandler;
import org.matsim.contrib.ev.withinday.events.AbortChargingProcessEvent;
import org.matsim.contrib.ev.withinday.events.AbortChargingProcessEventHandler;
import org.matsim.contrib.ev.withinday.events.FinishChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.FinishChargingAttemptEventHandler;
import org.matsim.contrib.ev.withinday.events.FinishChargingProcessEvent;
import org.matsim.contrib.ev.withinday.events.FinishChargingProcessEventHandler;
import org.matsim.contrib.ev.withinday.events.StartChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.StartChargingAttemptEventHandler;
import org.matsim.contrib.ev.withinday.events.StartChargingProcessEvent;
import org.matsim.contrib.ev.withinday.events.StartChargingProcessEventHandler;
import org.matsim.contrib.ev.withinday.events.UpdateChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.UpdateChargingAttemptEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

public class TestScenarioBuilder {
	private final MatsimTestUtils utils;

	public TestScenarioBuilder(MatsimTestUtils utils) {
		this.utils = utils;
	}

	// NETWORK PART

	private Integer networkSize = 10; // nodes
	private Double linkLength = 200.0; // meters
	private Double linkSpeed = 1.0; // meters per second

	public TestScenarioBuilder setNetworkSize(int networkSize) {
		this.networkSize = networkSize;
		return this;
	}

	public TestScenarioBuilder setLinkLength(double linkLength) {
		this.linkLength = linkLength;
		return this;
	}

	private void prepareNetwork(Scenario scenario) {
		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();

		Node[][] nodes = new Node[networkSize][networkSize];
		for (int i = 0; i < networkSize; i++) {
			for (int j = 0; j < networkSize; j++) {
				nodes[i][j] = networkFactory.createNode(Id.createNodeId(i + ":" + j),
						new Coord(i * linkLength, j * linkLength));
				network.addNode(nodes[i][j]);
			}
		}

		List<Link> links = new LinkedList<>();

		for (int i = 0; i < networkSize; i++) {
			for (int j = 0; j < networkSize - 1; j++) {
				Node firstNode = nodes[i][j];
				Node secondNode = nodes[i][j + 1];

				links.add(networkFactory.createLink(
						Id.createLinkId(firstNode.getId().toString() + "::" + secondNode.getId().toString()), firstNode,
						secondNode));

				links.add(networkFactory.createLink(
						Id.createLinkId(secondNode.getId().toString() + "::" + firstNode.getId().toString()),
						secondNode, firstNode));
			}
		}

		for (int j = 0; j < networkSize; j++) {
			for (int i = 0; i < networkSize - 1; i++) {
				Node firstNode = nodes[i][j];
				Node secondNode = nodes[i + 1][j];

				links.add(networkFactory.createLink(
						Id.createLinkId(firstNode.getId().toString() + "::" + secondNode.getId().toString()), firstNode,
						secondNode));

				links.add(networkFactory.createLink(
						Id.createLinkId(secondNode.getId().toString() + "::" + firstNode.getId().toString()),
						secondNode, firstNode));
			}
		}

		for (Link link : links) {
			link.setCapacity(1e9);
			link.setNumberOfLanes(1);
			link.setAllowedModes(Collections.singleton("car"));
			link.setLength(linkLength);
			link.setFreespeed(linkSpeed);
			network.addLink(link);
		}

		ActivityFacilities facilities = scenario.getActivityFacilities();
		ActivityFacilitiesFactory facilitiesFactory = facilities.getFactory();

		for (Link link : links) {
			ActivityFacility facility = facilitiesFactory.createActivityFacility(
					Id.create(link.getId(), ActivityFacility.class), link.getCoord(), link.getId());
			facilities.addActivityFacility(facility);
		}
	}

	// INFRASTRUCTURE PART

	record ChargerItem(String identifier, int x, int y, int plugCount, double plugPower, Set<Id<Person>> personIds,
			Set<Id<ActivityFacility>> facilityIds, boolean isPublic) {
	}

	private List<ChargerItem> chargers = new LinkedList<>();

	public TestScenarioBuilder addCharger(String identifier, int x, int y, int plugCount, double plugPower) {
		chargers.add(new ChargerItem(identifier, x, y, plugCount, plugPower, Collections.emptySet(),
				Collections.emptySet(), false));
		return this;
	}

	public TestScenarioBuilder addCharger(String identifier, int x, int y, int plugCount, double plugPower,
			Set<Id<Person>> personIds,
			Set<Id<ActivityFacility>> facilityIds, boolean isPublic) {
		chargers.add(new ChargerItem(identifier, x, y, plugCount, plugPower, personIds, facilityIds, isPublic));
		return this;
	}

	public TestScenarioBuilder addHomeCharger(String personId, int x, int y, int plugCount, double plugPower,
			String type) {
		String chargerId = "charger:person:" + personId;
		return addCharger(chargerId, x, y, plugCount, plugPower, Collections.singleton(Id.createPersonId(personId)),
				Collections.emptySet(), false);
	}

	public TestScenarioBuilder addWorkCharger(int x, int y, int plugCount, double plugPower,
			String type) {
		Id<Link> linkId = Id.createLinkId(x + ":" + y + "::" + (x + 1) + ":" + y);
		String chargerId = "charger:facility:" + linkId.toString();
		return addCharger(chargerId, x, y, plugCount, plugPower, Collections.emptySet(),
				Collections.singleton(Id.create(linkId.toString(), ActivityFacility.class)), false);
	}

	public TestScenarioBuilder addPublicCharger(String chargerId, int x, int y, int plugCount, double plugPower,
			String type) {
		String publicChargerId = "charger:public" + chargerId;
		return addCharger(publicChargerId, x, y, plugCount, plugPower, Collections.emptySet(),
				Collections.emptySet(), true);
	}

	private void prepareInfrastructure(Controler controller) {
		ChargingInfrastructureSpecification infrastructure = new ChargingInfrastructureSpecificationDefaultImpl();

		for (ChargerItem item : chargers) {
			Verify.verify(item.x >= 0 && item.x < networkSize - 1, "Invalid depot for vehicle " + item.identifier);
			Verify.verify(item.y >= 0 && item.y < networkSize - 1, "Invalid depot for vehicle " + item.identifier);

			Id<Link> linkId = Id.createLinkId(item.x + ":" + item.y + "::" + (item.x + 1) + ":" + item.y);

			ChargerSpecification specification = ImmutableChargerSpecification.newBuilder() //
					.id(Id.create(item.identifier, Charger.class)) // ,
					.linkId(linkId) //
					.chargerType("default") //
					.plugPower(item.plugPower) //
					.plugCount(item.plugCount) //
					.build();

			if (item.personIds.size() > 0) {
				PersonChargerProvider.setPersonIds(specification, item.personIds);
			}

			if (item.facilityIds.size() > 0) {
				FacilityChargerProvider.setFacilityIds(specification, item.facilityIds);
			}

			if (item.isPublic) {
				PublicChargerProvider.setPublic(specification, true);
			}

			infrastructure.addChargerSpecification(specification);
		}

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ChargingInfrastructureSpecification.class).toInstance(infrastructure);
			}
		});
	}

	// DEMAND PART

	record ActivityItem(String type, int x, int y, double endTime, String previousMode) {

	}

	record PersonItem(String identifer, List<ActivityItem> activities, double initialSoc) {
	}

	private LinkedList<PersonItem> persons = new LinkedList<>();

	public TestScenarioBuilder addPerson(String identifier, double initialSoc) {
		PersonItem item = new PersonItem(identifier, new LinkedList<>(), initialSoc);
		persons.add(item);
		return this;
	}

	public TestScenarioBuilder addActivity(String type, int x, int y, double endTime) {
		Preconditions.checkState(persons.size() > 0);
		persons.getLast().activities.add(new ActivityItem(type, x, y, endTime, "car"));
		return this;
	}

	public TestScenarioBuilder addActivity(String type, int x, int y) {
		return addActivity(type, x, y, Double.NaN);
	}

	public TestScenarioBuilder addActivity(String type, int x, int y, double endTime, String previousMode) {
		Preconditions.checkState(persons.size() > 0);
		persons.getLast().activities.add(new ActivityItem(type, x, y, endTime, previousMode));
		return this;
	}

	public TestScenarioBuilder addActivity(String type, int x, int y, String previousMode) {
		return addActivity(type, x, y, Double.NaN, previousMode);
	}

	private void preparePopulation(Scenario scenario) {
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		Vehicles vehicles = scenario.getVehicles();
		VehiclesFactory vehiclesFactory = vehicles.getFactory();

		VehicleType electricVehicleType = vehicles.getVehicleTypes().get(Id.create("electric", VehicleType.class));

		for (var item : persons) {
			Person person = populationFactory.createPerson(Id.createPersonId(item.identifer));
			WithinDayEvEngine.activate(person);
			population.addPerson(person);

			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);

			List<Activity> activities = new LinkedList<>();

			for (var activityItem : item.activities) {
				Verify.verify(activityItem.x >= 0 && activityItem.y < networkSize - 1,
						"Invalid location for person " + item.identifer);

				Id<Link> linkId = Id.createLinkId(
						activityItem.x + ":" + activityItem.y + "::" + (activityItem.x + 1) + ":" + activityItem.y);
				Activity activity = populationFactory.createActivityFromLinkId(activityItem.type, linkId);
				activity.setFacilityId(Id.create(linkId, ActivityFacility.class));

				if (!Double.isNaN(activityItem.endTime)) {
					activity.setEndTime(activityItem.endTime);
				}

				activities.add(activity);
			}

			for (int i = 0; i < activities.size(); i++) {
				if (i > 0) {
					String mode = item.activities.get(i).previousMode;
					Leg leg = populationFactory.createLeg(mode);
					leg.setRoutingMode(mode);
					plan.addLeg(leg);
				}

				plan.addActivity(activities.get(i));
			}

			Vehicle vehicle = vehiclesFactory.createVehicle(Id.createVehicleId(item.identifer), electricVehicleType);
			vehicles.addVehicle(vehicle);

			ElectricFleetUtils.setInitialSoc(vehicle, item.initialSoc);

			VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, Collections.singletonMap("car", vehicle.getId()));
		}
	}

	private double range_m = 10 * 1e3;

	public TestScenarioBuilder setElectricVehicleRange(double range_m) {
		this.range_m = range_m;
		return this;
	}

	private void prepareVehicles(Scenario scenario) {
		Vehicles vehicles = scenario.getVehicles();
		VehiclesFactory vehiclesFactory = vehicles.getFactory();

		VehicleType electricVehicleType = vehiclesFactory.createVehicleType(Id.create("electric", VehicleType.class));
		electricVehicleType.setNetworkMode(TransportMode.car);
		vehicles.addVehicleType(electricVehicleType);

		VehicleUtils.setEnergyCapacity(electricVehicleType.getEngineInformation(), EvUnits.J_to_kWh(range_m));

		VehicleUtils.setHbefaTechnology(electricVehicleType.getEngineInformation(),
				ElectricFleetUtils.EV_ENGINE_HBEFA_TECHNOLOGY);
	}

	// GENERAL PART

	private double simulationStartTime = 0.0;
	private double simulationEndTime = 24.0 * 3600.0;

	public TestScenarioBuilder setSimulationStartTime(double simulationStartTime) {
		this.simulationStartTime = simulationStartTime;
		return this;
	}

	public TestScenarioBuilder setSimulationEndTime(double simulationEndTime) {
		this.simulationEndTime = simulationEndTime;
		return this;
	}

	private boolean enableStrategicCharging = false;
	private int iterations = 0;

	public TestScenarioBuilder enableStrategicCharging(int iterations) {
		this.enableStrategicCharging = true;
		this.iterations = iterations;
		return this;
	}

	private Config prepareConfig() {
		Config config = ConfigUtils.createConfig();

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		ActivityParams genericActivity = new ActivityParams("generic");
		genericActivity.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(genericActivity);

		config.controller().setLastIteration(iterations);

		config.qsim().setStartTime(simulationStartTime);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);

		config.qsim().setEndTime(simulationEndTime);
		config.qsim().setSimEndtimeInterpretation(EndtimeInterpretation.onlyUseEndtime);

		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);
		config.qsim().setVehicleBehavior(VehicleBehavior.exception);

		config.routing().setAccessEgressType(AccessEgressType.accessEgressModeToLink);

		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(homeParams);

		ActivityParams workParams = new ActivityParams("work");
		workParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(workParams);

		ActivityParams plugParams = new ActivityParams(WithinDayEvEngine.PLUG_ACTIVITY_TYPE);
		plugParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(plugParams);

		ActivityParams unplugParams = new ActivityParams(WithinDayEvEngine.UNPLUG_ACTIVITY_TYPE);
		unplugParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(unplugParams);

		ActivityParams accessParams = new ActivityParams(WithinDayEvEngine.ACCESS_ACTIVITY_TYPE);
		accessParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(accessParams);

		ActivityParams waitParams = new ActivityParams(WithinDayEvEngine.WAIT_ACTIVITY_TYPE);
		waitParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(waitParams);

		config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		EvConfigGroup evConfigGroup = new EvConfigGroup();
		config.addModule(evConfigGroup);
		evConfigGroup.chargersFile = "none";

		WithinDayEvConfigGroup withinDayConfig = new WithinDayEvConfigGroup();
		config.addModule(withinDayConfig);
		withinDayConfig.carMode = "car";

		if (enableStrategicCharging) {
			StrategicChargingConfigGroup strategicConfig = new StrategicChargingConfigGroup();
			strategicConfig.selectionStrategy = SelectionStrategy.Best; // to have a strong effect
			config.addModule(strategicConfig);

			ChargingPlanScoringParameters scoringParameters = new ChargingPlanScoringParameters();
			strategicConfig.addParameterSet(scoringParameters);

			// only strategic charging
			StrategySettings chargingStrategy = new StrategySettings();
			chargingStrategy.setStrategyName(StrategicChargingReplanningStrategy.STRATEGY);
			chargingStrategy.setWeight(1.0);
			config.replanning().addStrategySettings(chargingStrategy);
		}

		return config;
	}

	private Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.createScenario(config);
		prepareNetwork(scenario);
		prepareVehicles(scenario);
		preparePopulation(scenario);
		return scenario;
	}

	private Controler prepareController(Scenario scenario) {
		Controler controller = new Controler(scenario);

		controller.addOverridingModule(new EvModule());
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(DriveEnergyConsumption.Factory.class).toInstance(vehicle -> {
					return (link, travelTime, linkEnterTime) -> link.getLength();
				});

				bind(AuxEnergyConsumption.Factory.class).toInstance(vehicle -> {
					return (beginTime, duration, linkId) -> 0.0;
				});
			}
		});

		prepareInfrastructure(controller);

		controller.addOverridingModule(new WithinDayEvModule());

		if (enableStrategicCharging) {
			controller.addOverridingModule(new StrategicChargingModule());
		}

		return controller;
	}

	public TestScenario build() {
		Config config = prepareConfig();
		Scenario scenario = prepareScenario(config);
		Controler controller = prepareController(scenario);

		Tracker tracker = prepareTracker(controller);

		return new TestScenario(config, scenario, controller, tracker);
	}

	public record TestScenario(Config config, Scenario scenario, Controler controller, Tracker tracker) {
	}

	private Tracker prepareTracker(Controler controller) {
		Tracker tracker = new Tracker();

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(tracker);
			}
		});

		return tracker;
	}

	public class Tracker
			implements ActivityStartEventHandler, ActivityEndEventHandler, //
			StartChargingProcessEventHandler, AbortChargingProcessEventHandler,
			FinishChargingProcessEventHandler, //
			StartChargingAttemptEventHandler, UpdateChargingAttemptEventHandler, AbortChargingAttemptEventHandler,
			FinishChargingAttemptEventHandler, //
			ChargingStartEventHandler,
			ChargingEndEventHandler, QueuedAtChargerEventHandler, QuitQueueAtChargerEventHandler,
			PersonStuckEventHandler, PersonDepartureEventHandler {
		public final LinkedList<ActivityStartEvent> activityStartEvents = new LinkedList<>();
		public final LinkedList<ActivityStartEvent> plugActivityEvents = new LinkedList<>();
		public final LinkedList<ActivityStartEvent> unplugActivityEvents = new LinkedList<>();
		public final LinkedList<ActivityEndEvent> unplugActivityEndEvents = new LinkedList<>();

		public final LinkedList<StartChargingProcessEvent> startChargingProcessEvents = new LinkedList<>();
		public final LinkedList<AbortChargingProcessEvent> abortCharingProcessEvents = new LinkedList<>();
		public final LinkedList<FinishChargingProcessEvent> finishChargingProcessEvents = new LinkedList<>();

		public final LinkedList<StartChargingAttemptEvent> startChargingAttemptEvents = new LinkedList<>();
		public final LinkedList<UpdateChargingAttemptEvent> updateChargingAttemptEvents = new LinkedList<>();
		public final LinkedList<AbortChargingAttemptEvent> abortCharingAttemptEvents = new LinkedList<>();
		public final LinkedList<FinishChargingAttemptEvent> finishChargingAttemptEvents = new LinkedList<>();

		public final LinkedList<ChargingStartEvent> chargingStartEvents = new LinkedList<>();
		public final LinkedList<ChargingEndEvent> chargingEndEvents = new LinkedList<>();
		public final LinkedList<QueuedAtChargerEvent> queuedAtChargerEvents = new LinkedList<>();
		public final LinkedList<QuitQueueAtChargerEvent> quitQueueAtChargerEvents = new LinkedList<>();
		public final LinkedList<PersonStuckEvent> personStuckEvents = new LinkedList<>();

		public final IdMap<Person, List<String>> sequences = new IdMap<>(Person.class);

		@Override
		synchronized public void handleEvent(ActivityStartEvent event) {
			if (!TripStructureUtils.isStageActivityType(event.getActType()) || WithinDayEvEngine.isManagedActivityType(event.getActType())) {
				activityStartEvents.add(event);

				if (event.getActType().equals(WithinDayEvEngine.PLUG_ACTIVITY_TYPE)) {
					plugActivityEvents.add(event);
				} else if (event.getActType().equals(WithinDayEvEngine.UNPLUG_ACTIVITY_TYPE)) {
					unplugActivityEvents.add(event);
				}
			}

			synchronized (sequences) {
				if (!sequences.containsKey(event.getPersonId())) {
					sequences.put(event.getPersonId(), new LinkedList<>());
				}

				sequences.get(event.getPersonId()).add("activity:" + event.getActType());
			}
		}

		@Override
		synchronized public void handleEvent(ActivityEndEvent event) {
			if (event.getActType().equals(WithinDayEvEngine.UNPLUG_ACTIVITY_TYPE)) {
				unplugActivityEndEvents.add(event);
			}
		}

		@Override
		synchronized public void handleEvent(QuitQueueAtChargerEvent event) {
			quitQueueAtChargerEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(QueuedAtChargerEvent event) {
			queuedAtChargerEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(ChargingEndEvent event) {
			chargingEndEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(ChargingStartEvent event) {
			chargingStartEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(StartChargingProcessEvent event) {
			startChargingProcessEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(AbortChargingProcessEvent event) {
			abortCharingProcessEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(FinishChargingProcessEvent event) {
			finishChargingProcessEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(StartChargingAttemptEvent event) {
			startChargingAttemptEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(UpdateChargingAttemptEvent event) {
			updateChargingAttemptEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(AbortChargingAttemptEvent event) {
			abortCharingAttemptEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(FinishChargingAttemptEvent event) {
			finishChargingAttemptEvents.add(event);
		}

		@Override
		synchronized public void handleEvent(PersonStuckEvent event) {
			personStuckEvents.add(event);
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			synchronized (sequences) {
				if (!sequences.containsKey(event.getPersonId())) {
					sequences.put(event.getPersonId(), new LinkedList<>());
				}

				sequences.get(event.getPersonId()).add("leg:" + event.getLegMode());
			}
		}

		@Override
		public void reset(int iteration) {
			activityStartEvents.clear();
			plugActivityEvents.clear();
			unplugActivityEvents.clear();

			startChargingProcessEvents.clear();
			abortCharingProcessEvents.clear();
			finishChargingProcessEvents.clear();

			startChargingAttemptEvents.clear();
			updateChargingAttemptEvents.clear();
			abortCharingAttemptEvents.clear();
			finishChargingAttemptEvents.clear();

			chargingStartEvents.clear();
			chargingEndEvents.clear();
			queuedAtChargerEvents.clear();
			quitQueueAtChargerEvents.clear();
			personStuckEvents.clear();

			sequences.clear();
		}
	}
}
