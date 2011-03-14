/**
 * 
 */
package playground.mzilske.deteval;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.population.Person;

public class Case {
	
	public static class Car { 
		protected int hubraum;
		protected int baujahr;
		protected int antriebsart;
		protected int primary_user;
	}
	
	public Collection<Person> members = new ArrayList<Person>();
	
	public int income;

	protected Collection<Car> cars = new ArrayList<Car>();
	
}

