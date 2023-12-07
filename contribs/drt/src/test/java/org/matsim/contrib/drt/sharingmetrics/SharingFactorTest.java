package org.matsim.contrib.drt.sharingmetrics;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.Set;

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


		Set<Id<Person>> groupRepresentatives = new HashSet<>();

		// two separate bookings
		groupRepresentatives.add(personId1);
		groupRepresentatives.add(personId2);


		ParallelEventsManager events = new ParallelEventsManager(false);
		SharingMetricsTracker sharingFactorTracker = new SharingMetricsTracker(new SharingMetricsTracker.GroupPredicate() {
			@Override
			public boolean isGroupRepresentative(Id<Person> personId) {
				return groupRepresentatives.contains(personId);
			}
		});
		events.addHandler(sharingFactorTracker);

		events.initProcessing();


		{
			//single trip, no pooling
			var requestId = Id.create(0, Request.class);
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId));

			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId, personId1, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(300.0, mode, requestId, personId1, vehicleId));
			events.flush();

			Assert.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId));
			Assert.assertFalse(sharingFactorTracker.getPoolingRates().get(requestId));
			Assert.assertEquals(1., sharingFactorTracker.getSharingFactors().get(requestId), MatsimTestUtils.EPSILON);
		}

		//clean up
		sharingFactorTracker.notifyMobsimBeforeCleanup(null);

		{
			//two trips exactly after each other, no pooling
			var requestId1 = Id.create(0, Request.class);
			var requestId2 = Id.create(1, Request.class);
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId2));

			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(300.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerPickedUpEvent(300.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(500.0, mode, requestId2, personId2, vehicleId));
			events.flush();

			Assert.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId1));
			Assert.assertFalse(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertEquals(1., sharingFactorTracker.getSharingFactors().get(requestId1), MatsimTestUtils.EPSILON);

			Assert.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assert.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId2));
			Assert.assertFalse(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assert.assertEquals(1., sharingFactorTracker.getSharingFactors().get(requestId2), MatsimTestUtils.EPSILON);
		}

		//clean up
		sharingFactorTracker.notifyMobsimBeforeCleanup(null);

		{
			//two trips overlap half of the time
			var requestId1 = Id.create(0, Request.class);
			var requestId2 = Id.create(1, Request.class);
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId2));

			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerPickedUpEvent(200.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(300.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(400.0, mode, requestId2, personId2, vehicleId));
			events.flush();

			Assert.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId1));
			Assert.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertEquals((100. + 100.) / (100 + 50), sharingFactorTracker.getSharingFactors().get(requestId1), MatsimTestUtils.EPSILON);

			Assert.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assert.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId2));
			Assert.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assert.assertEquals((100. + 100.) / (50 + 100), sharingFactorTracker.getSharingFactors().get(requestId2), MatsimTestUtils.EPSILON);
		}


		//clean up
		sharingFactorTracker.notifyMobsimBeforeCleanup(null);

		{
			// second trip (sharing factor = 2) happens completely within first trip (sharing factor = 1.2)
			var requestId1 = Id.create(0, Request.class);
			var requestId2 = Id.create(1, Request.class);
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId2));

			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerPickedUpEvent(200.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(300.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(400.0, mode, requestId1, personId1, vehicleId));
			events.flush();

			Assert.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId1));
			Assert.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertEquals((100. + 100. + 100.) / (100 + 50 + 100), sharingFactorTracker.getSharingFactors().get(requestId1), MatsimTestUtils.EPSILON);

			Assert.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assert.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId2));
			Assert.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assert.assertEquals((100. ) / (50.), sharingFactorTracker.getSharingFactors().get(requestId2), MatsimTestUtils.EPSILON);
		}

		//clean up
		sharingFactorTracker.notifyMobsimBeforeCleanup(null);

		{
			// two persons share exact same trip but not part of a group

			var requestId1 = Id.create(0, Request.class);
			var requestId2 = Id.create(1, Request.class);
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId2));

			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(200.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(200.0, mode, requestId2, personId2, vehicleId));
			events.flush();

			Assert.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId1));
			Assert.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertEquals(2., sharingFactorTracker.getSharingFactors().get(requestId1), MatsimTestUtils.EPSILON);

			Assert.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assert.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId2));
			Assert.assertTrue(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assert.assertEquals(2., sharingFactorTracker.getSharingFactors().get(requestId2), MatsimTestUtils.EPSILON);
		}


		//clean up
		sharingFactorTracker.notifyMobsimBeforeCleanup(null);

		{
			// two persons part of a group, only person 1 is representative -> not pooled
			groupRepresentatives.remove(personId2);

			var requestId1 = Id.create(0, Request.class);
			var requestId2 = Id.create(1, Request.class);
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId2));

			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerPickedUpEvent(100.0, mode, requestId2, personId2, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(200.0, mode, requestId1, personId1, vehicleId));
			events.processEvent(new PassengerDroppedOffEvent(200.0, mode, requestId2, personId2, vehicleId));
			events.flush();

			Assert.assertNotNull(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertNotNull(sharingFactorTracker.getSharingFactors().get(requestId1));
			Assert.assertFalse(sharingFactorTracker.getPoolingRates().get(requestId1));
			Assert.assertEquals(1., sharingFactorTracker.getSharingFactors().get(requestId1), MatsimTestUtils.EPSILON);

			Assert.assertNull(sharingFactorTracker.getPoolingRates().get(requestId2));
			Assert.assertNull(sharingFactorTracker.getSharingFactors().get(requestId2));
		}
	}
}
