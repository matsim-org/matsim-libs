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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mrieser / Senozon AG
 */
public class ObjectAttributesUtilsTest {

	@Test
	void testGetAllAttributes() {
		ObjectAttributes oa = new ObjectAttributes();
		oa.putAttribute("1", "a", "A");
		oa.putAttribute("1", "b", "B");
		oa.putAttribute("1", "c", "C");
		
		Collection<String> names = ObjectAttributesUtils.getAllAttributeNames(oa, "1");
		Assertions.assertEquals(3, names.size());
		Assertions.assertTrue(names.contains("a"));
		Assertions.assertTrue(names.contains("b"));
		Assertions.assertTrue(names.contains("c"));
		Assertions.assertFalse(names.contains("d"));
	}

	@Test
	void testGetAllAttributes_isImmutable() {
		ObjectAttributes oa = new ObjectAttributes();
		oa.putAttribute("1", "a", "A");
		oa.putAttribute("1", "b", "B");
		oa.putAttribute("1", "c", "C");
		
		Collection<String> names = ObjectAttributesUtils.getAllAttributeNames(oa, "1");
		try {
			names.add("d");
			Assertions.fail("Expected immutability-exception");
		} catch (Exception everythingOkay) {
		}

		try {
			names.remove("b");
			Assertions.fail("Expected immutability-exception");
		} catch (Exception everythingOkay) {
		}
		
		try {
			Iterator<String> iter = names.iterator();
			iter.next();
			iter.remove();
			Assertions.fail("Expected immutability-exception");
		} catch (Exception everythingOkay) {
		}
		
	}
}
