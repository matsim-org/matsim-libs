package playground.jbischoff.matsimha2;

import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;

public class ChangeActivityTimesAdvanced implements PlanStrategyModule, ActivityStartEventHandler {

	
	private Map<Id,Double> lastWorkActivitEndTimes;
	
//	Variation in Seconds
	
	public ChangeActivityTimesAdvanced(){
		this.lastWorkActivitEndTimes = new HashMap<Id, Double>(); 
		
	}
	
	
	public void prepareReplanning() {
		System.out.println("prepare.");
	}

	public void handlePlan(Plan plan) {
				
		Activity homeact = (Activity) plan.getPlanElements().get(0);
		Activity workact = (Activity) plan.getPlanElements().get(2);
		Leg legtowork = (Leg) plan.getPlanElements().get(1);
						
		double offset = homeact.getEndTime()-this.getLastWorkActivityStartTime(plan.getPerson().getId());
		System.out.println("Offset for agent: "+plan.getPerson().getId()+" is "+ offset +"with h-et:"+homeact.getEndTime()+" and LWST "+this.getLastWorkActivityStartTime(plan.getPerson().getId()));
		
		
		homeact.setEndTime(homeact.getEndTime()+offset);
		legtowork.setDepartureTime(legtowork.getDepartureTime()+offset);
		
		workact.setEndTime(workact.getEndTime()+offset);
		
		
		
	}

	public void finishReplanning() {
		System.out.println("Finish.");
	}

	public void reset(int iteration) {
		System.out.println("Reset.");
	}

	public void handleEvent(ActivityStartEvent event) {
		
		
		Id person = event.getPersonId();
		
		
		
		if (event.getActType().equals("work")){
			this.lastWorkActivitEndTimes.put(person, event.getTime());
//			System.out.println("Event occured");
			
		}
	}
	
	
	
	public Double getLastWorkActivityStartTime(Id personId){
		double endtime = 0.;
		if (this.lastWorkActivitEndTimes.containsKey(personId)){
			endtime = this.lastWorkActivitEndTimes.get(personId);
		}
		return endtime;
	}



	


}
