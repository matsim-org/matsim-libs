package org.matsim.contrib.drt.prebooking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.prebooking.unscheduler.ComplexRequestUnscheduler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.path.DivertedVrpPath;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.mockito.Mockito;

/**
 * @author Sebastian Hörl (sebhoerl) / IRT SystemX
 */
public class ComplexUnschedulerTest {
	@Test
	public void testDirectDropoffAfterPickup() {
		Fixture fixture = new Fixture();
		Schedule schedule = fixture.schedule;

		AcceptedDrtRequest otherRequest1 = fixture.createRequest();
		AcceptedDrtRequest otherRequest2 = fixture.createRequest();
		AcceptedDrtRequest unscheduleRequest = fixture.createRequest();

		fixture.addDrive("f10");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(otherRequest1);
		fixture.addDrive("f20");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(otherRequest2);
		fixture.addDrive("f30"); // replace start
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(unscheduleRequest);
		fixture.addDrive("f40");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(unscheduleRequest);
		fixture.addDrive("f50");
		fixture.addWait(300.0); // replace end
		fixture.addStop(60.0).addDropoffRequest(otherRequest2);
		fixture.addDrive("f60");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(otherRequest1);
		fixture.addStay(1000.0);

		schedule.nextTask();
		schedule.nextTask();

		ComplexRequestUnscheduler unscheduler = new ComplexRequestUnscheduler(fixture.lookup, fixture.entryFactory,
				fixture.taskFactory, fixture.router, fixture.travelTime, fixture.timingUpdater, false);

		unscheduler.unscheduleRequest(100.0, fixture.vehicle.getId(), unscheduleRequest.getId());

		assertEquals(13, schedule.getTaskCount());
		assertTrue(schedule.getTasks().get(0) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(1) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(2) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(3) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(4) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(5) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(6) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(7) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(8) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(9) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(10) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(11) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(12) instanceof DrtStayTask);

		DrtDriveTask insertedDriveTask = (DrtDriveTask) schedule.getTasks().get(6);
		assertEquals("f20", insertedDriveTask.getPath().getFromLink().getId().toString());
		assertEquals("f50", insertedDriveTask.getPath().getToLink().getId().toString());
	}

	@Test
	public void testStandardSituation() {
		Fixture fixture = new Fixture();
		Schedule schedule = fixture.schedule;

		AcceptedDrtRequest otherRequest1 = fixture.createRequest();
		AcceptedDrtRequest otherRequest2 = fixture.createRequest();
		AcceptedDrtRequest unscheduleRequest = fixture.createRequest();

		fixture.addDrive("f10");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(otherRequest1); // f10
		fixture.addDrive("f20"); // replace start
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(unscheduleRequest); // f20
		fixture.addDrive("f30");
		fixture.addWait(300.0); // replace end
		fixture.addStop(60.0).addPickupRequest(otherRequest2); // f30
		fixture.addDrive("f40");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(otherRequest2); // f40
		fixture.addDrive("f50"); // replace start
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(unscheduleRequest); // f50
		fixture.addDrive("f60");
		fixture.addWait(300.0); // replace end
		fixture.addStop(60.0).addDropoffRequest(otherRequest1); // f60
		fixture.addStay(1000.0);

		schedule.nextTask();
		schedule.nextTask();

		ComplexRequestUnscheduler unscheduler = new ComplexRequestUnscheduler(fixture.lookup, fixture.entryFactory,
				fixture.taskFactory, fixture.router, fixture.travelTime, fixture.timingUpdater, false);

		unscheduler.unscheduleRequest(100.0, fixture.vehicle.getId(), unscheduleRequest.getId());

		assertEquals(13, schedule.getTaskCount());
		assertTrue(schedule.getTasks().get(0) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(1) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(2) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(3) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(4) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(5) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(6) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(7) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(8) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(9) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(10) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(11) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(12) instanceof DrtStayTask);

		DrtDriveTask insertedDriveTask = (DrtDriveTask) schedule.getTasks().get(3);
		assertEquals("f10", insertedDriveTask.getPath().getFromLink().getId().toString());
		assertEquals("f30", insertedDriveTask.getPath().getToLink().getId().toString());

		DrtDriveTask insertedDriveTask2 = (DrtDriveTask) schedule.getTasks().get(9);
		assertEquals("f40", insertedDriveTask2.getPath().getFromLink().getId().toString());
		assertEquals("f60", insertedDriveTask2.getPath().getToLink().getId().toString());
	}

	@Test
	public void testRemoveAtEnd() {
		Fixture fixture = new Fixture();
		Schedule schedule = fixture.schedule;

		AcceptedDrtRequest otherRequest1 = fixture.createRequest();
		AcceptedDrtRequest otherRequest2 = fixture.createRequest();
		AcceptedDrtRequest unscheduleRequest = fixture.createRequest();

		fixture.addDrive("f10");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(otherRequest1); // f10
		fixture.addDrive("f20"); // replace start
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(unscheduleRequest); // f20
		fixture.addDrive("f30");
		fixture.addWait(300.0); // replace end
		fixture.addStop(60.0).addPickupRequest(otherRequest2); // f30
		fixture.addDrive("f40");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(otherRequest2); // f40
		fixture.addDrive("f50");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(otherRequest1); // f50
		fixture.addDrive("f60"); // replace start
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(unscheduleRequest); // f60 // replace end
		fixture.addStay(1000.0);

		schedule.nextTask();
		schedule.nextTask();

		ComplexRequestUnscheduler unscheduler = new ComplexRequestUnscheduler(fixture.lookup, fixture.entryFactory,
				fixture.taskFactory, fixture.router, fixture.travelTime, fixture.timingUpdater, false);

		unscheduler.unscheduleRequest(100.0, fixture.vehicle.getId(), unscheduleRequest.getId());

		assertEquals(13, schedule.getTaskCount());
		assertTrue(schedule.getTasks().get(0) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(1) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(2) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(3) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(4) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(5) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(6) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(7) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(8) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(9) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(10) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(11) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(12) instanceof DrtStayTask);

		DrtDriveTask insertedDriveTask = (DrtDriveTask) schedule.getTasks().get(3);
		assertEquals("f10", insertedDriveTask.getPath().getFromLink().getId().toString());
		assertEquals("f30", insertedDriveTask.getPath().getToLink().getId().toString());

		DrtDriveTask insertedDriveTask2 = (DrtDriveTask) schedule.getTasks().get(9);
		assertEquals("f40", insertedDriveTask2.getPath().getFromLink().getId().toString());
		assertEquals("f50", insertedDriveTask2.getPath().getToLink().getId().toString());

		DrtStayTask stayTask = (DrtStayTask) schedule.getTasks().get(12);
		assertEquals("f50", stayTask.getLink().getId().toString());
	}
	
	@Test
	public void testRemoveAtBeginningWithWaitSecond() {
		Fixture fixture = new Fixture();
		Schedule schedule = fixture.schedule;

		AcceptedDrtRequest otherRequest1 = fixture.createRequest();
		AcceptedDrtRequest otherRequest2 = fixture.createRequest();
		AcceptedDrtRequest unscheduleRequest = fixture.createRequest();

		fixture.addDrive("f10");
		fixture.addWait(300.0); // replace start
		fixture.addStop(60.0).addPickupRequest(unscheduleRequest); // f10
		fixture.addDrive("f20");
		fixture.addWait(300.0); // replace end
		fixture.addStop(60.0).addPickupRequest(otherRequest1); // f20
		fixture.addDrive("f30");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(otherRequest2); // f30
		fixture.addDrive("f40");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(otherRequest2); // f40
		fixture.addDrive("f50"); // replace start
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(unscheduleRequest); // f50
		fixture.addDrive("f60");
		fixture.addWait(300.0); // replace end
		fixture.addStop(60.0).addDropoffRequest(otherRequest1); // f60
		fixture.addStay(1000.0);

		schedule.nextTask();
		schedule.nextTask();

		ComplexRequestUnscheduler unscheduler = new ComplexRequestUnscheduler(fixture.lookup, fixture.entryFactory,
				fixture.taskFactory, fixture.router, fixture.travelTime, fixture.timingUpdater, false);

		unscheduler.unscheduleRequest(500.0, fixture.vehicle.getId(), unscheduleRequest.getId());

		assertEquals(15, schedule.getTaskCount());
		assertTrue(schedule.getTasks().get(0) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(1) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(2) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(3) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(4) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(5) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(6) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(7) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(8) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(9) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(10) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(11) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(12) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(13) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(14) instanceof DrtStayTask);

		DrtDriveTask insertedDriveTask = (DrtDriveTask) schedule.getTasks().get(2);
		assertEquals("f10", insertedDriveTask.getPath().getFromLink().getId().toString());
		assertEquals("f20", insertedDriveTask.getPath().getToLink().getId().toString());

		DrtDriveTask insertedDriveTask2 = (DrtDriveTask) schedule.getTasks().get(11);
		assertEquals("f40", insertedDriveTask2.getPath().getFromLink().getId().toString());
		assertEquals("f60", insertedDriveTask2.getPath().getToLink().getId().toString());
	}
	
	@Test
	public void testRemoveAtBeginningWithWaitFirst() {
		Fixture fixture = new Fixture();
		Schedule schedule = fixture.schedule;

		AcceptedDrtRequest otherRequest1 = fixture.createRequest();
		AcceptedDrtRequest otherRequest2 = fixture.createRequest();
		AcceptedDrtRequest unscheduleRequest = fixture.createRequest();

		fixture.addWait(300.0); // replace start
		fixture.addDrive("f10");
		fixture.addStop(60.0).addPickupRequest(unscheduleRequest); // f10
		fixture.addDrive("f20");
		fixture.addWait(300.0); // replace end
		fixture.addStop(60.0).addPickupRequest(otherRequest1); // f20
		fixture.addDrive("f30");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(otherRequest2); // f30
		fixture.addDrive("f40");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(otherRequest2); // f40
		fixture.addDrive("f50"); // replace start
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(unscheduleRequest); // f50
		fixture.addDrive("f60");
		fixture.addWait(300.0); // replace end
		fixture.addStop(60.0).addDropoffRequest(otherRequest1); // f60
		fixture.addStay(1000.0);

		schedule.nextTask();

		ComplexRequestUnscheduler unscheduler = new ComplexRequestUnscheduler(fixture.lookup, fixture.entryFactory,
				fixture.taskFactory, fixture.router, fixture.travelTime, fixture.timingUpdater, false);

		unscheduler.unscheduleRequest(500.0, fixture.vehicle.getId(), unscheduleRequest.getId());

		assertEquals(14, schedule.getTaskCount());
		assertTrue(schedule.getTasks().get(0) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(1) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(2) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(3) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(4) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(5) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(6) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(7) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(8) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(9) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(10) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(11) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(12) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(13) instanceof DrtStayTask);

		DrtDriveTask insertedDriveTask = (DrtDriveTask) schedule.getTasks().get(1);
		assertEquals("f0", insertedDriveTask.getPath().getFromLink().getId().toString());
		assertEquals("f20", insertedDriveTask.getPath().getToLink().getId().toString());

		DrtDriveTask insertedDriveTask2 = (DrtDriveTask) schedule.getTasks().get(10);
		assertEquals("f40", insertedDriveTask2.getPath().getFromLink().getId().toString());
		assertEquals("f60", insertedDriveTask2.getPath().getToLink().getId().toString());
	}
	
	@Test
	public void testRemoveAtBeginningWithDriveDiversion() {
		Fixture fixture = new Fixture();
		Schedule schedule = fixture.schedule;

		AcceptedDrtRequest otherRequest1 = fixture.createRequest();
		AcceptedDrtRequest otherRequest2 = fixture.createRequest();
		AcceptedDrtRequest unscheduleRequest = fixture.createRequest();

		fixture.addDrive("f10");
		fixture.addWait(300.0); // replace start
		fixture.addStop(60.0).addPickupRequest(unscheduleRequest); // f10
		fixture.addDrive("f20");
		fixture.addWait(300.0); // replace end
		fixture.addStop(60.0).addPickupRequest(otherRequest1); // f20
		fixture.addDrive("f30");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(otherRequest2); // f30
		fixture.addDrive("f40");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(otherRequest2); // f40
		fixture.addDrive("f50"); // replace start
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(unscheduleRequest); // f50
		fixture.addDrive("f60");
		fixture.addWait(300.0); // replace end
		fixture.addStop(60.0).addDropoffRequest(otherRequest1); // f60
		fixture.addStay(1000.0);

		schedule.nextTask();
		
		OnlineDriveTaskTracker tracker = Mockito.mock(OnlineDriveTaskTracker.class);
		schedule.getTasks().get(0).initTaskTracker(tracker);
		
		LinkTimePair diversionPoint = new LinkTimePair(fixture.network.getLinks().get(Id.createLinkId("f5")), 20.0);
		Mockito.when(tracker.getDiversionPoint()).thenReturn(diversionPoint);
		
		Mockito.doAnswer(invocation -> {
			VrpPathWithTravelData path = invocation.getArgument(0);
			DriveTask task = (DriveTask) schedule.getTasks().get(0);
			DivertedVrpPath divertedPath = new DivertedVrpPath(task.getPath(), path, 5);
			task.pathDiverted(divertedPath, path.getArrivalTime());
			return null;
		}).when(tracker).divertPath(Mockito.any());

		ComplexRequestUnscheduler unscheduler = new ComplexRequestUnscheduler(fixture.lookup, fixture.entryFactory,
				fixture.taskFactory, fixture.router, fixture.travelTime, fixture.timingUpdater, false);

		unscheduler.unscheduleRequest(500.0, fixture.vehicle.getId(), unscheduleRequest.getId());

		assertEquals(13, schedule.getTaskCount());
		assertTrue(schedule.getTasks().get(0) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(1) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(2) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(3) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(4) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(5) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(6) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(7) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(8) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(9) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(10) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(11) instanceof DrtStopTask);
		assertTrue(schedule.getTasks().get(12) instanceof DrtStayTask);

		DrtDriveTask insertedDriveTask = (DrtDriveTask) schedule.getTasks().get(0);
		assertEquals("f0", insertedDriveTask.getPath().getFromLink().getId().toString());
		assertEquals("f20", insertedDriveTask.getPath().getToLink().getId().toString());

		DrtDriveTask insertedDriveTask2 = (DrtDriveTask) schedule.getTasks().get(9);
		assertEquals("f40", insertedDriveTask2.getPath().getFromLink().getId().toString());
		assertEquals("f60", insertedDriveTask2.getPath().getToLink().getId().toString());
	}
	
	@Test
	public void testRemoveAllStartWithWait() {
		Fixture fixture = new Fixture();
		Schedule schedule = fixture.schedule;

		AcceptedDrtRequest unscheduleRequest = fixture.createRequest();

		fixture.addWait(300.0);
		fixture.addDrive("f10");
		fixture.addStop(60.0).addPickupRequest(unscheduleRequest); // f10
		fixture.addDrive("f20");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(unscheduleRequest); // f50
		fixture.addStay(1000.0);

		schedule.nextTask();
		
		ComplexRequestUnscheduler unscheduler = new ComplexRequestUnscheduler(fixture.lookup, fixture.entryFactory,
				fixture.taskFactory, fixture.router, fixture.travelTime, fixture.timingUpdater, false);

		unscheduler.unscheduleRequest(0.0, fixture.vehicle.getId(), unscheduleRequest.getId());

		assertEquals(2, schedule.getTaskCount());
		assertTrue(schedule.getTasks().get(0) instanceof DrtStayTask);
		assertTrue(schedule.getTasks().get(1) instanceof DrtStayTask);
		
		assertEquals("f0", ((StayTask) schedule.getTasks().get(0)).getLink().getId().toString());
		assertEquals("f0", ((StayTask) schedule.getTasks().get(1)).getLink().getId().toString());
	}
	
	@Test
	public void testRemoveAllStartWithDrive() {
		Fixture fixture = new Fixture();
		Schedule schedule = fixture.schedule;

		AcceptedDrtRequest unscheduleRequest = fixture.createRequest();

		fixture.addDrive("f10");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addPickupRequest(unscheduleRequest); // f10
		fixture.addDrive("f20");
		fixture.addWait(300.0);
		fixture.addStop(60.0).addDropoffRequest(unscheduleRequest); // f50
		fixture.addStay(1000.0);

		schedule.nextTask();
		
		OnlineDriveTaskTracker tracker = Mockito.mock(OnlineDriveTaskTracker.class);
		schedule.getTasks().get(0).initTaskTracker(tracker);
		
		LinkTimePair diversionPoint = new LinkTimePair(fixture.network.getLinks().get(Id.createLinkId("f5")), 20.0);
		Mockito.when(tracker.getDiversionPoint()).thenReturn(diversionPoint);
		
		Mockito.doAnswer(invocation -> {
			VrpPathWithTravelData path = invocation.getArgument(0);
			DriveTask task = (DriveTask) schedule.getTasks().get(0);
			DivertedVrpPath divertedPath = new DivertedVrpPath(task.getPath(), path, 5);
			task.pathDiverted(divertedPath, path.getArrivalTime());
			return null;
		}).when(tracker).divertPath(Mockito.any());
		
		ComplexRequestUnscheduler unscheduler = new ComplexRequestUnscheduler(fixture.lookup, fixture.entryFactory,
				fixture.taskFactory, fixture.router, fixture.travelTime, fixture.timingUpdater, false);

		unscheduler.unscheduleRequest(0.0, fixture.vehicle.getId(), unscheduleRequest.getId());

		assertEquals(2, schedule.getTaskCount());
		assertTrue(schedule.getTasks().get(0) instanceof DrtDriveTask);
		assertTrue(schedule.getTasks().get(1) instanceof DrtStayTask);
		
		DrtDriveTask driveTask = (DrtDriveTask) schedule.getTasks().get(0);
		assertEquals("f0", driveTask.getPath().getFromLink().getId().toString());
		assertEquals("f5", driveTask.getPath().getToLink().getId().toString());
		
		assertEquals("f5", ((StayTask) schedule.getTasks().get(1)).getLink().getId().toString());
	}

	private Network createNetwork() {
		Network network = NetworkUtils.createNetwork();
		NetworkFactory networkFactory = network.getFactory();

		List<Node> nodes = new LinkedList<>();

		for (int i = 0; i < 100; i++) {
			Node node = networkFactory.createNode(Id.createNodeId("n" + i), new Coord(0.0, i * 1000.0));
			network.addNode(node);
			nodes.add(node);
		}

		for (int i = 0; i < 99; i++) {
			Link forwardLink = networkFactory.createLink(Id.createLinkId("f" + i), nodes.get(i), nodes.get(i + 1));
			network.addLink(forwardLink);

			Link backwardLink = networkFactory.createLink(Id.createLinkId("b" + i), nodes.get(i + 1), nodes.get(i));
			network.addLink(backwardLink);
		}

		for (Link link : network.getLinks().values()) {
			link.setAllowedModes(Collections.singleton("car"));
			link.setLength(1000.0);
			link.setFreespeed(1.0);
		}

		return network;
	}

	private class Fixture {
		private final DvrpVehicle vehicle;
		private final Schedule schedule;
		private final Network network;

		private Link currentLink;
		private double currentTime;

		private final DrtTaskFactory taskFactory = new DrtTaskFactoryImpl();
		private final LeastCostPathCalculator router;
		private final TravelTime travelTime = new FreeSpeedTravelTime();

		private final VehicleEntry.EntryFactory entryFactory;
		private final ScheduleTimingUpdater timingUpdater;
		private final DvrpVehicleLookup lookup;

		private int requestIndex = 0;

		Fixture() {
			this.network = createNetwork();

			Link depotLink = network.getLinks().get(Id.createLinkId("f0"));

			DvrpVehicleSpecification vehicleSpecification = ImmutableDvrpVehicleSpecification.newBuilder() //
					.id(Id.create("vehicle", DvrpVehicle.class)) //
					.capacity(4) //
					.serviceBeginTime(0.0) //
					.serviceEndTime(30.0 * 3600.0) //
					.startLinkId(depotLink.getId()) //
					.build();

			this.vehicle = new DvrpVehicleImpl(vehicleSpecification, depotLink);
			this.schedule = vehicle.getSchedule();
			this.currentLink = vehicle.getStartLink();
			this.currentTime = 0.0;
			this.router = new DijkstraFactory().createPathCalculator(network,
					new OnlyTimeDependentTravelDisutility(travelTime), travelTime);

			this.lookup = Mockito.mock(DvrpVehicleLookup.class);
			Mockito.when(this.lookup.lookupVehicle(Mockito.any())).thenReturn(vehicle);

			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.stopDuration = 30.0;
			drtConfig.maxWaitTime = 600.0;

			this.entryFactory = new VehicleDataEntryFactoryImpl();

			this.timingUpdater = Mockito.mock(ScheduleTimingUpdater.class);
		}

		AcceptedDrtRequest createRequest() {
			AcceptedDrtRequest request = Mockito.mock(AcceptedDrtRequest.class);
			Mockito.when(request.getId()).thenReturn(Id.create("req_" + requestIndex++, Request.class));
			return request;
		}

		DrtStayTask addWait(double duration) {
			DrtStayTask task = taskFactory.createStayTask(vehicle, currentTime, currentTime + duration, currentLink);
			schedule.addTask(task);

			currentTime += duration;
			return task;
		}

		DrtStayTask addStay(double duration) {
			DrtStayTask task = taskFactory.createStayTask(vehicle, currentTime, currentTime + duration, currentLink);
			schedule.addTask(task);

			currentTime += duration;
			return task;
		}

		DrtStopTask addStop(double duration) {
			DrtStopTask task = taskFactory.createStopTask(vehicle, currentTime, currentTime + duration, currentLink);
			schedule.addTask(task);
			currentTime += duration;
			return task;
		}

		DrtDriveTask addDrive(String destinationLinkId) {
			Link destinationLink = network.getLinks().get(Id.createLinkId(destinationLinkId));

			VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(currentLink, destinationLink, currentTime, router,
					travelTime);
			DrtDriveTask driveTask = taskFactory.createDriveTask(vehicle, path, DrtDriveTask.TYPE);
			schedule.addTask(driveTask);

			currentTime = driveTask.getEndTime();
			currentLink = destinationLink;

			return driveTask;
		}
	}
}
