package playground.sergioo.passivePlanning2012.core.population;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.PersonImpl;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.api.population.BasePlan;

public class BaseSocialPerson extends PersonImpl implements BasePerson {

	//Attributes
	private final BasePlan basePlan = new BasePlanImpl(this);
	
	//Constructors
	public BaseSocialPerson(Id id) {
		super(id);
	}

	//Methods
	@Override
	public BasePlan getBasePlan() {
		return basePlan;
	}

}
