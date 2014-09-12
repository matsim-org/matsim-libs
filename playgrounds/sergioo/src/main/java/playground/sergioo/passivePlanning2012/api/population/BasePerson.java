package playground.sergioo.passivePlanning2012.api.population;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public interface BasePerson extends Person {

	//Methods
	public Plan getBasePlan();

}
