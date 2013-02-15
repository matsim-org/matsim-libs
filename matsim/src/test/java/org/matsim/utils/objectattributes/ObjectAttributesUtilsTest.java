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

package org.matsim.utils.objectattributes;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author mrieser / Senozon AG
 */
public class ObjectAttributesUtilsTest {

	@Test
	public void testGetAllAttributes() {
		ObjectAttributes oa = new ObjectAttributes();
		oa.putAttribute("1", "a", "A");
		oa.putAttribute("1", "b", "B");
		oa.putAttribute("1", "c", "C");
		
		Collection<String> names = ObjectAttributesUtils.getAllAttributeNames(oa, "1");
		Assert.assertEquals(3, names.size());
		Assert.assertTrue(names.contains("a"));
		Assert.assertTrue(names.contains("b"));
		Assert.assertTrue(names.contains("c"));
		Assert.assertFalse(names.contains("d"));
	}
	
	@Test
	public void testGetAllAttributes_isImmutable() {
		ObjectAttributes oa = new ObjectAttributes();
		oa.putAttribute("1", "a", "A");
		oa.putAttribute("1", "b", "B");
		oa.putAttribute("1", "c", "C");
		
		Collection<String> names = ObjectAttributesUtils.getAllAttributeNames(oa, "1");
		try {
			names.add("d");
			Assert.fail("Expected immutability-exception");
		} catch (Exception everythingOkay) {
		}

		try {
			names.remove("b");
			Assert.fail("Expected immutability-exception");
		} catch (Exception everythingOkay) {
		}
		
		try {
			Iterator<String> iter = names.iterator();
			iter.next();
			iter.remove();
			Assert.fail("Expected immutability-exception");
		} catch (Exception everythingOkay) {
		}
		
	}
}
