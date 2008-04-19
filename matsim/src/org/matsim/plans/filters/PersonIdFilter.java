package org.matsim.plans.filters;

import java.util.regex.Pattern;

import org.matsim.plans.Person;
import org.matsim.plans.algorithms.PersonAlgorithmI;

/**
 * Filters persons whose id matches a certain pattern (regular expression).
 *
 * @author kmeister
 */
public class PersonIdFilter extends AbstractPersonFilter {

	private String personIdPattern = null;
	
	public PersonIdFilter(String personIdPattern, PersonAlgorithmI nextAlgorithm) {
		super();
		this.personIdPattern = personIdPattern;
		this.nextAlgorithm = nextAlgorithm;
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
