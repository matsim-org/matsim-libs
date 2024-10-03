package org.matsim.contrib.drt.taas;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
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
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsParams;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.prebooking.PassengerRequestBookedEvent;
import org.matsim.contrib.drt.prebooking.PassengerRequestBookedEventHandler;
import org.matsim.contrib.drt.prebooking.PrebookingParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.google.common.base.Verify;

public class TaasScenarioBuilder {
	private final MatsimTestUtils utils;

	public TaasScenarioBuilder(MatsimTestUtils utils) {
		this.utils = utils;
	}

	// NETWORK PART

	private Integer networkSize = 10; // nodes
	private Double linkLength = 200.0; // meters
	private Double linkSpeed = 1.0; // meters per second

	public TaasScenarioBuilder setNetworkSize(int networkSize) {
		this.networkSize = networkSize;
		return this;
	}

	public TaasScenarioBuilder setLinkLength(double linkLength) {
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
	}

	// SERVICE PART

	record VehicleItem(String identifier, int depotX, int depotY, DvrpVehicleLoad capacity) {
	}

	private List<VehicleItem> vehicles = new LinkedList<>();

	public TaasScenarioBuilder addVehicle(String identifier, int depotX, int depotY, DvrpVehicleLoad capacity) {
		vehicles.add(new VehicleItem(identifier, depotX, depotY, capacity));
		return this;
	}

	private String mode = "drt";

	public TaasScenarioBuilder setMode(String mode) {
		this.mode = mode;
		return this;
	}

	double passengerMaxWaitTime = 300.0;
	double passengerMaxDelay = 600.0;
	double parcelLatestDeliveryTime = 20.0 * 3600.0;

	public TaasScenarioBuilder setPassengerParameters(double maxWaitTime, double maxDelay) {
		this.passengerMaxWaitTime = maxWaitTime;
		this.passengerMaxDelay = maxDelay;
		return this;
	}

	public TaasScenarioBuilder setParcelParameters(double latestDeliveryTime) {
		this.parcelLatestDeliveryTime = latestDeliveryTime;
		return this;
	}

	private TaasServiceModule prepareService(Controler controller) {
		FleetSpecificationImpl fleet = new FleetSpecificationImpl();

		for (VehicleItem item : vehicles) {
			Verify.verify(item.depotX >= 0 && item.depotX < networkSize - 1,
					"Invalid depot for vehicle " + item.identifier);
			Verify.verify(item.depotY >= 0 && item.depotY < networkSize - 1,
					"Invalid depot for vehicle " + item.identifier);

			Id<Link> depotLinkId = Id
					.createLinkId(item.depotX + ":" + item.depotY + "::" + (item.depotX + 1) + ":" + item.depotY);

			fleet.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder() //
					.id(Id.create(item.identifier, DvrpVehicle.class)) //
					.capacity(item.capacity) //
					.serviceBeginTime(simulationStartTime) //
					.serviceEndTime(simulationEndTime) //
					.startLinkId(depotLinkId) //
					.build());
		}

		controller.addOverridingModule(new AbstractDvrpModeModule(mode) {
			@Override
			public void install() {
				bindModal(FleetSpecification.class).toInstance(fleet);
			}
		});

		TaasServiceModule serviceModule = new TaasServiceModule(mode);
		controller.addOverridingModule(serviceModule);
		return serviceModule;
	}

	// DEMAND PART

	record DemandItem(String requestType, String identifer, double departureTime, int originX, int originY,
			int destinationX, int destinationY) {
	}

	private List<DemandItem> demand = new LinkedList<>();

	public TaasScenarioBuilder addDemand(String requestType, String identifier, double departureTime, int originX,
			int originY, int destinationX, int destinationY) {
		demand.add(
				new DemandItem(requestType, identifier, departureTime, originX, originY, destinationX, destinationY));
		return this;
	}

	private void preparePopulation(Scenario scenario) {
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		for (var item : demand) {
			Verify.verify(item.originX >= 0 && item.originX < networkSize - 1,
					"Invalid origin for demand " + item.identifer);
			Verify.verify(item.originY >= 0 && item.originY < networkSize - 1,
					"Invalid origin for demand " + item.identifer);
			Verify.verify(item.destinationX >= 0 && item.destinationX < networkSize - 1,
					"Invalid destination for demand " + item.identifer);
			Verify.verify(item.destinationY >= 0 && item.destinationY < networkSize - 1,
					"Invalid destination for demand " + item.identifer);

			Person person = populationFactory.createPerson(Id.createPersonId(item.identifer));
			person.getAttributes().putAttribute(TaasDrtRouteCreator.REQUEST_TYPE, item.requestType);
			population.addPerson(person);

			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);

			Id<Link> firstLinkId = Id
					.createLinkId(item.originX + ":" + item.originY + "::" + (item.originX + 1) + ":" + item.originY);
			Activity firstActivity = populationFactory.createActivityFromLinkId("generic", firstLinkId);
			firstActivity.setEndTime(item.departureTime);
			plan.addActivity(firstActivity);

			Leg leg = populationFactory.createLeg(mode);
			leg.setRoutingMode(mode);
			plan.addLeg(leg);

			Id<Link> secondLinkId = Id.createLinkId(item.destinationX + ":" + item.destinationY + "::"
					+ (item.destinationX + 1) + ":" + item.destinationY);
			Activity secondActivity = populationFactory.createActivityFromLinkId("generic", secondLinkId);
			plan.addActivity(secondActivity);
		}
	}

	// GENERAL PART

	private double simulationStartTime = 0.0;
	private double simulationEndTime = 10.0 * 3600.0;

	public TaasScenarioBuilder setSimulationStartTime(double simulationStartTime) {
		this.simulationStartTime = simulationStartTime;
		return this;
	}

	public TaasScenarioBuilder setSimulationEndTime(double simulationEndTime) {
		this.simulationEndTime = simulationEndTime;
		return this;
	}

	private Config prepareConfig() {
		Config config = ConfigUtils.createConfig();

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		ActivityParams genericActivity = new ActivityParams("generic");
		genericActivity.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(genericActivity);

		ModeParams modeParams = new ModeParams(mode);
		config.scoring().addModeParams(modeParams);

		config.controller().setLastIteration(0);

		config.qsim().setStartTime(simulationStartTime);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);

		config.qsim().setEndTime(simulationEndTime);
		config.qsim().setSimEndtimeInterpretation(EndtimeInterpretation.onlyUseEndtime);

		DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
		config.addModule(dvrpConfig);

		SquareGridZoneSystemParams zoneSystemParams = new SquareGridZoneSystemParams();
		zoneSystemParams.cellSize = 100.0;

		DvrpTravelTimeMatrixParams matrixParams = DvrpConfigGroup.get(config).getTravelTimeMatrixParams();
		matrixParams.addParameterSet(zoneSystemParams);

		MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
		config.addModule(multiModeDrtConfig);

		DrtConfigGroup drtConfig = new DrtConfigGroup();
		multiModeDrtConfig.addParameterSet(drtConfig);

		drtConfig.mode = "drt";
		drtConfig.stopDuration = 30.0;

		DrtInsertionSearchParams insertionParams = new ExtensiveInsertionSearchParams();
		drtConfig.addParameterSet(insertionParams);

		DrtOptimizationConstraintsParams constraintsContainer = new DrtOptimizationConstraintsParams();
		drtConfig.addParameterSet(constraintsContainer);

		DefaultDrtOptimizationConstraintsSet constraints = new DefaultDrtOptimizationConstraintsSet();
		constraints.name = "default";
		constraints.maxWaitTime = 300.0;
		constraints.maxTravelTimeBeta = 1.0;
		constraints.maxTravelTimeAlpha = 600.0;
		constraintsContainer.addParameterSet(constraints);

		PrebookingParams prebookingParams = new PrebookingParams();
		drtConfig.addParameterSet(prebookingParams);

		return config;
	}

	private Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.createScenario(config);

		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
				new DrtRouteFactory());

		prepareNetwork(scenario);
		preparePopulation(scenario);
		return scenario;
	}

	private Controler prepareController(Scenario scenario) {
		Controler controller = new Controler(scenario);
		controller.configureQSimComponents(DvrpQSimComponents.activateModes(mode));
		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new MultiModeDrtModule());
		return controller;
	}

	public TestScenario build() {
		Config config = prepareConfig();
		Scenario scenario = prepareScenario(config);
		Controler controller = prepareController(scenario);

		TaasServiceModule service = prepareService(controller);
		Tracker tracker = prepareTracker(controller);

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);
		return new TestScenario(config, scenario, controller, tracker, drtConfig, service);
	}

	public record TestScenario(Config config, Scenario scenario, Controler controller, Tracker tracker,
			DrtConfigGroup drt, TaasServiceModule service) {
	}

	private Tracker prepareTracker(Controler controller) {
		Tracker tracker = new Tracker(controller.getScenario().getPopulation());

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(tracker);
			}
		});

		return tracker;
	}

	public class RequestInformation {
		public double departureTime = Double.NaN;
		public double submissionTime = Double.NaN;
		public double bookingTime = Double.NaN;
		public double scheduledTime = Double.NaN;
		public double pickupTime = Double.NaN;
		public double dropoffTime = Double.NaN;
		public double rejectionTime = Double.NaN;
	}

	public class Tracker implements PassengerRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler,
			PassengerRequestBookedEventHandler, PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler,
			PassengerRequestScheduledEventHandler, PersonDepartureEventHandler {
		private final Map<String, RequestInformation> data = new HashMap<>();

		Tracker(Population population) {
			for (Person person : population.getPersons().values()) {
				data.put(person.getId().toString(), new RequestInformation());
			}
		}

		public RequestInformation getInformation(String identifier) {
			return data.get(identifier);
		}

		@Override
		public void handleEvent(PassengerDroppedOffEvent event) {
			if (!data.containsKey(event.getPersonId().toString()))
				return;
			data.get(event.getPersonId().toString()).dropoffTime = event.getTime();
		}

		@Override
		public void handleEvent(PassengerPickedUpEvent event) {
			if (!data.containsKey(event.getPersonId().toString()))
				return;
			data.get(event.getPersonId().toString()).pickupTime = event.getTime();
		}

		@Override
		public void handleEvent(PassengerRequestBookedEvent event) {
			for (Id<Person> personId : event.getPersonIds()) {
				if (!data.containsKey(personId.toString()))
					continue;
				data.get(personId.toString()).bookingTime = event.getTime();
			}
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			for (Id<Person> personId : event.getPersonIds()) {
				if (!data.containsKey(personId.toString()))
					continue;
				data.get(personId.toString()).rejectionTime = event.getTime();
			}
		}

		@Override
		public void handleEvent(PassengerRequestSubmittedEvent event) {
			for (Id<Person> personId : event.getPersonIds()) {
				if (!data.containsKey(personId.toString()))
					continue;
				data.get(personId.toString()).submissionTime = event.getTime();
			}
		}

		@Override
		public void handleEvent(PassengerRequestScheduledEvent event) {
			for (Id<Person> personId : event.getPersonIds()) {
				if (!data.containsKey(personId.toString()))
					continue;
				data.get(personId.toString()).scheduledTime = event.getTime();
			}
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (!data.containsKey(event.getPersonId().toString()))
				return;
			data.get(event.getPersonId().toString()).departureTime = event.getTime();
		}
	}
}
