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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;

/**
 * Based on FloatMatrix from sbb-matsim-extensions
 *
 * @author Michal Maciejewski (michalm)
 */
public final class Matrix {
	//Range of unsigned short: 0-65535 (18:12:15)
	//In case 18 hours is not enough, we can reduce the resolution from seconds to tens of seconds
	private static final int MAX_UNSIGNED_SHORT = Short.MAX_VALUE - Short.MIN_VALUE;

	//up to around 46k+ is possible (limited by the max data array size)
	private static final int MAX_ZONE_COUNT = 45_000;

	//there are usually not so many Zone objects, so not a problem if zoneIndex2localIndex is sparse
	private final int[] zoneIndex2localIndex = new int[Id.getNumberOfIds(Zone.class)];
	private final int size;
	private final short[] data;

	Matrix(Set<Zone> zones) {
		size = zones.size();
		checkArgument(size <= MAX_ZONE_COUNT, "Too many zones. Try using bigger cell sizes (when using grid zones)");

		//to make sure we do not refer to zones added later
		Arrays.fill(zoneIndex2localIndex, -1);

		int nextIndex = 0;
		for (Zone zone : zones) {
			zoneIndex2localIndex[zone.getId().index()] = nextIndex;
			nextIndex++;
		}

		data = new short[size * size];
		Arrays.fill(data, (short)MAX_UNSIGNED_SHORT);//-1
	}

	public int get(Zone fromZone, Zone toZone) {
		short shortValue = data[dataIndex(fromZone, toZone)];
		if (shortValue == -1) {
			throw new NoSuchElementException("No value set for zones: " + fromZone.getId() + " -> " + toZone.getId());
		}
		return Short.toUnsignedInt(shortValue);
	}

	public void set(Zone fromZone, Zone toZone, double value) {
		checkArgument(Double.isFinite(value) && value >= 0 && value < MAX_UNSIGNED_SHORT);
		data[dataIndex(fromZone, toZone)] = (short)value;
	}

	private int dataIndex(Zone fromZone, Zone toZone) {
		int fromIndex = zoneIndex2localIndex[fromZone.getId().index()];
		checkArgument(fromIndex >= 0, "Matrix was not created for zone: (%s)", fromZone);
		int toIndex = zoneIndex2localIndex[toZone.getId().index()];
		checkArgument(toIndex >= 0, "Matrix was not created for zone: (%s)", toZone);
		return fromIndex * size + toIndex;
	}
}
