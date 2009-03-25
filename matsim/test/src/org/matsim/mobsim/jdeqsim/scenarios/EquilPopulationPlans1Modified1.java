package org.matsim.mobsim.jdeqsim.scenarios;

import java.util.List;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.basic.v01.IdImpl;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.mobsim.jdeqsim.util.testable.PopulationModifier;


public class EquilPopulationPlans1Modified1 implements PopulationModifier{

	Population population=null;
	
	public Population getPopulation(){
		return population;
	} 
	
	public Population modifyPopulation(Population population) {
		// modify population: a plan was needed, which contained some properties to be compared with C++
		this.population=population;
		Person p=population.getPerson(new IdImpl("1"));
		Plan plan= p.getSelectedPlan();
		List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
		((Activity)actsLegs.get(0)).setEndTime(360);
		((Activity)actsLegs.get(2)).setEndTime(900); // this requires immediate departure after arrival
		((Activity)actsLegs.get(4)).setEndTime(2000);
		return population;
	}

}
