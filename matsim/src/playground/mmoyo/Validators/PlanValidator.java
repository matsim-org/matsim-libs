package playground.mmoyo.Validators;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;


/**
 *   returns counts of a plan for performance analysis
 */
public class PlanValidator {
	PathValidator pathValidator= new PathValidator (); 

	public PlanValidator() {
	}

	public boolean hasAllLegs(final Plan plan){
		for (BasicPlanElement basicPlanElement: plan.getPlanElements()){
			if (basicPlanElement instanceof Leg) {
				//Leg leg = (Leg)basicPlanElement;
				//if (!pathValidator.isValid(leg.getRoute())){ }
				return false;
			}
		}
		return true;
	}
	
	
}