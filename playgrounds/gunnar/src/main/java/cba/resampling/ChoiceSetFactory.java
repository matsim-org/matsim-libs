package cba.resampling;

import java.util.Set;

import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface ChoiceSetFactory<A extends Alternative> {

	public Set<A> newChoiceSet(Person person);
	
}
