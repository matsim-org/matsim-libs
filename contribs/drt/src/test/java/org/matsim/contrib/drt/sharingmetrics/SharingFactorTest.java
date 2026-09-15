package org.matsim.contrib.drt.sharingmetrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;

/**
 * @author nkuehnel / MOIA
 */
public class SharingFactorTest {

	/**
	 * Test method for {@link SharingMetricsTracker}.
	 */
	@Test
	public void testDrtSharingFactorHandler() {
		String mode = "mode_0";

		var vehicleId = Id.create("v1", DvrpVehicle.class);
		var personId1 = Id.createPersonId("p1");
		var personId2 = Id.createPersonId("p2");


		ParallelEventsManager events = new ParallelEventsManager(false);
		SharingMetricsTracker sharingFactorTracker = new SharingMetricsTracker(mode);
		events.addHandler(sharingFactorTracker);

		events.initProcessing();


		{
			//single trip, no pooling
			var requestId = Id.create(0, Request.class);
			Assertions.assertNull(sharingFactorTracker.getPoolingRates().get(requestId));

			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId, List.of(personId1), null, null, 0, 0, 0, 0, 0, 0, null, null));
			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId, personId1, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(300.0, mode, requestId, personId1, vehicleId));
			events.flush();

			Assertions.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId));
			Assertions.assertFalse(sharingFactorTracker.getPoolingRates().get(requestId));
			Assertions.assertEquals(1., sharingFactorTracker.getSharingFactors().get(requestId), MatsimTestUtils.EPSILON);
		}

		//clean up
		sharingFactorTracker.reset(0);

		{
			//two trips exactly after each other, no pooling
			var requestId1 = Id.create(0, Request.class);
			var requestId2 = Id.create(1, Request.class);
			Assertions.assertNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertNull(sharingFactorTracker.getPoolingRates().get(requestId2));

			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId1, List.of(personId1), null, null, 0, 0, 0, 0, 0, 0, null, null));
			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId2, List.of(personId2), null, null, 0, 0, 0, 0, 0, 0, null, null));
			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(300.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerPickedUpEvent(300.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(500.0, mode, requestId2, personId2, vehicleId));
			events.flush();

			Assertions.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId1));
			Assertions.assertFalse(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertEquals(1., sharingFactorTracker.getSharingFactors().get(requestId1), MatsimTestUtils.EPSILON);

			Assertions.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assertions.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId2));
			Assertions.assertFalse(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assertions.assertEquals(1., sharingFactorTracker.getSharingFactors().get(requestId2), MatsimTestUtils.EPSILON);
		}

		//clean up
		sharingFactorTracker.reset(0);

		{
			//two trips overlap half of the time
			var requestId1 = Id.create(0, Request.class);
			var requestId2 = Id.create(1, Request.class);
			Assertions.assertNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertNull(sharingFactorTracker.getPoolingRates().get(requestId2));

			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId1, List.of(personId1), null, null, 0, 0, 0, 0, 0, 0, null, null));
			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId2, List.of(personId2), null, null, 0, 0, 0, 0, 0, 0, null, null));
			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerPickedUpEvent(200.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(300.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(400.0, mode, requestId2, personId2, vehicleId));
			events.flush();

			Assertions.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId1));
			Assertions.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertEquals((100. + 100.) / (100 + 50), sharingFactorTracker.getSharingFactors().get(requestId1), MatsimTestUtils.EPSILON);

			Assertions.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assertions.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId2));
			Assertions.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assertions.assertEquals((100. + 100.) / (50 + 100), sharingFactorTracker.getSharingFactors().get(requestId2), MatsimTestUtils.EPSILON);
		}


		//clean up
		sharingFactorTracker.reset(0);

		{
			// second trip (sharing factor = 2) happens completely within first trip (sharing factor = 1.2)
			var requestId1 = Id.create(0, Request.class);
			var requestId2 = Id.create(1, Request.class);
			Assertions.assertNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertNull(sharingFactorTracker.getPoolingRates().get(requestId2));

			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId1, List.of(personId1), null, null, 0, 0, 0, 0, 0, 0, null, null));
			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId2, List.of(personId2), null, null, 0, 0, 0, 0, 0, 0, null, null));

			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerPickedUpEvent(200.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(300.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(400.0, mode, requestId1, personId1, vehicleId));
			events.flush();

			Assertions.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId1));
			Assertions.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertEquals((100. + 100. + 100.) / (100 + 50 + 100), sharingFactorTracker.getSharingFactors().get(requestId1), MatsimTestUtils.EPSILON);

			Assertions.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assertions.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId2));
			Assertions.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assertions.assertEquals((100.) / (50.), sharingFactorTracker.getSharingFactors().get(requestId2), MatsimTestUtils.EPSILON);
		}

		//clean up
		sharingFactorTracker.reset(0);

		{
			// two persons share exact same trip but not part of a group

			var requestId1 = Id.create(0, Request.class);
			var requestId2 = Id.create(1, Request.class);
			Assertions.assertNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertNull(sharingFactorTracker.getPoolingRates().get(requestId2));

			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId1, List.of(personId1), null, null, 0, 0, 0, 0, 0, 0, null, null));
			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId2, List.of(personId2), null, null, 0, 0, 0, 0, 0, 0, null, null));

			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(200.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(200.0, mode, requestId2, personId2, vehicleId));
			events.flush();

			Assertions.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId1));
			Assertions.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertEquals(2., sharingFactorTracker.getSharingFactors().get(requestId1), MatsimTestUtils.EPSILON);

			Assertions.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assertions.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId2));
			Assertions.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assertions.assertEquals(2., sharingFactorTracker.getSharingFactors().get(requestId2), MatsimTestUtils.EPSILON);
		}


		//clean up
		sharingFactorTracker.reset(0);

		{
			// two persons part of a group -> not pooled

			var requestId1 = Id.create(0, Request.class);
			Assertions.assertNull(sharingFactorTracker.getPoolingRates().get(requestId1));

			events.processEvent(new DrtRequestSubmittedEvent(0.0, mode, requestId1, List.of(personId1, personId2), null, null, 0, 0, 0, 0, 0, 0, null, null));

			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(200.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(200.0, mode, requestId1, personId2, vehicleId));
			events.flush();

			Assertions.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId1));
			Assertions.assertFalse(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assertions.assertEquals(1., sharingFactorTracker.getSharingFactors().get(requestId1), MatsimTestUtils.EPSILON);
		}
	}

	/**
	 * Verifies that two trackers bound to different modes each process only their own
	 * mode's events and produce independent, correct results.
	 * <p>
	 * mode_A has two pooled trips (sharing factor > 1); mode_B has a single solo trip
	 * (sharing factor = 1). Before the fix both trackers would have seen all events and
	 * produced identical, aggregated output.
	 */
	@Test
	public void testSharingMetricsAreComputedPerMode() {
		String modeA = "drt_A";
		String modeB = "drt_B";

		var vehicleA = Id.create("vA", DvrpVehicle.class);
		var vehicleB = Id.create("vB", DvrpVehicle.class);

		var person1 = Id.createPersonId("p1");
		var person2 = Id.createPersonId("p2");
		var person3 = Id.createPersonId("p3");

		// Two requests in mode_A overlap completely -> both are pooled, sharing factor = 2
		var requestA1 = Id.create("rA1", Request.class);
		var requestA2 = Id.create("rA2", Request.class);

		// One solo request in mode_B -> not pooled, sharing factor = 1
		var requestB1 = Id.create("rB1", Request.class);

		ParallelEventsManager events = new ParallelEventsManager(false);

		SharingMetricsTracker trackerA = new SharingMetricsTracker(modeA);
		SharingMetricsTracker trackerB = new SharingMetricsTracker(modeB);

		events.addHandler(trackerA);
		events.addHandler(trackerB);
		events.initProcessing();

		// mode_A: two requests picked up at the same time and dropped off at the same time
		events.processEvent(new DrtRequestSubmittedEvent(0.0, modeA, requestA1, List.of(person1), null, null, 0, 0, 0, 0, 0, 0, null, null));
		events.processEvent(new DrtRequestSubmittedEvent(0.0, modeA, requestA2, List.of(person2), null, null, 0, 0, 0, 0, 0, 0, null, null));
		events.processEvent(new PassengerPickedUpEvent(100.0, modeA, requestA1, person1, vehicleA));
		events.processEvent(new PassengerPickedUpEvent(100.0, modeA, requestA2, person2, vehicleA));
		events.processEvent(new PassengerDroppedOffEvent(200.0, modeA, requestA1, person1, vehicleA));
		events.processEvent(new PassengerDroppedOffEvent(200.0, modeA, requestA2, person2, vehicleA));

		// mode_B: single solo trip
		events.processEvent(new DrtRequestSubmittedEvent(0.0, modeB, requestB1, List.of(person3), null, null, 0, 0, 0, 0, 0, 0, null, null));
		events.processEvent(new PassengerPickedUpEvent(100.0, modeB, requestB1, person3, vehicleB));
		events.processEvent(new PassengerDroppedOffEvent(300.0, modeB, requestB1, person3, vehicleB));

		events.flush();

		// --- trackerA: must contain exactly the two mode_A requests ---
		Assertions.assertEquals(2, trackerA.getSharingFactors().size(), "trackerA must track only mode_A requests");
		Assertions.assertNull(trackerA.getSharingFactors().get(requestB1), "trackerA must not contain mode_B request");
		Assertions.assertTrue(trackerA.getPoolingRates().get(requestA1), "requestA1 must be pooled");
		Assertions.assertTrue(trackerA.getPoolingRates().get(requestA2), "requestA2 must be pooled");
		Assertions.assertEquals(2., trackerA.getSharingFactors().get(requestA1), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(2., trackerA.getSharingFactors().get(requestA2), MatsimTestUtils.EPSILON);

		// --- trackerB: must contain exactly the one mode_B request ---
		Assertions.assertEquals(1, trackerB.getSharingFactors().size(), "trackerB must track only mode_B requests");
		Assertions.assertNull(trackerB.getSharingFactors().get(requestA1), "trackerB must not contain mode_A request rA1");
		Assertions.assertNull(trackerB.getSharingFactors().get(requestA2), "trackerB must not contain mode_A request rA2");
		Assertions.assertFalse(trackerB.getPoolingRates().get(requestB1), "requestB1 must not be pooled");
		Assertions.assertEquals(1., trackerB.getSharingFactors().get(requestB1), MatsimTestUtils.EPSILON);
	}
}
