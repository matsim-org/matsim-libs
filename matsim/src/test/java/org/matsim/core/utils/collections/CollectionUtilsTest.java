/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.collections;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author mrieser
 */
public class CollectionUtilsTest {

	private final static Logger log = LogManager.getLogger(CollectionUtilsTest.class);

	@Test
	void testSetToString() {
		Set<String> set = new LinkedHashSet<String>();
		set.add("Aaa");
		set.add("Bbb");
		set.add("Ddd");
		set.add("Ccc");
		Assertions.assertEquals("Aaa,Bbb,Ddd,Ccc", CollectionUtils.setToString(set));
	}

	@Test
	void testArrayToString() {
		String[] array = new String[] {"Aaa", "Bbb", "Ddd", "Ccc"};
		Assertions.assertEquals("Aaa,Bbb,Ddd,Ccc", CollectionUtils.arrayToString(array));
	}

	@Test
	void testStringToSet() {
		String[] testStrings = new String[] {
				"Aaa,Bbb,Ddd,Ccc",
				",Aaa,Bbb,Ddd,Ccc",
				"Aaa,Bbb,Ddd,Ccc,",
				" ,Aaa,Bbb,Ddd,Ccc, ",
				" , Aaa , Bbb , Ddd , Ccc , ",
				" , Aaa ,	Bbb ,		Ddd , Ccc , ",
				",,, Aaa ,	Bbb ,,		Ddd , Ccc ,,",
				" ,, , Aaa ,	Bbb ,,		Ddd , Ccc ,, ",
			};
		for (String str : testStrings) {
			log.info("testing String: " + str);
			Set<String> set = CollectionUtils.stringToSet(str);
			Assertions.assertEquals(4, set.size());
			Iterator<String> iter = set.iterator();
			Assertions.assertEquals("Aaa", iter.next());
			Assertions.assertEquals("Bbb", iter.next());
			Assertions.assertEquals("Ddd", iter.next());
			Assertions.assertEquals("Ccc", iter.next());
			Assertions.assertFalse(iter.hasNext());
		}
	}

	@Test
	void testNullStringToSet() {
		Set<String> set = CollectionUtils.stringToSet(null);
		Assertions.assertEquals(0, set.size());
	}

	@Test
	void testStringToArray() {
		String[] testStrings = new String[] {
				"Aaa,Bbb,Ddd,Ccc",
				",Aaa,Bbb,Ddd,Ccc",
				"Aaa,Bbb,Ddd,Ccc,",
				" ,Aaa,Bbb,Ddd,Ccc, ",
				" , Aaa , Bbb , Ddd , Ccc , ",
				" , Aaa ,	Bbb ,		Ddd , Ccc , ",
				",,, Aaa ,	Bbb ,,		Ddd , Ccc ,,",
				" ,, , Aaa ,	Bbb ,,		Ddd , Ccc ,, ",
			};
		for (String str : testStrings) {
			log.info("testing String: " + str);
			String[] array = CollectionUtils.stringToArray(str);
			Assertions.assertEquals(4, array.length);
			Assertions.assertEquals("Aaa", array[0]);
			Assertions.assertEquals("Bbb", array[1]);
			Assertions.assertEquals("Ddd", array[2]);
			Assertions.assertEquals("Ccc", array[3]);
		}
	}

	@Test
	void testNullStringToArray() {
		String[] array = CollectionUtils.stringToArray(null);
		Assertions.assertEquals(0, array.length);
	}

	@Test
	void testIdSetToString() {
		Set<Id<Link>> set = new LinkedHashSet<Id<Link>>();
		set.add(Id.create("Aaa", Link.class));
		set.add(Id.create("Bbb", Link.class));
		set.add(Id.create("Ddd", Link.class));
		set.add(Id.create("Ccc", Link.class));
		Assertions.assertEquals("Aaa,Bbb,Ddd,Ccc", CollectionUtils.idSetToString(set));		
	}

}
