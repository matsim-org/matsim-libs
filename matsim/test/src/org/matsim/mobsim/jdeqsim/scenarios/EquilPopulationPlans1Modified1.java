package org.matsim.mobsim.jdeqsim.scenarios;

import java.util.ArrayList;

import org.matsim.mobsim.jdeqsim.util.testable.PopulationModifier;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;


public class EquilPopulationPlans1Modified1 implements PopulationModifier{

	Population population=null;
	
	public Population getPopulation(){
		return population;
	} 
	

	public Population modifyPopulation(Population population) {
		// modify population: a plan was needed, which contained some properties to be compared with C++
		this.population=population;
		Person p=population.getPerson("1");
		Plan plan= p.getSelectedPlan();
		ArrayList<Object> actsLegs =plan.getActsLegs();
		((Act)actsLegs.get(0)).setEndTime(360);
		((Act)actsLegs.get(2)).setEndTime(900); // this requires immediate departure after arrival
		((Act)actsLegs.get(4)).setEndTime(2000);
		return population;
	}
	
	
	
	
}
