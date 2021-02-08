/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.schedule.DrtStopTask;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class KNearestInsertionsAtEndFilterTest {

	@Test
	public void noInsertions_emptyList() {
		var filteredInsertions = filterInsertionsAtEnd();
		assertThat(filteredInsertions).isEmpty();
	}

	@Test
	public void noAtEndInsertions_allReturned() {
		var vehicleEntry = vehicleEntry(start(0), stop(100));
		var insertion1 = insertion(vehicleEntry, 0, 11);
		var insertion2 = insertion(vehicleEntry, 0, 22);

		assertThat(filterInsertionsAtEnd(insertion1, insertion2)).containsExactlyInAnyOrder(insertion1.getInsertion(),
				insertion2.getInsertion());
	}

	@Test
	public void onlyAtEndInsertions_theEarliestReturned() {
		// estimated arrival time: 100 + 10 = 110
		var vehicleEntry1 = vehicleEntry(start(0), stop(100));
		var insertion1 = insertion(vehicleEntry1, 1, 10);

		// estimated arrival time: 50 + 61 = 111
		var vehicleEntry2 = vehicleEntry(start(50));
		var insertion2 = insertion(vehicleEntry2, 0, 61);

		//insertion1 is better
		assertThat(filterInsertionsAtEnd(insertion1, insertion2)).containsExactly(insertion1.getInsertion());
		assertThat(filterInsertionsAtEnd(insertion2, insertion1)).containsExactly(insertion1.getInsertion());
	}

	@Test
	public void onlyAtEndInsertions_equalArrivalTime_theFirstReturned() {
		// estimated arrival time: 100 + 10 = 110
		var vehicleEntry1 = vehicleEntry(start(0), stop(100));
		var insertion1 = insertion(vehicleEntry1, 1, 10);

		// estimated arrival time: 50 + 60 = 110
		var vehicleEntry2 = vehicleEntry(start(50));
		var insertion2 = insertion(vehicleEntry2, 0, 60);

		//take the first
		assertThat(filterInsertionsAtEnd(insertion1, insertion2)).containsExactly(insertion1.getInsertion());
		assertThat(filterInsertionsAtEnd(insertion2, insertion1)).containsExactly(insertion2.getInsertion());
	}

	@Test
	public void mixedTypeInsertions_bothReturned() {
		var vehicleEntry = vehicleEntry(start(0), stop(100));
		var insertionAfterStart = insertion(vehicleEntry, 0, 11);
		var insertionAtEnd = insertion(vehicleEntry, 1, 10);

		//insertionAtEnd always returned last
		assertThat(filterInsertionsAtEnd(insertionAfterStart, insertionAtEnd)).containsExactlyInAnyOrder(
				insertionAfterStart.getInsertion(), insertionAtEnd.getInsertion());
	}

	private InsertionWithDetourData<Double> insertion(VehicleEntry vehicleEntry, int pickupIdx,
			double toPickupDetourTime) {
		return new InsertionWithDetourData<>(new InsertionGenerator.Insertion(vehicleEntry,
				new InsertionGenerator.InsertionPoint(pickupIdx, vehicleEntry.getWaypoint(pickupIdx), null,
						vehicleEntry.getWaypoint(pickupIdx + 1)), null), toPickupDetourTime, null, null, null);
	}

	private Waypoint.Start start(double endTime) {
		return new Waypoint.Start(null, null, endTime, 0);
	}

	private Waypoint.Stop stop(double endTime) {
		return new Waypoint.Stop(new DrtStopTask(endTime - 10, endTime, null), 0);
	}

	private VehicleEntry vehicleEntry(Waypoint.Start start, Waypoint.Stop... stops) {
		return new VehicleEntry(null, start, ImmutableList.copyOf(stops));
	}

	@SafeVarargs
	private List<InsertionGenerator.Insertion> filterInsertionsAtEnd(InsertionWithDetourData<Double>... insertions) {
		return KNearestInsertionsAtEndFilter.filterInsertionsAtEnd(1, 1, List.of(insertions));
	}
}
