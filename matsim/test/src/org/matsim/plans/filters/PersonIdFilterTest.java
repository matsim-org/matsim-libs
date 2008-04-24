/* *********************************************************************** *
 * project: org.matsim.*
 * PersonIdFilterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.plans.filters;

import java.util.TreeMap;

import org.matsim.plans.Person;
import org.matsim.testcases.MatsimTestCase;

public class PersonIdFilterTest extends MatsimTestCase {

	/**
	 * @author komeiste
	 * @throws Exception
	 */
	public void testPersonIdFilter() throws Exception {

		// create fixture: 4 persons with special ids
		TreeMap<String, Person> persons = new TreeMap<String, Person>();
		for (String personId : new String[]{"1", "2102002002", "30", "3030"}) {
			persons.put(personId, new Person(personId, "f", "30", "yes", "yes", "yes"));
		}

		PersonIdFilter tenDigitIdFilter = new PersonIdFilter("[0-9]{10}", null);
		PersonIdFilter agent30IdFilter =  new PersonIdFilter("30", null);
		PersonIdFilter containsAOneIdFilter = new PersonIdFilter(".*1.*", null);
		PersonIdFilter between100And9999IdFilter = new PersonIdFilter("[1-9][0-9]{2,3}", null);

		assertFalse(tenDigitIdFilter.judge(persons.get("1")));
		assertTrue(tenDigitIdFilter.judge(persons.get("2102002002")));
		assertFalse(tenDigitIdFilter.judge(persons.get("30")));
		assertFalse(tenDigitIdFilter.judge(persons.get("3030")));

		assertFalse(agent30IdFilter.judge(persons.get("1")));
		assertFalse(agent30IdFilter.judge(persons.get("2102002002")));
		assertTrue(agent30IdFilter.judge(persons.get("30")));
		assertFalse(agent30IdFilter.judge(persons.get("3030")));

		assertTrue(containsAOneIdFilter.judge(persons.get("1")));
		assertTrue(containsAOneIdFilter.judge(persons.get("2102002002")));
		assertFalse(containsAOneIdFilter.judge(persons.get("30")));
		assertFalse(containsAOneIdFilter.judge(persons.get("3030")));

		assertFalse(between100And9999IdFilter.judge(persons.get("1")));
		assertFalse(between100And9999IdFilter.judge(persons.get("2102002002")));
		assertFalse(between100And9999IdFilter.judge(persons.get("30")));
		assertTrue(between100And9999IdFilter.judge(persons.get("3030")));
	}


}
