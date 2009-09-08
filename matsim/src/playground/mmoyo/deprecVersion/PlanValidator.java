package playground.mmoyo.deprecVersion;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;


/**
 *   returns counts of a plan for performance analysis
 */
public class PlanValidator {
	PathValidator pathValidator= new PathValidator (); 

	public PlanValidator() {
	}

	public boolean hasAllLegs(final PlanImpl plan){
		for (BasicPlanElement basicPlanElement: plan.getPlanElements()){
			if (basicPlanElement instanceof LegImpl) {
				//Leg leg = (Leg)basicPlanElement;
				//if (!pathValidator.isValid(leg.getRoute())){ }
				return false;
			}
		}
		return true;
	}
	
	
}