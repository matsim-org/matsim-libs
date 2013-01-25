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
package playground.thibautd.utils;

import static org.junit.Assert.*;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author thibautd
 */
public class ObjectPoolTest {
	@Test
	public void testInstanceIsPooled() throws Exception {
		final ObjectPool<Id> pool = new ObjectPool<Id>();

		final Id instance1 = new IdImpl( "jojo" );
		final Id instance2 = new IdImpl( "jojo" );
		final Id instance3 = new IdImpl( "jojo" );

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

