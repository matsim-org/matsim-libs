package playground.jbischoff.matsimha2;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

public class ChangeActivityTimesAdvanced implements PlanStrategyModule, ActivityStartEventHandler {

	
	private Map<Id<Person>,Double> lastWorkActivitEndTimes;
	
//	Variation in Seconds
	
	public ChangeActivityTimesAdvanced(){
		this.lastWorkActivitEndTimes = new HashMap<>(); 
		
	}
	
	
	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		System.out.println("prepare.");
	}

	@Override
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

	@Override
	public void finishReplanning() {
		System.out.println("Finish.");
	}

	@Override
	public void reset(int iteration) {
		System.out.println("Reset.");
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		
		Id<Person> person = event.getPersonId();
		
		
		
		if (event.getActType().equals("work")){
			this.lastWorkActivitEndTimes.put(person, event.getTime());
//			System.out.println("Event occured");
			
		}
	}
	
	
	
	public Double getLastWorkActivityStartTime(Id<Person> personId){
		double endtime = 0.;
		if (this.lastWorkActivitEndTimes.containsKey(personId)){
			endtime = this.lastWorkActivitEndTimes.get(personId);
		}
		return endtime;
	}



	


}
