package cba.resampling;

import java.util.Set;

import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface ChoiceSetProvider {

	public Set<Alternative> newChoiceSet(Person person, int numberOfDraws);
	
}
