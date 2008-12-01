package playground.wrashid.DES;

import java.util.ArrayList;

import org.matsim.events.BasicEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;

public class EndLegMessage extends EventMessage {

	public EndLegMessage(Scheduler scheduler,Vehicle vehicle) {
		super(scheduler,vehicle);
		eventType=SimulationParameters.END_LEG;
		priority=SimulationParameters.PRIORITY_ARRIVAL_MESSAGE;
	}
	

	@Override
	public void selfhandleMessage() {
		//vehicle.leavePreviousRoad();
		
		
		
		// schedule next leg, if there are more legs, else end trip (TODO)
		
		// start next leg
		// assumption: actions and legs are alternating in plans file
		vehicle.setLegIndex(vehicle.getLegIndex()+2);
		vehicle.setLinkIndex(-1);
		
			Plan plan = vehicle.getOwnerPerson().getSelectedPlan(); // that's the plan the
														// person will execute
			ArrayList<Object> actsLegs = plan.getActsLegs();
		if ((actsLegs.size()>vehicle.getLegIndex())){	
			vehicle.setCurrentLeg((Leg) actsLegs.get(vehicle.getLegIndex()));
			// the leg the agent performs
			double departureTime = vehicle.getCurrentLeg().getDepartureTime(); // the time the agent
															// departs at this
															// activity
			
			
			// if the departureTime for the leg is in the past, then set it to the current simulation time
			// this avoids that messages in the past are put into the scheduler (which makes no sense anyway)
			if (departureTime<scheduler.getSimTime()){
				departureTime=scheduler.getSimTime();
			}
			
	
			
			// this is the link, where the first activity took place
			vehicle.setCurrentLink(((Act) actsLegs.get(vehicle.getLegIndex()-1)).getLink());
	
			Road road=Road.allRoads.get(vehicle.getCurrentLink().getId().toString());
			vehicle.scheduleStartingLegMessage(departureTime, road);
		}
		
	}
	
	public void logEvent() {
		BasicEvent event=null;
		
		if (eventType.equalsIgnoreCase(SimulationParameters.END_LEG)){
			event=new AgentArrivalEvent(this.getMessageArrivalTime(),vehicle.getOwnerPerson().getId().toString(),vehicle.getCurrentLink().getId().toString(),vehicle.getLegIndex()-1);
		}
		
		SimulationParameters.events.processEvent(event);
	}
	
}
