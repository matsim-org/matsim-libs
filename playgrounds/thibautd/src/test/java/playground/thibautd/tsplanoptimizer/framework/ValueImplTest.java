/* *********************************************************************** *
 * project: org.matsim.*
 * ValueImplTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.tsplanoptimizer.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Id;

/**
 * @author thibautd
 */
public class ValueImplTest {
	private static List<ValueImpl> createValues() {
		List<ValueImpl> values = new ArrayList<ValueImpl>();
		values.add( new ValueImpl( Id.create( 1 , Object.class) ) );
		values.add( new ValueImpl( Id.create( 2 , Object.class) ) );
		values.add( new ValueImpl( Id.create( 1 , Object.class) ) );
		values.add( new ValueImpl( "bla" ) );
		values.add( new ValueImpl( "blou" ) );
		values.add( new ValueImpl( "bla" ) );
		values.add( new ValueImpl( 1 ) );
		values.add( new ValueImpl( 1 ) );
		values.add( new ValueImpl( 2 ) );
		return values;
	}

	@Test
	public void testEquals() throws Exception {
		List<ValueImpl> values = createValues();

		for (Value v1 : values) {
			for (Value v2 : values) {
				if (v1.equals( v2 )) {
					assertEquals(
							"values are equal but internal values aren't",
							v1.getValue(),
							v2.getValue());
				}
				else {
					assertFalse(
							"values aren't equal but internal values are",
							v1.getValue().equals( v2.getValue() ));
				}
			}
		}
	}

	@Test
	public void testClone() throws Exception {
		List<ValueImpl> values = createValues();

		for (Value orig : values) {
			Value clone = orig.createClone();
			assertEquals(
					"clones are not equal!",
					orig,
					clone);
			clone.setValue( "this is a new value for the clone" );
			assertFalse(
					"modifying the clone modifies the cloned!",
					orig.equals( clone ));
		}
	}
}

