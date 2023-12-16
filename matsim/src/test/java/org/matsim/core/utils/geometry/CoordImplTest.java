/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.core.utils.geometry;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;

/**
 * @author mrieser / Senozon AG
 */
public class CoordImplTest {

	/**
	 * The old hashCode implementation of CoordImpl has problems that it generated the same
	 * hash values for coordinates that were located in a grid-like structure. Make sure
	 * the new hashCode implementation does not have this problem.
	 */
	@Test
	void testHashCode() {
		int[] hashCodes = new int[] {
				new Coord(1.0, 1.0).hashCode(),
				new Coord(2.0, 2.0).hashCode(),
				new Coord(3.0, 3.0).hashCode(),
				new Coord(2.0, 1.0).hashCode(),
				new Coord(3.0, 2.0).hashCode(),
				new Coord(1.0, 2.0).hashCode(),
				new Coord(2.0, 3.0).hashCode(),
				new Coord(1.0, 3.0).hashCode(),
				new Coord(3.0, 1.0).hashCode(),
				new Coord(3.0, 1.5).hashCode(),
				new Coord(3.0, 1.1).hashCode()
		};
		
		// sort array and count how many unique values we have:
		Arrays.sort(hashCodes);
		int lastValue = -1;
		int cnt = 0;
		for (int hc : hashCodes) {
			if (lastValue != hc) {
				cnt++;
			}
			lastValue = hc;
		}

		Assertions.assertEquals(hashCodes.length, cnt, "There has been a hashCode collision, which is undesired!");
	}
}
