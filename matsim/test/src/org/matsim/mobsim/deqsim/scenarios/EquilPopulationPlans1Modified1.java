package org.matsim.mobsim.deqsim.scenarios;

import java.util.ArrayList;

import org.matsim.mobsim.deqsim.util.testable.PopulationModifier;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;


public class EquilPopulationPlans1Modified1 implements PopulationModifier{

	Population population=null;
	
	public Population getPopulation(){
		return population;
	} 
	

	public Population modifyPopulation(Population population) {
		// modify population: we need act end time (plan has only duration)
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
