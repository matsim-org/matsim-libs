/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package org.matsim.api.core.v01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.matsim.vehicles.Vehicle;

/**
 * @author Michal Maciejewski (michalm)
 */
public class IdCollectorsTest {

	@Test
	public void testToIdMap() {
		record Entry(Id<Vehicle> id, String value) {
		}

		var e1 = new Entry(Id.createVehicleId("v1"), "1");
		var e2 = new Entry(Id.createVehicleId("v2"), "2");
		var e3 = new Entry(Id.createVehicleId("v3"), "3");

		var entries = List.of(e1, e2, e3);
		Map<Id<Vehicle>, String> map = entries.stream().collect(IdCollectors.toIdMap(Vehicle.class, Entry::id, Entry::value));

		assertThat(map).hasSize(3);
		assertThat(map).containsEntry(e1.id, e1.value);
		assertThat(map).containsEntry(e2.id, e2.value);
		assertThat(map).containsEntry(e3.id, e3.value);
	}

	@Test
	public void testToIdMap_streamWithDuplicateKeys() {
		record Entry(Id<Vehicle> id, String value) {
		}

		var e1 = new Entry(Id.createVehicleId("v"), "1");

		var entries = List.of(e1, e1);
		assertThatThrownBy(() -> entries.stream().collect(IdCollectors.toIdMap(Vehicle.class, Entry::id, Entry::value))).isInstanceOf(
				IllegalStateException.class).hasMessage("Duplicate key %s (attempted merging values %s and %s)", e1.id, e1.value, e1.value);
	}

	@Test
	public void testToIdSet() {
		var id1 = Id.createVehicleId("v1");
		var id2 = Id.createVehicleId("v2");
		var id3 = Id.createVehicleId("v3");

		var ids = List.of(id1, id2, id3, id1); // with duplicates
		var set = ids.stream().collect(IdCollectors.toIdSet(Vehicle.class));

		assertThat(set).containsExactlyInAnyOrder(id1, id2, id3);
	}
}
