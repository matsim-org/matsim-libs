/* *********************************************************************** *
 * project: org.matsim.*
 * ObjectPoolTest.java
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
package org.matsim.contrib.socnetsim.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author thibautd
 */
public class ObjectPoolTest {
	@Test
	public void testInstanceIsPooled() throws Exception {
		final ObjectPool<String> pool = new ObjectPool<String>();

		final String instance1 = new String( "jojo" );
		final String instance2 = new String( "jojo" );
		final String instance3 = new String( "jojo" );

		assertTrue("the two variables should be different objects", instance1 != instance2);
		
		assertSame(
				"first instance not returned when pooled",
				instance1,
				pool.getPooledInstance( instance1 ));

		assertNotSame(
				"second instance returned instead of first",
				instance2,
				pool.getPooledInstance( instance2 ));

		assertSame(
				"first instance not returned while pooled",
				instance1,
				pool.getPooledInstance( instance2 ));

		assertSame(
				"first instance not returned while pooled",
				instance1,
				pool.getPooledInstance( instance3 ));
	}

	// TODO test forgetting
}

