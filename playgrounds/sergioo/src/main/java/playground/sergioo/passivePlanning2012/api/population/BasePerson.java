package playground.sergioo.passivePlanning2012.api.population;

import org.matsim.api.core.v01.population.Person;

public interface BasePerson extends Person {

	//Methods
	public BasePlan getBasePlan();
	public boolean isPlanning();

}
