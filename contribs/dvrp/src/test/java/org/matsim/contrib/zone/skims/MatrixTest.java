/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.zone.skims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MatrixTest {
	private final Zone unknownZone = new ZoneImpl(Id.create("?", Zone.class), null, null, null);

	private final Zone zoneA = new ZoneImpl(Id.create("A", Zone.class), null, null, null);
	private final Zone zoneB = new ZoneImpl(Id.create("B", Zone.class), null, null, null);
	private final Zone zoneC = new ZoneImpl(Id.create("C", Zone.class), null, null, null);

	private final Set<Zone> zones = Set.of(zoneA, zoneB, zoneC);
	private final Matrix matrix = new Matrix(zones);

	private final static int MAX_ALLOWED_VALUE = (1 << 16) - 2;

	@Test
	void get_unsetValue() {
		for (Zone from : zones) {
			for (Zone to : zones) {
				assertThatThrownBy(() -> matrix.get(from, to)).isExactlyInstanceOf(NoSuchElementException.class)
						.hasMessage("No value set for zones: %s -> %s", from.getId(), to.getId());
			}
		}
	}

	@Test
	void get_validValue() {
		matrix.set(zoneA, zoneB, 0);
		assertThat(matrix.get(zoneA, zoneB)).isEqualTo(0);

		matrix.set(zoneA, zoneC, MAX_ALLOWED_VALUE);
		assertThat(matrix.get(zoneA, zoneC)).isEqualTo(MAX_ALLOWED_VALUE);
	}

	@Test
	void set_invalidValue() {
		assertThatThrownBy(() -> matrix.set(zoneA, zoneB, Double.POSITIVE_INFINITY)).isExactlyInstanceOf(
				IllegalArgumentException.class);
		assertThatThrownBy(() -> matrix.set(zoneA, zoneC, -1)).isExactlyInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> matrix.set(zoneB, zoneC, MAX_ALLOWED_VALUE + 1)).isExactlyInstanceOf(
				IllegalArgumentException.class);
	}

	@Test
	void get_unknownZone() {
		assertThatThrownBy(() -> matrix.get(zoneA, unknownZone)).isExactlyInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> matrix.get(unknownZone, zoneC)).isExactlyInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void set_unknownZone() {
		assertThatThrownBy(() -> matrix.set(zoneA, unknownZone, 1)).isExactlyInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> matrix.set(unknownZone, zoneC, 1)).isExactlyInstanceOf(IllegalArgumentException.class);
	}
}
