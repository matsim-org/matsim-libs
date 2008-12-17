package org.matsim.socialnetworks.algorithms;

import org.apache.log4j.Logger;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;


public class PersonForgetKnowledge extends AbstractPersonAlgorithm {

	double multiple=1;
	private final Logger log = Logger.getLogger(PersonForgetKnowledge.class);
	
	public PersonForgetKnowledge(double x) {
		super();
		this.multiple=x;
		
	}

	@Override
	public void run(Person person) {
		// TODO Auto-generated method stub
//		Remember a number of activities equal to at least the number of
//		acts per plan times the number of plans in memory

		Plan p = person.getSelectedPlan();
		int max_memory = (int) (p.getActsLegs().size()/2*person.getPlans().size()*multiple);
		person.getKnowledge().getMentalMap().manageMemory(max_memory, person.getPlans());		
	}

	
}
