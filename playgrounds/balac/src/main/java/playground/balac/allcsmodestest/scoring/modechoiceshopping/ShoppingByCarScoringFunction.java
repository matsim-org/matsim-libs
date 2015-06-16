package playground.balac.allcsmodestest.scoring.modechoiceshopping;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;

public class ShoppingByCarScoringFunction implements BasicScoring {

	private final Plan plan;
	private Scenario scenario;
	private double score;
	public ShoppingByCarScoringFunction(final Plan plan, Scenario scenario) {
		
		this.plan = plan;
		this.scenario = scenario;
		this.reset();

	}
	
	private void reset() {
		
		score = 0.0;
	}
	
	@Override
	public void finish() {
		Leg previousLeg = null;

		for (PlanElement pe : plan.getPlanElements()) {
			
				
				if (pe instanceof Leg)
					previousLeg = (Leg) pe;
				else if (pe instanceof Activity) {
					
					if (((Activity) pe).getType().equals("shop")) {
						
						if (previousLeg.getMode().equals("car"))
							score += (double)scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "randValue");
							
					}
					
					
				}
			}
			
		}
		
	

	@Override
	public double getScore() {
		// TODO Auto-generated method stub
		return score;
	}

}
