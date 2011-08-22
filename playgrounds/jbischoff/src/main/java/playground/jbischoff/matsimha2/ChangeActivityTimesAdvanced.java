package playground.jbischoff.matsimha2;

import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;

public class ChangeActivityTimesAdvanced implements PlanStrategyModule, ActivityEndEventHandler {

	
	private Map<Id,Double> lastWorkActivitStartTimes;
	
//	Variation in Seconds
	
	public ChangeActivityTimesAdvanced(){
		this.lastWorkActivitStartTimes = new HashMap<Id, Double>(); 
		
	}
	
	
	@Override
	public void prepareReplanning() {
		System.out.println("prepare.");
	}

	@Override
	public void handlePlan(Plan plan) {
				
		Activity homeact = (Activity) plan.getPlanElements().get(0);
		Activity workact = (Activity) plan.getPlanElements().get(2);
		Leg legtowork = (Leg) plan.getPlanElements().get(1);
//		Leg legfromwork = (Leg) plan.getPlanElements().get(3);
						
		double offset = workact.getEndTime()-this.getLastWorkActivityEndTime(plan.getPerson().getId());
		System.out.println("Offset for agent:"+plan.getPerson().getId()+" is "+ offset +"with et:"+workact.getEndTime()+" and LET "+this.getLastWorkActivityEndTime(plan.getPerson().getId()));
		
		
		homeact.setEndTime(homeact.getEndTime()+offset);
		legtowork.setDepartureTime(legtowork.getDepartureTime()+offset);
		
		workact.setEndTime(workact.getEndTime()+offset);
		
		
		
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
		
		
		Id person = event.getPersonId();
		
//		System.out.println(event.getActType() + " Event occured");
		
		
		if (event.getActType().equals("w")){
			this.lastWorkActivitStartTimes.put(person, event.getTime());
			System.out.println("Event occured");
			
		}
	}
	
	
	
	public Double getLastWorkActivityEndTime(Id personId){
		double endtime = 0.;
		if (this.lastWorkActivitStartTimes.containsKey(personId)){
			endtime = this.lastWorkActivitStartTimes.get(personId);
		}
		return endtime;
	}



	


}
