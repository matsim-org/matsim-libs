/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.insertion.extensive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.PickupDetourInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.InsertionPoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class KNearestInsertionsAtEndFilterTest {

	@Test
	void k0_atEndInsertionsNotReturned() {
		var vehicleEntry = vehicleEntry("v1", start(0), stop(100));
		var insertion = insertion(vehicleEntry, 1, 10);
		var filteredInsertions = KNearestInsertionsAtEndFilter.filterInsertionsAtEnd(0, List.of(insertion));
		assertThat(filteredInsertions).isEmpty();
	}

	@Test
	void noInsertions_emptyList() {
		var filteredInsertions = filterOneInsertionAtEnd();
		assertThat(filteredInsertions).isEmpty();
	}

	@Test
	void noAtEndInsertions_allReturned() {
		var vehicleEntry = vehicleEntry("v1", start(0), stop(100));
		var insertion1 = insertion(vehicleEntry, 0, 11);
		var insertion2 = insertion(vehicleEntry, 0, 22);

		assertThat(filterOneInsertionAtEnd(insertion1, insertion2)).containsExactlyInAnyOrder(insertion1.insertion,
				insertion2.insertion);
	}

	@Test
	void onlyAtEndInsertions_theEarliestReturned() {
		var vehicleEntry1 = vehicleEntry("v1", start(0), stop(100));
		var insertion1 = insertion(vehicleEntry1, 1, 110);

		var vehicleEntry2 = vehicleEntry("v2", start(50));
		var insertion2 = insertion(vehicleEntry2, 0, 111);

		//insertion1 is better
		assertThat(filterOneInsertionAtEnd(insertion1, insertion2)).containsExactly(insertion1.insertion);
		assertThat(filterOneInsertionAtEnd(insertion2, insertion1)).containsExactly(insertion1.insertion);
	}

	@Test
	void onlyAtEndInsertions_equalArrivalTime_useVehicleIdAsTieBreaker() {
		var vehicleEntry1 = vehicleEntry("v1", start(0), stop(100));
		var insertion1 = insertion(vehicleEntry1, 1, 110);

		var vehicleEntry2 = vehicleEntry("v2", start(50));
		var insertion2 = insertion(vehicleEntry2, 0, 110);

		//take the first
		assertThat(filterOneInsertionAtEnd(insertion1, insertion2)).containsExactly(insertion1.insertion);
		assertThat(filterOneInsertionAtEnd(insertion2, insertion1)).containsExactly(insertion1.insertion);
	}

	@Test
	void mixedTypeInsertions_bothReturned() {
		var vehicleEntry = vehicleEntry("v1", start(0), stop(100));
		var insertionAfterStart = insertion(vehicleEntry, 0, 11);
		var insertionAtEnd = insertion(vehicleEntry, 1, 10);

		//insertionAtEnd always returned last
		assertThat(filterOneInsertionAtEnd(insertionAfterStart, insertionAtEnd)).containsExactlyInAnyOrder(
				insertionAfterStart.insertion, insertionAtEnd.insertion);
	}

	private InsertionWithDetourData insertion(VehicleEntry vehicleEntry, int pickupIdx, double pickupDepartureTime) {
		return new InsertionWithDetourData(new Insertion(vehicleEntry,
				new InsertionPoint(pickupIdx, vehicleEntry.getWaypoint(pickupIdx), null,
						vehicleEntry.getWaypoint(pickupIdx + 1)), null), null,
				new DetourTimeInfo(new PickupDetourInfo(pickupDepartureTime, Double.NaN), null));
	}

	private Waypoint.Start start(double endTime) {
		return new Waypoint.Start(null, null, endTime, 0);
	}

	private Waypoint.Stop stop(double endTime) {
		return new Waypoint.Stop(new DefaultDrtStopTask(endTime - 10, endTime, null), 0);
	}

	private VehicleEntry vehicleEntry(String id, Waypoint.Start start, Waypoint.Stop... stops) {
		var vehicle = mock(DvrpVehicle.class);
		when(vehicle.getId()).thenReturn(Id.create(id, DvrpVehicle.class));
		return new VehicleEntry(vehicle, start, ImmutableList.copyOf(stops), null, null, 0);
	}

	private List<Insertion> filterOneInsertionAtEnd(InsertionWithDetourData... insertions) {
		return KNearestInsertionsAtEndFilter.filterInsertionsAtEnd(1, List.of(insertions));
	}
}
