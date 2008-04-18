package org.matsim.plans.filters;

import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.filters.PersonIdFilter;
import org.matsim.testcases.MatsimTestCase;

public class PersonIdFilterTest extends MatsimTestCase {

	public void testPersonIdFilter() throws Exception {
		
		// create fixture: 3 persons with special ids
		TreeMap<String, Person> persons = new TreeMap<String, Person>();
		for (String personId : new String[]{"1", "2102002002", "30"}) {
			persons.put(personId, new Person(personId, "f", "30", "yes", "yes", "yes"));
		}
 
		PersonIdFilter tenDigitIdFilter = new PersonIdFilter("[0-9]{10}");
		PersonIdFilter agent30Filter =  new PersonIdFilter("30");
		PersonIdFilter containsAOneFilter = new PersonIdFilter(".*1.*");
		PersonIdFilter between100And9999Filter = new PersonIdFilter("[1-9][0-9]{2,3}");
		
		assertFalse(tenDigitIdFilter.judge(persons.get("1")));
		assertTrue(tenDigitIdFilter.judge(persons.get("2102002002")));
		assertFalse(tenDigitIdFilter.judge(persons.get("30")));

		assertFalse(agent30Filter.judge(persons.get("1")));
		assertFalse(agent30Filter.judge(persons.get("2102002002")));
		assertTrue(agent30Filter.judge(persons.get("30")));

		assertTrue(containsAOneFilter.judge(persons.get("1")));
		assertTrue(containsAOneFilter.judge(persons.get("2102002002")));
		assertFalse(containsAOneFilter.judge(persons.get("30")));

		assertFalse(between100And9999Filter.judge(persons.get("1")));
		assertFalse(between100And9999Filter.judge(persons.get("2102002002")));
		assertFalse(between100And9999Filter.judge(persons.get("30")));
}
	
	
}
