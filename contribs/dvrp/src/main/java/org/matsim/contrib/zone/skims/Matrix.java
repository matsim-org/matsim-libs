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
import org.matsim.contrib.common.zones.Zone;

/**
 * Based on FloatMatrix from sbb-matsim-extensions
 *
 * @author Michal Maciejewski (michalm)
 */
public final class Matrix {
	//Range of unsigned short: 0-65535 (18:12:15)
	//In case 18 hours is not enough, we can reduce the resolution from seconds to tens of seconds
	private static final int MAX_UNSIGNED_SHORT = Short.MAX_VALUE - Short.MIN_VALUE;

	//there are usually not so many Zone objects, so not a problem if zoneIndex2localIndex is sparse
	private final int[] zoneIndex2matrixIndex = new int[Id.getNumberOfIds(Zone.class)];
	private final short[][] matrix;

	public Matrix(Set<Zone> zones) {
		//to make sure we do not refer to zones added later
		Arrays.fill(zoneIndex2matrixIndex, -1);

		int nextIndex = 0;
		for (Zone zone : zones) {
			zoneIndex2matrixIndex[zone.getId().index()] = nextIndex;
			nextIndex++;
		}

		matrix = new short[zones.size()][zones.size()];
		for (var row : matrix) {
			Arrays.fill(row, (short)MAX_UNSIGNED_SHORT);//-1
		}
	}

	public int get(Zone fromZone, Zone toZone) {
		short shortValue = matrix[matrixIndex(fromZone)][matrixIndex(toZone)];
		if (shortValue == -1) {
			throw new NoSuchElementException("No value set for zones: " + fromZone.getId() + " -> " + toZone.getId());
		}
		return Short.toUnsignedInt(shortValue);
	}

	void set(Zone fromZone, Zone toZone, double value) {
		checkArgument(Double.isFinite(value) && value >= 0 && value < MAX_UNSIGNED_SHORT);
		matrix[matrixIndex(fromZone)][matrixIndex(toZone)] = (short)value;
	}

	private int matrixIndex(Zone zone) {
		int index = zoneIndex2matrixIndex[zone.getId().index()];
		checkArgument(index >= 0, "Matrix was not created for zone: (%s)", zone);
		return index;
	}
}
