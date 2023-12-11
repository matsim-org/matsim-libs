package org.matsim.contrib.drt.extension.insertion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.insertion.distances.DistanceApproximator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerWaitingEvent;
import org.matsim.contrib.dvrp.passenger.PassengerWaitingEventHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class DrtInsertionExtensionIT {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private Controler createController() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		return DrtControlerCreator.createControler(config, false);
	}

	@Test
	void testExclusivityContraint() {
		IdSet<Request> exclusiveRequestIds = new IdSet<>(Request.class);

		for (int i = 100; i <= 200; i++) {
			exclusiveRequestIds.add(Id.create("drt_" + i, Request.class));
		}

		Controler controller = createController();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());

		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig)
				.withExclusivity(request -> exclusiveRequestIds.contains(request.getId()));

		controller.addOverridingQSimModule(insertionModule);

		ExclusiveRequestsHandler handler = new ExclusiveRequestsHandler();
		handler.install(controller);

		controller.run();

		IdSet<Request> unsharedRequestIds = handler.getUnsharedRequestIds();
		assertTrue(unsharedRequestIds.containsAll(exclusiveRequestIds));
	}

	static public class ExclusiveRequestsHandler implements PassengerPickedUpEventHandler,
			PassengerDroppedOffEventHandler, PassengerRequestSubmittedEventHandler {
		private final IdMap<Request, RideInterval> rideIntervals = new IdMap<>(Request.class);
		private final IdSet<Request> rejectedIds = new IdSet<>(Request.class);

		@Override
		public void handleEvent(PassengerPickedUpEvent event) {
			RideInterval interval = new RideInterval();
			interval.requestId = event.getRequestId();
			interval.vehicleId = event.getVehicleId();
			interval.startTime = event.getTime();
			rideIntervals.put(event.getRequestId(), interval);
		}

		@Override
		public void handleEvent(PassengerDroppedOffEvent event) {
			rideIntervals.get(event.getRequestId()).endTime = event.getTime();
		}

		@Override
		public void handleEvent(PassengerRequestSubmittedEvent event) {
			rejectedIds.add(event.getRequestId());
		}

		static class RideInterval {
			Id<Request> requestId;
			Id<DvrpVehicle> vehicleId;
			double startTime;
			double endTime;
		}

		public IdSet<Request> getUnsharedRequestIds() {
			List<RideInterval> rideList = new ArrayList<>(rideIntervals.values());

			IdSet<Request> unsharedRequestIds = new IdSet<>(Request.class);
			rideList.forEach(interval -> unsharedRequestIds.add(interval.requestId));

			for (int i = 0; i < rideList.size(); i++) {
				for (int j = i + 1; j < rideList.size(); j++) {
					RideInterval firstInterval = rideList.get(i);
					RideInterval secondInterval = rideList.get(j);

					if (firstInterval.startTime >= secondInterval.startTime
							&& firstInterval.startTime <= secondInterval.endTime) {
						unsharedRequestIds.remove(firstInterval.requestId);
						unsharedRequestIds.remove(secondInterval.requestId);
					}

					if (firstInterval.endTime >= secondInterval.startTime
							&& firstInterval.endTime <= secondInterval.endTime) {
						unsharedRequestIds.remove(firstInterval.requestId);
						unsharedRequestIds.remove(secondInterval.requestId);
					}
				}
			}

			unsharedRequestIds.addAll(rejectedIds);
			return unsharedRequestIds;
		}

		public IdSet<Request> getSharedRequestIds() {
			IdSet<Request> sharedRequestIds = new IdSet<>(Request.class);
			rideIntervals.values().forEach(interval -> sharedRequestIds.add(interval.requestId));

			sharedRequestIds.removeAll(getUnsharedRequestIds());
			return sharedRequestIds;
		}

		public void install(Controler controller) {
			ExclusiveRequestsHandler self = this;

			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(self);
				}
			});
		}
	}

	@Test
	void testSkillsConstraint() {
		IdSet<Request> restrictedRequestIds = new IdSet<>(Request.class);
		for (int i = 100; i <= 200; i++) {
			restrictedRequestIds.add(Id.create("drt_" + i, Request.class));
		}

		Controler controller = createController();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());

		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig).withSkills(
				v -> v.getId().toString().endsWith("_1") ? Set.of("skill") : null,
				r -> restrictedRequestIds.contains(r.getId()) ? Set.of("skill") : null);

		controller.addOverridingQSimModule(insertionModule);

		SkillsHandler handler = new SkillsHandler();
		handler.install(controller);

		controller.run();

		int totalRequests = 0;
		int restrictedRequests = 0;
		int validMatchings = 0;
		int invalidMatchings = 0;

		for (var item : handler.vehicles.entrySet()) {
			totalRequests++;

			if (restrictedRequestIds.contains(item.getKey())) {
				restrictedRequests++;

				if (item.getValue().toString().endsWith("_1")) {
					validMatchings++;
				} else {
					invalidMatchings++;
				}
			}
		}

		assertEquals(343, totalRequests);
		assertEquals(67, restrictedRequests);
		assertEquals(67, validMatchings);
		assertEquals(0, invalidMatchings);
	}

	static public class SkillsHandler implements PassengerPickedUpEventHandler {
		private final IdMap<Request, Id<DvrpVehicle>> vehicles = new IdMap<>(Request.class);

		@Override
		public void handleEvent(PassengerPickedUpEvent event) {
			vehicles.put(event.getRequestId(), event.getVehicleId());
		}

		public void install(Controler controller) {
			SkillsHandler self = this;

			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(self);
				}
			});
		}
	}

	@Test
	void testRangeConstraint() {
		Controler controller = createController();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());

		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig) //
				.withVehicleRange(100.0 * 1e3);

		controller.addOverridingQSimModule(insertionModule);

		DistanceHandler handler = new DistanceHandler(controller.getScenario().getNetwork());
		handler.install(controller);

		controller.run();

		for (var item : handler.distances.entrySet()) {
			assertTrue(item.getValue() < 100.0 * 1e3);
		}
	}

	@Test
	void testRangeConstraintWithCustomInstances() {
		Controler controller = createController();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());

		CustomCalculator distanceCalculator = new CustomCalculator();
		CustomCalculator distanceApproximator = new CustomCalculator();

		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig) //
				.withVehicleRange(100.0 * 1e3) //
				.withDistanceCalculator(distanceCalculator) //
				.withDistanceApproximator(distanceApproximator);

		controller.addOverridingQSimModule(insertionModule);

		DistanceHandler handler = new DistanceHandler(controller.getScenario().getNetwork());
		handler.install(controller);

		controller.run();

		for (var item : handler.distances.entrySet()) {
			assertTrue(item.getValue() < 100.0 * 1e3);
		}

		assertEquals(1470, distanceCalculator.calculatedDistances);
		assertEquals(5288, distanceApproximator.calculatedDistances);
	}

	@Test
	void testRangeConstraintWithCustomInjection() {
		Controler controller = createController();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());

		CustomDistanceCalculator distanceCalculator = new CustomDistanceCalculator();
		CustomDistanceApproximator distanceApproximator = new CustomDistanceApproximator();

		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig) //
				.withVehicleRange(100.0 * 1e3) //
				.withDistanceCalculator(CustomDistanceCalculator.class) //
				.withDistanceApproximator(CustomDistanceApproximator.class);

		controller.addOverridingQSimModule(insertionModule);

		controller.addOverridingQSimModule(new AbstractDvrpModeQSimModule("drt") {
			@Override
			protected void configureQSim() {
				bindModal(CustomDistanceCalculator.class).toInstance(distanceCalculator);
				bindModal(CustomDistanceApproximator.class).toInstance(distanceApproximator);
			}
		});

		DistanceHandler handler = new DistanceHandler(controller.getScenario().getNetwork());
		handler.install(controller);

		controller.run();

		for (var item : handler.distances.entrySet()) {
			assertTrue(item.getValue() < 100.0 * 1e3);
		}

		assertEquals(1470, distanceCalculator.calculatedDistances);
		assertEquals(5288, distanceApproximator.calculatedDistances);
	}

	static class CustomDistanceCalculator extends CustomCalculator {
	}

	static class CustomDistanceApproximator extends CustomCalculator {
	}

	static class CustomCalculator implements DistanceApproximator {
		int calculatedDistances = 0;

		@Override
		public synchronized double calculateDistance(double departureTime, Link fromLink, Link toLink) {
			calculatedDistances++;
			return CoordUtils.calcEuclideanDistance(fromLink.getCoord(), toLink.getCoord());
		}
	}

	static public class DistanceHandler implements LinkEnterEventHandler {
		private final Network network;
		private final IdMap<Vehicle, Double> distances = new IdMap<>(Vehicle.class);

		DistanceHandler(Network network) {
			this.network = network;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			distances.compute(event.getVehicleId(),
					(id, val) -> val == null ? 0.0 : val + network.getLinks().get(event.getLinkId()).getLength());
		}

		public void install(Controler controller) {
			DistanceHandler self = this;

			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(self);
				}
			});
		}
	}

	@Test
	void testCustomConstraint() {
		Controler controller = createController();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());

		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig) //
				.withConstraint((request, insertion, detourInfo) -> {
					return Integer.parseInt(request.getId().toString().split("_")[1]) > 100;
				});

		controller.addOverridingQSimModule(insertionModule);

		RejectedRequestsHandler handler = new RejectedRequestsHandler();
		handler.install(controller);

		controller.run();

		for (int i = 0; i <= 100; i++) {
			assertTrue(handler.rejectedRequests.contains(Id.create("drt_" + i, Request.class)));
		}

		assertEquals(112, handler.rejectedRequests.size());
	}

	static public class RejectedRequestsHandler implements PassengerRequestRejectedEventHandler {
		private final IdSet<Request> rejectedRequests = new IdSet<>(Request.class);

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			rejectedRequests.add(event.getRequestId());
		}

		public void install(Controler controller) {
			RejectedRequestsHandler self = this;

			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(self);
				}
			});
		}
	}

	@Test
	void testDefaults() {
		Controler controller = createController();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());

		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig);
		controller.addOverridingQSimModule(insertionModule);

		ObjectiveHandler handler = new ObjectiveHandler(controller.getScenario().getNetwork());
		handler.install(controller);

		controller.run();

		assertEquals(16, handler.rejectedRequests);
		assertEquals(2112862.0, handler.fleetDistance, 1e-3);
		assertEquals(698710.0, handler.activeTime(), 1e-3);
		assertEquals(280.19623, handler.meanWaitTime(), 1e-3);
	}

	@Test
	void testVehicleActiveTimeObjective() {
		Controler controller = createController();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());

		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig) //
				.minimizeVehicleActiveTime();

		controller.addOverridingQSimModule(insertionModule);

		ObjectiveHandler handler = new ObjectiveHandler(controller.getScenario().getNetwork());
		handler.install(controller);

		controller.run();

		assertEquals(16, handler.rejectedRequests);
		assertEquals(2112862.0, handler.fleetDistance, 1e-3);
		assertEquals(698710.0, handler.activeTime(), 1e-3);
		assertEquals(280.19623, handler.meanWaitTime(), 1e-3);
	}

	@Test
	void testVehicleDistanceObjective() {
		Controler controller = createController();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());

		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig) //
				.minimizeVehicleDistance();

		controller.addOverridingQSimModule(insertionModule);

		ObjectiveHandler handler = new ObjectiveHandler(controller.getScenario().getNetwork());
		handler.install(controller);

		controller.run();

		assertEquals(22, handler.rejectedRequests);
		assertEquals(2066658.0, handler.fleetDistance, 1e-3);
		assertEquals(694149.0, handler.activeTime(), 1e-3);
		assertEquals(280.61475, handler.meanWaitTime(), 1e-3);
	}

	@Test
	void testPassengerPassengerDelayObjective() {
		Controler controller = createController();
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());

		DrtInsertionModule insertionModule = new DrtInsertionModule(drtConfig) //
				.minimizePassengerDelay(1.0, 0.0);

		controller.addOverridingQSimModule(insertionModule);

		ObjectiveHandler handler = new ObjectiveHandler(controller.getScenario().getNetwork());
		handler.install(controller);

		controller.run();

		assertEquals(23, handler.rejectedRequests);
		assertEquals(2141019.0, handler.fleetDistance, 1e-3);
		assertEquals(704897.0, handler.activeTime(), 1e-3);
		assertEquals(241.17808, handler.meanWaitTime(), 1e-3);
	}

	static public class ObjectiveHandler
			implements PassengerWaitingEventHandler, PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler,
			PassengerRequestRejectedEventHandler, LinkEnterEventHandler, TaskEndedEventHandler {
		private final IdMap<Request, Double> departureTimes = new IdMap<>(Request.class);
		private final IdMap<Request, Double> pickupTimes = new IdMap<>(Request.class);
		private final IdMap<Request, Double> dropoffTimes = new IdMap<>(Request.class);
		private final IdMap<DvrpVehicle, Double> lastNonStayTime = new IdMap<>(DvrpVehicle.class);

		private int rejectedRequests = 0;
		private double fleetDistance = 0.0;

		private final Network network;

		ObjectiveHandler(Network network) {
			this.network = network;
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			rejectedRequests++;
		}

		@Override
		public void handleEvent(PassengerWaitingEvent event) {
			departureTimes.put(event.getRequestId(), event.getTime());
		}

		@Override
		public void handleEvent(PassengerPickedUpEvent event) {
			pickupTimes.put(event.getRequestId(), event.getTime());

		}

		@Override
		public void handleEvent(PassengerDroppedOffEvent event) {
			dropoffTimes.put(event.getRequestId(), event.getTime());
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			fleetDistance += network.getLinks().get(event.getLinkId()).getLength();
		}

		@Override
		public void handleEvent(TaskEndedEvent event) {
			if (DrtTaskBaseType.DRIVE.isBaseTypeOf(event.getTaskType())) {
				lastNonStayTime.put(event.getDvrpVehicleId(), event.getTime());
			}
		}

		public double meanWaitTime() {
			double totalWaitTime = 0.0;
			double totalRequests = 0.0;

			List<Double> waitTimes = new LinkedList<>();

			for (Id<Request> requestId : departureTimes.keySet()) {
				if (pickupTimes.containsKey(requestId) && dropoffTimes.containsKey(requestId)) {
					totalWaitTime += pickupTimes.get(requestId) - departureTimes.get(requestId);
					waitTimes.add(pickupTimes.get(requestId) - departureTimes.get(requestId));
					totalRequests += 1;
				}
			}

			return totalWaitTime / totalRequests;
		}

		public double activeTime() {
			return lastNonStayTime.values().stream().mapToDouble(d -> d).sum();
		}

		public void install(Controler controller) {
			ObjectiveHandler self = this;

			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(self);
				}
			});
		}
	}
}
