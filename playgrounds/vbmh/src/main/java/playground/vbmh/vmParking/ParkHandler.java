package playground.vbmh.vmParking;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.LegImpl;

import java.util.Map;

/**
 * Handles the AktivityStartEvent and starts the parking process; Same for the ActivityEndEvent; Additionally all the events of
 * each agent are counted to be able to get the activity which the agent currently performs.
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */



public class ParkHandler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {

	ParkControl parkControl = new ParkControl();
	
	public ParkControl getParkControl() {
		return parkControl;
	}

	public void setPark_control(ParkControl park_control) {
		this.parkControl = park_control;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// TODO Auto-generated method stub
	
		//Activitys zaehlen:

        Person person = parkControl.controller.getScenario().getPopulation().getPersons().get(event.getPersonId());
		Map<String, Object> personAttributes = person.getCustomAttributes();
		
		String legMode = null;
		
		if (personAttributes.get("ActCounter")!= null){
			Integer counter = (Integer) personAttributes.get("ActCounter");
			LegImpl leg = (LegImpl) person.getSelectedPlan().getPlanElements().get(counter);
			legMode = leg.getMode();
			counter ++;
			personAttributes.put("ActCounter", counter);
		} else{
			personAttributes.put("ActCounter", 0);
		}
		
		
		
		
		if(legMode=="car"){
			if(!event.getActType().equals("ParkO")&&!event.getActType().equals("ParkP")){
				parkControl.park(event);
			}
		}
			
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub

        Person person = parkControl.controller.getScenario().getPopulation().getPersons().get(event.getPersonId());
		Map<String, Object> personAttributes = person.getCustomAttributes();
		
		
		if (personAttributes.get("ActCounter")!= null){
			Integer counter = (Integer) personAttributes.get("ActCounter");
			counter ++;
			personAttributes.put("ActCounter", counter);
		} else{
			System.out.println("F E H L E R agent ohne ActCounter unterwegs"); //should not happen
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// TODO Auto-generated method stub
		parkControl.leave(event);
	}

}
