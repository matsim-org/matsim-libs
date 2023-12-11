/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */

package org.matsim.contrib.drt.extension.operations.shifts.analysis.efficiency;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftEndedEvent;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftStartedEvent;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.fare.DrtFareHandler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftEfficiencyTest {

	public static final double FARE = 10.;

	/**
	 * Test method for {@link ShiftEfficiencyTracker}.
	 */
	@Test
	void testDrtShiftEfficiency() {

		EventsManager events = new EventsManagerImpl();
		ShiftEfficiencyTracker shiftEfficiencyTracker = new ShiftEfficiencyTracker();
		events.addHandler(shiftEfficiencyTracker);
		events.initProcessing();

		Id<DrtShift> shift1 = Id.create("shift1", DrtShift.class);
		Id<Link> link1 = Id.createLinkId("link1");
		Id<DvrpVehicle> vehicle1 = Id.create("vehicle1", DvrpVehicle.class);
		Id<OperationFacility> operationFacility1 = Id.create("operationFacility1", OperationFacility.class);

		events.processEvent(new DrtShiftStartedEvent(10 * 3600, shift1, vehicle1, link1)
		);
		// should throw because vehicle is already registered with another shift
		Assertions.assertThrows(RuntimeException.class, () -> {
			events.processEvent(new DrtShiftStartedEvent(10 * 3600, shift1, vehicle1, link1));
		});

		Id<Request> request1 = Id.create("request1", Request.class);
		Id<Person> person1 = Id.createPersonId("person1");

		events.processEvent(new PassengerDroppedOffEvent(11 * 3600, "drt",
				request1, person1, vehicle1));
		Assertions.assertTrue(shiftEfficiencyTracker.getCurrentRecord().getRequestsByShift().get(shift1).contains(request1));

		events.processEvent(new PersonMoneyEvent(11 * 3600, person1, -FARE,
				DrtFareHandler.PERSON_MONEY_EVENT_PURPOSE_DRT_FARE, "drt", request1.toString()));
		Assertions.assertEquals(FARE, shiftEfficiencyTracker.getCurrentRecord().getRevenueByShift().get(shift1), MatsimTestUtils.EPSILON);

		Assertions.assertFalse(shiftEfficiencyTracker.getCurrentRecord().getFinishedShifts().containsKey(shift1));
		events.processEvent(new DrtShiftEndedEvent(20 * 3600, shift1, vehicle1, link1, operationFacility1));
		Assertions.assertTrue(shiftEfficiencyTracker.getCurrentRecord().getFinishedShifts().containsKey(shift1));
	}
}
