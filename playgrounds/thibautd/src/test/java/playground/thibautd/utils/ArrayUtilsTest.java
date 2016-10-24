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
package playground.thibautd.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author thibautd
 */
public class ArrayUtilsTest {
	@Test
	public void testSearchLowest() {
		final Integer[] arr = {1,2,3,3,3,4,5};

		int lowest = ArrayUtils.searchLowest( arr , i -> i, 3 , 0 , 7 );

		Assert.assertEquals(
				"unexpected minimum index",
				5,
				lowest );

		lowest = ArrayUtils.searchLowest( arr , i -> i, 3 , 0 , 3 );

		Assert.assertEquals(
				"unexpected minimum index when restricting range",
				3,
				lowest );
	}
}

