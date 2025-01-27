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
		SharingMetricsTracker sharingFactorTracker = new SharingMetricsTracker();
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
}
