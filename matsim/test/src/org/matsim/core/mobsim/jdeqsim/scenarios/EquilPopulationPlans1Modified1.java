package org.matsim.core.mobsim.jdeqsim.scenarios;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.jdeqsim.util.PopulationModifier;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationImpl;


public class EquilPopulationPlans1Modified1 implements PopulationModifier{

	private PopulationImpl population=null;
	
	public PopulationImpl getPopulation(){
		return population;
	} 
	
	public PopulationImpl modifyPopulation(PopulationImpl population) {
		// modify population: a plan was needed, which contained some properties to be compared with C++
		this.population=population;
		Person p=population.getPersons().get(new IdImpl("1"));
		Plan plan= p.getSelectedPlan();
		List<? extends PlanElement> actsLegs = plan.getPlanElements();
		((ActivityImpl)actsLegs.get(0)).setEndTime(360);
		((ActivityImpl)actsLegs.get(2)).setEndTime(900); // this requires immediate departure after arrival
		((ActivityImpl)actsLegs.get(4)).setEndTime(2000);
		return population;
	}

}
