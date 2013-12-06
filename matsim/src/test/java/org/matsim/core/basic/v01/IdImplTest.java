/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package org.matsim.core.basic.v01;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author jwjoubert
 */
public class IdImplTest {

	@Test
	public void testConstructor(){
		try{
			@SuppressWarnings("unused")
			IdImpl id = new IdImpl(null); 
			Assert.fail("Should have thrown a NullPointerException. Nt allowedto pass null.");
		} catch(NullPointerException e) {
			/* Should catch the NullPointerException. */
		}
		
		String s = "1234";
		IdImpl id = new IdImpl(s);
		Assert.assertTrue("Wrong Id.", id.toString().equalsIgnoreCase(s) );
	}
	
	@Test
	public void testEquals() {
		IdImpl id1 = new IdImpl("1234");
		IdImpl id2 = new IdImpl("1235");
		IdImpl id3 = new IdImpl("1234");
		Assert.assertFalse("Two Ids should not be equal.", id1.equals(id2));
		Assert.assertTrue("Two Ids should be equal.", id1.equals(id3));
		
		String s = "1234";
		Assert.assertFalse("Two dissimilar objects should not be equal.", id1.equals(s));
	}
	
	@Test
	public void testCompareTo(){
		IdImpl id1 = new IdImpl("1234");
		IdImpl id2 = new IdImpl("1235");
		Assert.assertEquals("Id1 is smaller than Id2", -1, id1.compareTo(id2));
		Assert.assertEquals("Id1 is smaller than Id2", 1, id2.compareTo(id1));
		
		IdImpl id3 = new IdImpl("1234");
		Assert.assertEquals("Id1 is the same as Id3", 0, id1.compareTo(id3));
	}

}
