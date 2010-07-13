package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.population.PlanImpl;


public class ParkingBookKeeper implements ActivityStartEventHandler, ActivityEndEventHandler {

	private Controler controler;
	private ParkingOccupancyMaintainer parkingOccupancyMaintainer=new ParkingOccupancyMaintainer();

	public ParkingBookKeeper(Controler controler){
		this.controler=controler;
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		System.out.println(event.toString());
//		Person person= controler.getPopulation().getPersons().get(event.getPersonId());
//		Plan originalPlan=person.getSelectedPlan();
//		PlanImpl newPlan = new PlanImpl(person);
//		Activity homeAct= (Activity) originalPlan.getPlanElements().get(0);
		
		
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getLinkId()==null){ 
			//System.out.println(event.toString());
		}
		
		//System.out.println(event.toString());
		
		
		
		
		
		
		
		// add score.
		
		controler.getEvents().processEvent(new AgentMoneyEventImpl(3600.0, event.getPersonId(), 3.4));
	}

	// code snipet for adding scores:
	//events.processEvent(new AgentMoneyEvent(3600.0, person, 3.4));
	
	public void performScoring(){
		
	}
	
	
}
