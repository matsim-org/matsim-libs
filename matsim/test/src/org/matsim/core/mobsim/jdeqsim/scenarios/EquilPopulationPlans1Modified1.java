package org.matsim.core.mobsim.jdeqsim.scenarios;

import java.util.List;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.jdeqsim.util.PopulationModifier;
import org.matsim.core.population.ActivityImpl;


public class EquilPopulationPlans1Modified1 implements PopulationModifier{

	private Population population=null;
	
	public Population getPopulation(){
		return population;
	} 
	
	public Population modifyPopulation(Population population) {
		// modify population: a plan was needed, which contained some properties to be compared with C++
		this.population=population;
		Person p=population.getPersons().get(new IdImpl("1"));
		Plan plan= p.getSelectedPlan();
		List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
		((ActivityImpl)actsLegs.get(0)).setEndTime(360);
		((ActivityImpl)actsLegs.get(2)).setEndTime(900); // this requires immediate departure after arrival
		((ActivityImpl)actsLegs.get(4)).setEndTime(2000);
		return population;
	}

}
