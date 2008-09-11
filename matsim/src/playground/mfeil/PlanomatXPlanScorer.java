/**
 * 
 */
package playground.mfeil;

import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.scoring.PlanScorer;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

/**
 * @author Matthias Feil
 *
 */
public class PlanomatXPlanScorer extends PlanScorer {

	/**
	 * @param factory
	 */
	private ScoringFunctionFactory factory;
	
	public PlanomatXPlanScorer(ScoringFunctionFactory factory) {
		super(factory);
		this.factory = factory;
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public double getScore(final Plan plan) {
		System.out.println("PlanomatXPlanScorer wurde aufgerufen!");
		ScoringFunction function = this.factory.getNewScoringFunction(plan);
		org.matsim.population.Leg leg;
		for (int i = 1; i < plan.getActsLegs().size(); i++) {
			if (i % 2 != 0) {
				leg = (Leg) plan.getActsLegs().get(i);
				double depTime = 0;
				double arrTime = 0;
				for (int x = 0;x<i;x++){
					if (x % 2 == 0){
						Act actHelp = (Act) (plan.getActsLegs().get(x));
						if (actHelp.getDur()>=0){
								depTime = actHelp.getDur() + depTime;
						}
						else if (actHelp.getEndTime()>=0){
							depTime = actHelp.getEndTime() + depTime;
						}
						else {
							depTime = 24*3600;
						}
					}
					//else if (legHelp.getTravTime()>=0){
						//depTime = legHelp.getTravTime() + depTime;
					//}
				}
				//System.out.println("depTime ist "+depTime);
				function.startLeg(depTime, leg);
				
				if (leg.getTravTime()>=0){
					arrTime = depTime + leg.getTravTime();
				}
				else{
					arrTime = depTime;
				}
				//System.out.println("arrTime ist "+arrTime);
				function.endLeg(arrTime);
			}
		}
		function.finish();
		return function.getScore();
	}
}
