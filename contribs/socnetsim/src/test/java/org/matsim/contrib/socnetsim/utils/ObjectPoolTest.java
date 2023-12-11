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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author thibautd
 */
public class ObjectPoolTest {
	@Test
	void testInstanceIsPooled() throws Exception {
		final ObjectPool<String> pool = new ObjectPool<String>();

		final String instance1 = new String( "jojo" );
		final String instance2 = new String( "jojo" );
		final String instance3 = new String( "jojo" );

		assertTrue(instance1 != instance2, "the two variables should be different objects");
		
		assertSame(
				instance1,
				pool.getPooledInstance( instance1 ),
				"first instance not returned when pooled");

		assertNotSame(
				instance2,
				pool.getPooledInstance( instance2 ),
				"second instance returned instead of first");

		assertSame(
				instance1,
				pool.getPooledInstance( instance2 ),
				"first instance not returned while pooled");

		assertSame(
				instance1,
				pool.getPooledInstance( instance3 ),
				"first instance not returned while pooled");
	}

	// TODO test forgetting
}

