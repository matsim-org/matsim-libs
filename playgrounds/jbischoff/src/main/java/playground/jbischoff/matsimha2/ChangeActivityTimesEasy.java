package playground.jbischoff.matsimha2;

import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;

public class ChangeActivityTimesEasy implements PlanStrategyModule, ActivityEndEventHandler {

	
	private double VARIATION;
public double getVARIATION() {
		return VARIATION;
	}


	public void setVARIATION(double variation) {
		VARIATION = variation;
	}

	//	Variation in Seconds
	private Random rnd;
	
	public ChangeActivityTimesEasy(){
		this.VARIATION = 3600.0;
		 this.rnd = new Random();
		
	}
	
	
	@Override
	public void prepareReplanning() {
		System.out.println("prepare.");
	}

	@Override
	public void handlePlan(Plan plan) {
		
		
		Activity act = (Activity) plan.getPlanElements().get(0);
		Leg leg = (Leg) plan.getPlanElements().get(1);
		int vz = 0;
		if (rnd.nextBoolean()) vz = 1;
		else vz = -1;
				
		double personalVar = rnd.nextDouble() * vz * this.VARIATION;
		double oldendTime = act.getEndTime();
		act.setEndTime(oldendTime+personalVar);
		leg.setDepartureTime(oldendTime+personalVar);
	
	}

	@Override
	public void finishReplanning() {
		System.out.println("Finish.");
	}

	@Override
	public void reset(int iteration) {
		System.out.println("Reset.");
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		
	}
	


}
