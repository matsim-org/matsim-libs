
/* *********************************************************************** *
 * project: org.matsim.*
 * AttributesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.utils.objectattributes.attributable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

	/**
 * @author thibautd
 */
public class AttributesTest {
	 @Test
	 void testInsertion() {
		final Attributes attributes = new AttributesImpl();

		attributes.putAttribute( "sun" , "nice" );
		attributes.putAttribute( "rain is nice" , false );
		attributes.putAttribute( "the answer" , 42 );
		attributes.putAttribute( "1 the begin" , 1L );

		Assertions.assertEquals( 4 , attributes.size(), "unexpected number of elements in "+attributes );

		Assertions.assertEquals( "nice" ,
				attributes.getAttribute( "sun" ),
				"unexpected value " );

		Assertions.assertEquals( false ,
				attributes.getAttribute( "rain is nice" ),
				"unexpected value " );

		Assertions.assertEquals( 42 ,
				attributes.getAttribute( "the answer" ),
				"unexpected value " );

		Assertions.assertEquals( 1L ,
				attributes.getAttribute( "1 the begin" ),
				"unexpected value " );
	}

	 @Test
	 void testReplacement() {
		final Attributes attributes = new AttributesImpl();

		attributes.putAttribute( "sun" , "nice" );
		attributes.putAttribute( "rain is nice" , false );
		attributes.putAttribute( "the answer" , 7 );
		attributes.putAttribute( "1 the begin" , 1L );

		// that was wrong!
		final Object wrong = attributes.putAttribute( "the answer" , 42 );

		Assertions.assertEquals( 4 , attributes.size(), "unexpected number of elements in "+attributes );

		Assertions.assertEquals( 7 , wrong, "unexpected replaced value " );

		Assertions.assertEquals( 42 ,
				attributes.getAttribute( "the answer" ),
				"unexpected value " );
	}

	 @Test
	 void testRemoval() {
		final Attributes attributes = new AttributesImpl();

		attributes.putAttribute( "sun" , "nice" );
		attributes.putAttribute( "rain is nice" , false );
		attributes.putAttribute( "the answer" , 7 );
		attributes.putAttribute( "1 the begin" , 1L );

		// no need to store such a stupid statement
		final Object wrong = attributes.removeAttribute( "rain is nice" );

		Assertions.assertEquals( 3 , attributes.size(), "unexpected number of elements in "+attributes );

		Assertions.assertEquals( false , wrong, "unexpected removed value " );

		Assertions.assertNull( attributes.getAttribute( "rain is nice" ),
				"unexpected mapping " );
	}

	 @Test
	 void testGetAsMap() {
		final Attributes attributes = new AttributesImpl();

		attributes.putAttribute( "sun" , "nice" );
		attributes.putAttribute( "rain is nice" , false );

		Map<String, Object> map = attributes.getAsMap();
		Assertions.assertEquals(2, map.size());

		Assertions.assertEquals("nice", map.get("sun"));
		Assertions.assertEquals(false, map.get("rain is nice"));

		Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
		boolean foundSun = false;
		boolean foundRain = false;
		Assertions.assertTrue(iter.hasNext());
		Map.Entry<String, Object> e = iter.next();
		if (e.getKey().equals("sun") && e.getValue().equals("nice")) {
			foundSun = true;
		}
		if (e.getKey().equals("rain is nice") && e.getValue().equals(false)) {
			foundRain = true;
		}
		Assertions.assertTrue(iter.hasNext());
		e = iter.next();
		if (e.getKey().equals("sun") && e.getValue().equals("nice")) {
			foundSun = true;
		}
		if (e.getKey().equals("rain is nice") && e.getValue().equals(false)) {
			foundRain = true;
		}
		Assertions.assertFalse(iter.hasNext());

		Assertions.assertTrue(foundSun);
		Assertions.assertTrue(foundRain);

		try {
			iter.next();
			Assertions.fail("Expected NoSuchElementException, but got none.");
		} catch (NoSuchElementException ignore) {
			// expected
		} catch (Exception ex) {
			ex.printStackTrace();
			Assertions.fail("Expected NoSuchElementException, but caught a different one.");
		}
	}
}
