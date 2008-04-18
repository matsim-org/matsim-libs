package org.matsim.plans.filters;

import java.util.regex.Pattern;

import org.matsim.plans.Person;

public class PersonIdFilter extends AbstractPersonFilter {

	private String personIdPattern = null;
	
	public PersonIdFilter(String personIdPattern) {
		super();
		this.personIdPattern = personIdPattern;
	}

	@Override
	public boolean judge(Person person) {

		String personId = person.getId().toString();
		
		if (Pattern.matches(this.personIdPattern, personId)) {
			return true;
		}
		
		return false;
	}

}
