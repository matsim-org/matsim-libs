package org.matsim.contrib.dvrp.router;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.Fleets;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Singleton;

/**
 * @author Sebastian Hörl (sebhoerl)
 */
public class DiversionTest {
	static public final String MODE = "testmode";
	static public final int DIVERSION_FREQUENCY = 10;

	@Test
	/**
	 * This is a relatively complex test to the the diversion behaviour. The
	 * experiment is constructed as folows: We have one DVRP vehicle which moves
	 * along 8 links (l0 to l8) with a distance of 1000m each and a odd freespeed to
	 * that the QSim will perform rounding.
	 * 
	 * To do so, before the simulation starts, the route from l0 to l8 is calculated
	 * and assigned to a drive task, which is added to the vehicle. Here, we already
	 * have an estimated for the arrival time, which should predict correctly the
	 * final arrival time of the vehicle at l8. This is the first fact to check.
	 * 
	 * After, we divert the vehicle every N seconds. To do so, we obtain the next
	 * possible diversion point from the drive task tracker and perform a routing
	 * from there to l8. Again, any time we do that, we should obtain the correct
	 * arrival time if prediction is correct.
	 * 
	 * State 1 Nov 2021 is the following: In this scenario:
	 * 
	 * <ul>
	 * <li>the vehicle arrives at the end of l8 at 657s (reference value).</li>
	 * <li>the initial routing predicts an arrival time of 656s (wrong, too
	 * quick)</li>
	 * <li>at every diversion the new routing gives an arrival time of 658s (wrong,
	 * too slow)</li>
	 * <li>once the vehicle enters l8 all remaining diversions give 657s
	 * (correct)</li>
	 * </ul>
	 * 
	 * Hypotheses for the time being:
	 * 
	 * <ul>
	 * <li>Different than stated in QSimFreeSpeedTravelTime, departing from the
	 * current activity takes 2s according to the events. One for "enters traffic",
	 * one for "leaves link".</li>
	 * <li>The change of behaviour for the diversion when entering the final link
	 * comes from the fallback in createPath.createPath with considerZeroLengthPath
	 * == true</li>
	 * <li>The difference between the initial routing and the diversion comes from
	 * the fact that when departing from an activity, we have the slack described
	 * above (agent needs to depart), but we do *not* have that when diverting. In
	 * that case, the agent will simply enter the next link after traversing the
	 * current one.</li>
	 * </ul>
	 */
	public void testDiversions() {
		Config config = ConfigUtils.createConfig();

		{
			/* Create some necessary configuration for the test */

			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setLastIteration(0);

			config.qsim().setStartTime(0.0);
			config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);

			DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
			config.addModule(dvrpConfigGroup);
		}

		Scenario scenario = ScenarioUtils.createScenario(config);

		{
			/*
			 * Create a network of 10 nodes and 9 links along a line with distance 1000m
			 * each and odd freespeed of 12.23 to make sure we're using the correct
			 * QSim-based freespeed and get corrected travel time values.
			 */

			Network network = scenario.getNetwork();
			NetworkFactory networkFactory = network.getFactory();

			for (int i = 0; i < 10; i++) {
				Node node = networkFactory.createNode(Id.createNodeId("n" + i), new Coord(0.0, 1000.0));
				network.addNode(node);
			}

			for (int i = 0; i < 9; i++) {
				Node fromNode = network.getNodes().get(Id.createNodeId("n" + i));
				Node toNode = network.getNodes().get(Id.createNodeId("n" + (i + 1)));

				Link link = networkFactory.createLink(Id.createLinkId("l" + i), fromNode, toNode);
				link.setFreespeed(12.23);
				link.setCapacity(1e9);
				link.setLength(1000.0);
				network.addLink(link);
			}
		}

		FleetSpecification fleetSpecification = new FleetSpecificationImpl();

		{
			/* Create fleet specification of one vehicle at the first link (id = l0) */

			fleetSpecification.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder() //
					.capacity(4) //
					.id(Id.create("vehicle", DvrpVehicle.class)) //
					.serviceBeginTime(0.0) //
					.serviceEndTime(2000.0) //
					.startLinkId(Id.createLinkId("l0")) //
					.build());
		}

		// Set up tracker
		TestTracker testTracker = new TestTracker();

		// Create controller with DVRP
		Controler controller = new Controler(scenario);
		controller.addOverridingModule(new DvrpModule());

		// Add bindings for test case
		controller.addOverridingModule(new TestModeModule(testTracker));
		controller.addOverridingQSimModule(new TestModeQSimModule(fleetSpecification, testTracker));

		// Active test mode and run
		controller.configureQSimComponents(DvrpQSimComponents.activateModes(MODE));
		controller.run();

		// Log results

		System.out.println("Arrival in simulation acc. to events: " + testTracker.eventBasedArrivalTime);
		System.out.println("Predicted arrival time when preparing drive task: " + testTracker.initialArrivalTime);
		System.out.println("Predicted arrival time for diversions: " + testTracker.diversionArrivalTimes);

		// Took the fixed value from the simulation, to make sure this stays consistent
		// over refactorings
		assertThat(testTracker.eventBasedArrivalTime).isEqualTo(657.0);

		// Initially calculated arrival time should predict correctly the final arrival
		// time
		assertThat(testTracker.initialArrivalTime).isEqualTo(657.0);

		// Along the route, when diverting to the same destination, arrival time should
		// stay constant
		testTracker.diversionArrivalTimes.forEach(t -> {
			assertThat(t).isEqualTo(657.0);
		});
	}

	static private class TestModeModule extends AbstractDvrpModeModule {
		private final TestTracker testTracker;

		public TestModeModule(TestTracker testTracker) {
			super(MODE);

			this.testTracker = testTracker;
		}

		@Override
		public void install() {
			/*
			 * Standard bindings for routing (see OneTaxi)
			 */

			DvrpModes.registerDvrpMode(binder(), getMode());
			install(new DvrpModeRoutingNetworkModule(getMode(), false));

			/*
			 * Add handler to track the actual arrival time in simulation
			 */
			addEventHandlerBinding().toInstance(new ArrivalHandler(testTracker));

			// TODO SIPLIFY
			bindModal(PassengerRequestValidator.class).toInstance(new PassengerRequestValidator() {
				@Override
				public Set<String> validateRequest(PassengerRequest request) {
					return null;
				}
			});
		}
	}

	static private class TestModeQSimModule extends AbstractDvrpModeQSimModule {
		private final FleetSpecification fleetSpecification;
		private final TestTracker testTracker;

		protected TestModeQSimModule(FleetSpecification fleetSpecification, TestTracker testTracker) {
			super(MODE);

			this.fleetSpecification = fleetSpecification;
			this.testTracker = testTracker;
		}

		@Override
		protected void configureQSim() {
			/*
			 * Bind the fleet and agent source
			 */
			bindModal(Fleet.class).toProvider(modalProvider(getter -> {
				return Fleets.createDefaultFleet(fleetSpecification, getter.getModal(Network.class).getLinks()::get);
			})).in(Singleton.class);

			install(new VrpAgentSourceQSimModule(getMode()));

			/*
			 * Create the simplest possible DynActionCreator, which simply creates a VrpLeg
			 * with online drive tracker from the only drive task that will be put in the
			 * schedule by the TestOptimizer
			 */
			bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(modalProvider(getter -> {
				return (DynAgent dynAgent, DvrpVehicle vehicle, double now) -> {
					return VrpLegFactory.createWithOnlineTracker(TransportMode.car, vehicle,
							(DvrpVehicle v, Link nextLink) -> {
							}, getter.get(MobsimTimer.class));
				};
			}));

			/*
			 * Bind TestOptimizer
			 */
			bindModal(VrpOptimizer.class).to(TestOptimizer.class);
			addModalComponent(TestOptimizer.class);

			bind(TestOptimizer.class).toProvider(modalProvider(getter -> {
				Network network = getter.getModal(Network.class);
				MobsimTimer timer = getter.get(MobsimTimer.class);
				Fleet fleet = getter.getModal(Fleet.class);

				return new TestOptimizer(timer, network, fleet, getConfig().qsim(), testTracker);
			})).in(Singleton.class);
		}
	}

	static private class TestOptimizer implements VrpOptimizer, MobsimBeforeSimStepListener {
		private final MobsimTimer timer;

		private final TravelTime travelTime;

		private final LeastCostPathCalculator router;
		private final DvrpVehicle vehicle;

		private final Link startLink;
		private final Link endLink;

		private TestTracker testTracker;

		public TestOptimizer(MobsimTimer timer, Network network, Fleet fleet, QSimConfigGroup qsimConfig,
				TestTracker testTracker) {
			this.timer = timer;
			this.testTracker = testTracker;

			this.travelTime = new QSimFreeSpeedTravelTime(qsimConfig);
			TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

			this.router = new DijkstraFactory().createPathCalculator(network, travelDisutility, travelTime);
			this.vehicle = fleet.getVehicles().values().iterator().next();

			/*
			 * We save the links. We want to go from L0 to L8
			 */
			this.startLink = Objects.requireNonNull(network.getLinks().get(Id.createLinkId("l0")));
			this.endLink = Objects.requireNonNull(network.getLinks().get(Id.createLinkId("l8")));

			/*
			 * Prepare vehicle schedule by adding drive task
			 */
			insertDriveTask();
		}

		private void insertDriveTask() {
			Schedule schedule = vehicle.getSchedule();

			/*
			 * Here we perform an initial routing at time 0.0 from the chosen start link to
			 * the end link
			 */
			VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(startLink, endLink, timer.getTimeOfDay(), router,
					travelTime);

			// ... and we add it as the first and only task to the schedule
			DriveTask driveTask = new DriveTask(() -> "drive", path);
			schedule.addTask(driveTask);

			// Track the initially obtained arrival time
			testTracker.initialArrivalTime = path.getArrivalTime();
		}

		@Override
		public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
			if (vehicle.getSchedule().getStatus().equals(ScheduleStatus.STARTED)) {
				if (timer.getTimeOfDay() % DIVERSION_FREQUENCY == 0) {
					Task currentTask = vehicle.getSchedule().getCurrentTask();

					if (currentTask instanceof DriveTask) {
						/*
						 * Every N seconds, we perform a diversion. For that, we use the current
						 * diversion point as given by the trask tracker and we route from this
						 * diversion point to the end link.
						 */

						// (1) Obtain task tracker from the active drive task
						OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) currentTask.getTaskTracker();
						LinkTimePair diversionPoint = tracker.getDiversionPoint();

						// (2) Perform rerouting from the diversion point
						VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(diversionPoint, endLink, router,
								travelTime);

						// (3) Implement the diversion
						tracker.divertPath(path);

						// (4) Track the currently estimated arrival time
						this.testTracker.diversionArrivalTimes.add(path.getArrivalTime());
					}
				}
			}
		}

		@Override
		public void requestSubmitted(Request request) {
		}

		@Override
		public void nextTask(DvrpVehicle vehicle) {
			vehicle.getSchedule().nextTask(); // Just advance
		}
	}

	static private class ArrivalHandler implements PersonArrivalEventHandler {
		private final TestTracker testTracker;

		ArrivalHandler(TestTracker testTracker) {
			this.testTracker = testTracker;
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			testTracker.eventBasedArrivalTime = event.getTime();
		}
	}

	static private class TestTracker {
		private double initialArrivalTime;
		private double eventBasedArrivalTime;
		private List<Double> diversionArrivalTimes = new LinkedList<>();
	}
}
