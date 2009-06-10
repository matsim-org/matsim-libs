package playground.mmoyo.Validators;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.network.Network;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;

/**
 *   returns counts of a plan for performance analysis
 */
public class PlanValidator {
	PathValidator pathValidator= new PathValidator (); 

	public PlanValidator() {
	}

	public boolean hasAllLegs(Plan plan){
		int i=0;
		for (BasicPlanElement basicPlanElement: plan.getPlanElements()){
			if (++i%2==0){  //if (basicPlanElement instanceof Leg) {
				Leg leg = (Leg)basicPlanElement;
				//if (!pathValidator.isValid(leg.getRoute())){ }
				return false;
			}
		}
		return true;
	}
	
	
}