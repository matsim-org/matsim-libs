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

package playground.meisterk.org.matsim.run.westumfahrung;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.testcases.MatsimTestCase;

public class PersonIdFilterTest extends MatsimTestCase {

	/**
	 * @author komeiste
	 * @throws Exception
	 */
	public void testPersonIdFilter() throws Exception {

		// create fixture: 4 persons with special ids
		TreeMap<String, PersonImpl> persons = new TreeMap<String, PersonImpl>();
		for (String personId : new String[]{"1", "2102002002", "30", "3030"}) {
			persons.put(personId, new PersonImpl(Id.create(personId, Person.class)));
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
