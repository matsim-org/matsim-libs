package org.matsim.contrib.drt.prebooking;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.prebooking.logic.AttributeBasedPrebookingLogic;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PrebookingTestEnvironment {
	private final MatsimTestUtils utils;

	private final int width = 10;
	private final int height = 10;

	private final double edgeLength = 200.0;
	private final double speed = 10.0;

	private int vehicleCapacity = 4;

	private double maximumWaitTime = 3600.0;
	private double detourRelative = 1.3;
	private double detourAbsolute = 300.0;
	private double stopDuration = 60.0;
	private double endTime = 30.0 * 3600.0;

	public PrebookingTestEnvironment(MatsimTestUtils utils) {
		this.utils = utils;
	}

	public PrebookingTestEnvironment configure(double maximumWaitTime, double detourRelative, double detourAbsolute,
			double stopDuration) {
		this.maximumWaitTime = maximumWaitTime;
		this.detourRelative = detourRelative;
		this.detourAbsolute = detourAbsolute;
		this.stopDuration = stopDuration;
		return this;
	}

	public PrebookingTestEnvironment endTime(double endTime) {
		this.endTime = endTime;
		return this;
	}

	private class Request {
		String personId;

		Pair<Integer, Integer> origin = null;
		Pair<Integer, Integer> destination = null;

		double departureTime;
		double submissionTime;
		double plannedDepartureTime;
	}

	List<Request> requests = new LinkedList<>();

	public PrebookingTestEnvironment addRequest(String personId, int originX, int originY, int destinationX,
			int destinationY, double departureTime, double submissionTime, double plannedDepartureTime) {
		Request request = new Request();
		request.personId = personId;
		request.origin = Pair.of(originX, originY);
		request.destination = Pair.of(destinationX, destinationY);
		request.departureTime = departureTime;
		request.submissionTime = submissionTime;
		request.plannedDepartureTime = plannedDepartureTime;
		requests.add(request);
		return this;
	}

	public PrebookingTestEnvironment addRequest(String personId, int originX, int originY, int destinationX,
			int destinationY, double departureTime, double submissionTime) {
		return addRequest(personId, originX, originY, destinationX, destinationY, departureTime, submissionTime,
				Double.NaN);
	}

	public PrebookingTestEnvironment addRequest(String personId, int originX, int originY, int destinationX,
			int destinationY, double departureTime) {
		return addRequest(personId, originX, originY, destinationX, destinationY, departureTime, Double.NaN,
				Double.NaN);
	}

	private class Vehicle {
		String vehicleId;
		Pair<Integer, Integer> depot;
	}

	private List<Vehicle> vehicles = new LinkedList<>();

	public PrebookingTestEnvironment addVehicle(String vehicleId, int depotX, int depotY) {
		Vehicle vehicle = new Vehicle();
		vehicle.vehicleId = vehicleId;
		vehicle.depot = Pair.of(depotX, depotY);
		vehicles.add(vehicle);
		return this;
	}

	public PrebookingTestEnvironment setVehicleCapacity(int vehicleCapacity) {
		this.vehicleCapacity = vehicleCapacity;
		return this;
	}

	public Controler build() {
		Config config = ConfigUtils.createConfig();
		buildConfig(config);

		Scenario scenario = ScenarioUtils.createScenario(config);

		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
				new DrtRouteFactory());

		buildNetwork(scenario);
		buildPopulation(scenario);

		Controler controller = new Controler(scenario);
		buildController(controller);
		buildFleet(controller);

		configureRequestListener(controller);
		configureVehicleListener(controller);

		return controller;
	}

	private void buildFleet(Controler controller) {
		FleetSpecification fleetSpecification = new FleetSpecificationImpl();

		for (Vehicle vehicle : vehicles) {
			fleetSpecification.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder() //
					.id(Id.create(vehicle.vehicleId, DvrpVehicle.class)) //
					.capacity(vehicleCapacity) //
					.serviceBeginTime(0.0) //
					.serviceEndTime(endTime) //
					.startLinkId(createLinkId(createNodeId(vehicle.depot.getLeft(), vehicle.depot.getRight()),
							createNodeId(vehicle.depot.getLeft() + 1, vehicle.depot.getRight()))) //
					.build());
		}

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				bindModal(FleetSpecification.class).toInstance(fleetSpecification);
			}
		});
	}

	private void buildController(Controler controller) {
		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new MultiModeDrtModule());

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = MultiModeDrtConfigGroup.get(controller.getConfig());
		controller.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfigGroup));
	}

	private void buildConfig(Config config) {
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);

		config.qsim().setStartTime(0.0);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);

		config.qsim().setEndTime(endTime);
		config.qsim().setSimEndtimeInterpretation(EndtimeInterpretation.onlyUseEndtime);

		ModeParams drtParams = new ModeParams("drt");
		config.scoring().addModeParams(drtParams);

		ActivityParams genericParams = new ActivityParams("generic");
		genericParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(genericParams);

		DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
		config.addModule(dvrpConfig);

		MultiModeDrtConfigGroup drtConfig = new MultiModeDrtConfigGroup();
		config.addModule(drtConfig);

		DrtConfigGroup modeConfig = new DrtConfigGroup();
		drtConfig.addParameterSet(modeConfig);
		modeConfig.mode = "drt";
		modeConfig.maxWaitTime = maximumWaitTime;
		modeConfig.maxTravelTimeAlpha = detourRelative;
		modeConfig.maxTravelTimeBeta = detourAbsolute;
		modeConfig.stopDuration = stopDuration;
		modeConfig.idleVehiclesReturnToDepots = false;
		modeConfig.vehiclesFile = null;

		DrtInsertionSearchParams searchParams = new SelectiveInsertionSearchParams();
		modeConfig.addDrtInsertionSearchParams(searchParams);

		DrtConfigs.adjustMultiModeDrtConfig(drtConfig, config.scoring(), config.routing());
	}

	private void buildPopulation(Scenario scenario) {
		Population population = scenario.getPopulation();
		PopulationFactory factory = population.getFactory();

		for (Request request : requests) {
			Id<Person> personId = Id.createPersonId(request.personId);

			Id<Link> originLinkId = createLinkId(createNodeId(request.origin.getLeft(), request.origin.getRight()),
					createNodeId(request.origin.getLeft() + 1, request.origin.getRight()));
			Id<Link> destinationLinkId = createLinkId(
					createNodeId(request.destination.getLeft(), request.destination.getRight()),
					createNodeId(request.destination.getLeft() + 1, request.destination.getRight()));

			Person person = factory.createPerson(personId);
			population.addPerson(person);

			Plan plan = factory.createPlan();
			person.addPlan(plan);

			Activity firstActivity = factory.createActivityFromLinkId("generic", originLinkId);
			plan.addActivity(firstActivity);
			firstActivity.setEndTime(request.departureTime);

			Leg firstLeg = factory.createLeg("drt");
			plan.addLeg(firstLeg);

			if (!Double.isNaN(request.submissionTime)) {
				firstActivity.getAttributes().putAttribute(AttributeBasedPrebookingLogic.getSubmissionAttribute("drt"),
						request.submissionTime);
			}

			if (!Double.isNaN(request.plannedDepartureTime)) {
				firstActivity.getAttributes().putAttribute(
						AttributeBasedPrebookingLogic.getPlannedDepartureAttribute("drt"),
						request.plannedDepartureTime);
			}

			Activity secondActivity = factory.createActivityFromLinkId("generic", destinationLinkId);
			plan.addActivity(secondActivity);
		}
	}

	private Id<Node> createNodeId(int i, int j) {
		return Id.createNodeId(i + ":" + j);
	}

	private Id<Link> createLinkId(Id<Node> startId, Id<Node> endId) {
		return Id.createLinkId(startId.toString() + "-" + endId.toString());
	}

	private void buildNetwork(Scenario scenario) {
		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();

		Node[][] nodes = new Node[width][height];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				nodes[i][j] = factory.createNode(createNodeId(i, j), new Coord(i * edgeLength, j * edgeLength));
				network.addNode(nodes[i][j]);
			}
		}

		// Horizontal
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width - 1; i++) {
				Node firstNode = nodes[i][j];
				Node secondNode = nodes[i + 1][j];

				Id<Link> forwardId = createLinkId(firstNode.getId(), secondNode.getId());
				Id<Link> backwardId = createLinkId(secondNode.getId(), firstNode.getId());

				Link forwardLink = factory.createLink(forwardId, firstNode, secondNode);
				Link backwardLink = factory.createLink(backwardId, secondNode, firstNode);

				network.addLink(forwardLink);
				network.addLink(backwardLink);
			}
		}

		// Vertical
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height - 1; j++) {
				Node firstNode = nodes[i][j];
				Node secondNode = nodes[i][j + 1];

				Id<Link> forwardId = createLinkId(firstNode.getId(), secondNode.getId());
				Id<Link> backwardId = createLinkId(secondNode.getId(), firstNode.getId());

				Link forwardLink = factory.createLink(forwardId, firstNode, secondNode);
				Link backwardLink = factory.createLink(backwardId, secondNode, firstNode);

				network.addLink(forwardLink);
				network.addLink(backwardLink);
			}
		}

		// Defaults
		for (Link link : network.getLinks().values()) {
			link.setFreespeed(speed);
			link.setCapacity(1e9);
			link.setLength(edgeLength);
			link.setNumberOfLanes(1.0);
		}
	}

	// ANALYSIS PART

	public class RequestInfo {
		public boolean rejected = false;

		public double submissionTime = Double.NaN;
		public double pickupTime = Double.NaN;
		public double dropoffTime = Double.NaN;

		public List<Double> submissionTimes = new LinkedList<>();
		public Id<org.matsim.contrib.dvrp.optimizer.Request> drtRequestId = null;
	}

	private Map<String, RequestInfo> requestInfo = new HashMap<>();

	public Map<String, RequestInfo> getRequestInfo() {
		return requestInfo;
	}

	private void configureRequestListener(Controler controller) {
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(new RequestListener());
			}
		});
	}

	private class RequestListener implements DrtRequestSubmittedEventHandler, PassengerPickedUpEventHandler,
			PassengerDroppedOffEventHandler, PassengerRequestRejectedEventHandler {
		@Override
		public void handleEvent(DrtRequestSubmittedEvent event) {
			for (Id<Person> personId : event.getPersonIds()) {
				requestInfo.computeIfAbsent(personId.toString(), id -> new RequestInfo()).submissionTime = event
						.getTime();
				requestInfo.computeIfAbsent(personId.toString(), id -> new RequestInfo()).drtRequestId = event
						.getRequestId();
				requestInfo.computeIfAbsent(personId.toString(), id -> new RequestInfo()).submissionTimes
						.add(event.getTime());
			}
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			for (Id<Person> personId : event.getPersonIds()) {
				requestInfo.computeIfAbsent(personId.toString(), id -> new RequestInfo()).rejected = true;
			}
		}

		@Override
		public void handleEvent(PassengerPickedUpEvent event) {
			requestInfo.computeIfAbsent(event.getPersonId().toString(), id -> new RequestInfo()).pickupTime = event
					.getTime();
		}

		@Override
		public void handleEvent(PassengerDroppedOffEvent event) {
			requestInfo.computeIfAbsent(event.getPersonId().toString(), id -> new RequestInfo()).dropoffTime = event
					.getTime();
		}
	}

	public class TaskInfo {
		public double startTime = Double.NaN;
		public double endTime = Double.NaN;
		public String type;
	}

	private Map<String, LinkedList<TaskInfo>> taskInfo = new HashMap<>();

	public Map<String, LinkedList<TaskInfo>> getTaskInfo() {
		return taskInfo;
	}

	private void configureVehicleListener(Controler controller) {
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(new VehicleListener());
			}
		});
	}

	private class VehicleListener implements TaskStartedEventHandler, TaskEndedEventHandler {
		@Override
		public void handleEvent(TaskStartedEvent event) {
			taskInfo.computeIfAbsent(event.getDvrpVehicleId().toString(), id -> new LinkedList<>()).add(new TaskInfo());
			taskInfo.get(event.getDvrpVehicleId().toString()).getLast().startTime = event.getTime();
			taskInfo.get(event.getDvrpVehicleId().toString()).getLast().type = event.getTaskType().name();
		}

		@Override
		public void handleEvent(TaskEndedEvent event) {
			taskInfo.get(event.getDvrpVehicleId().toString()).getLast().endTime = event.getTime();
		}
	}
}
