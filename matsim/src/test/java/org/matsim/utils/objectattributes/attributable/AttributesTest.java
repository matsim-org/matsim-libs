
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

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author thibautd
 */
public class AttributesTest {
	@Test
	public void testInsertion() {
		final Attributes attributes = new Attributes();

		attributes.putAttribute( "sun" , "nice" );
		attributes.putAttribute( "rain is nice" , false );
		attributes.putAttribute( "the answer" , 42 );
		attributes.putAttribute( "1 the begin" , 1L );

		Assert.assertEquals( "unexpected number of elements in "+attributes ,
				4 , attributes.size() );

		Assert.assertEquals( "unexpected value " ,
				"nice" ,
				attributes.getAttribute( "sun" ) );

		Assert.assertEquals( "unexpected value " ,
				false ,
				attributes.getAttribute( "rain is nice" ) );

		Assert.assertEquals( "unexpected value " ,
				42 ,
				attributes.getAttribute( "the answer" ) );

		Assert.assertEquals( "unexpected value " ,
				1L ,
				attributes.getAttribute( "1 the begin" ) );
	}

	@Test
	public void testReplacement() {
		final Attributes attributes = new Attributes();

		attributes.putAttribute( "sun" , "nice" );
		attributes.putAttribute( "rain is nice" , false );
		attributes.putAttribute( "the answer" , 7 );
		attributes.putAttribute( "1 the begin" , 1L );

		// that was wrong!
		final Object wrong = attributes.putAttribute( "the answer" , 42 );

		Assert.assertEquals( "unexpected number of elements in "+attributes ,
				4 , attributes.size() );

		Assert.assertEquals( "unexpected replaced value " ,
				7 , wrong );

		Assert.assertEquals( "unexpected value " ,
				42 ,
				attributes.getAttribute( "the answer" ) );
	}

	@Test
	public void testRemoval() {
		final Attributes attributes = new Attributes();

		attributes.putAttribute( "sun" , "nice" );
		attributes.putAttribute( "rain is nice" , false );
		attributes.putAttribute( "the answer" , 7 );
		attributes.putAttribute( "1 the begin" , 1L );

		// no need to store such a stupid statement
		final Object wrong = attributes.removeAttribute( "rain is nice" );

		Assert.assertEquals( "unexpected number of elements in "+attributes ,
				3 , attributes.size() );

		Assert.assertEquals( "unexpected removed value " ,
				false , wrong );

		Assert.assertNull( "unexpected mapping " ,
				attributes.getAttribute( "rain is nice" ) );
	}

	@Test
	public void testGetAsMap() {
		final Attributes attributes = new Attributes();

		attributes.putAttribute( "sun" , "nice" );
		attributes.putAttribute( "rain is nice" , false );

		Map<String, Object> map = attributes.getAsMap();
		Assert.assertEquals(2, map.size());

		Assert.assertEquals("nice", map.get("sun"));
		Assert.assertEquals(false, map.get("rain is nice"));

		Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
		boolean foundSun = false;
		boolean foundRain = false;
		Assert.assertTrue(iter.hasNext());
		Map.Entry<String, Object> e = iter.next();
		if (e.getKey().equals("sun") && e.getValue().equals("nice")) {
			foundSun = true;
		}
		if (e.getKey().equals("rain is nice") && e.getValue().equals(false)) {
			foundRain = true;
		}
		Assert.assertTrue(iter.hasNext());
		e = iter.next();
		if (e.getKey().equals("sun") && e.getValue().equals("nice")) {
			foundSun = true;
		}
		if (e.getKey().equals("rain is nice") && e.getValue().equals(false)) {
			foundRain = true;
		}
		Assert.assertFalse(iter.hasNext());

		Assert.assertTrue(foundSun);
		Assert.assertTrue(foundRain);

		try {
			iter.next();
			Assert.fail("Expected NoSuchElementException, but got none.");
		} catch (NoSuchElementException ignore) {
			// expected
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail("Expected NoSuchElementException, but caught a different one.");
		}
	}
}
