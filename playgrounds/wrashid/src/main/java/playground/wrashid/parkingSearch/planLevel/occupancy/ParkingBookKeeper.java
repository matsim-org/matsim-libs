package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
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

import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class ParkingBookKeeper implements ActivityStartEventHandler, ActivityEndEventHandler {

	private Controler controler;
	private ParkingOccupancyMaintainer parkingOccupancyMaintainer;

	// true: starts parking/arrival
	// false: end parking/departure
	HashMap<Id, Boolean> personParkingMode = new HashMap<Id, Boolean>();

	
	
	
	public ParkingOccupancyMaintainer getParkingOccupancyMaintainer() {
		return parkingOccupancyMaintainer;
	}

	public ParkingBookKeeper(Controler controler) {
		this.controler = controler;
		parkingOccupancyMaintainer=new ParkingOccupancyMaintainer(controler);
		
		ParkingRoot.setParkingOccupancyMaintainer(parkingOccupancyMaintainer);
	}

	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();

		if (event.getActType().equalsIgnoreCase("parking")) {
			if (!personParkingMode.containsKey(personId)) {
				// init personParkingMode (first parking action in the morning
				// is departure)
				personParkingMode.put(personId, false);
			}

			if (personParkingMode.get(personId)==true){
				parkingOccupancyMaintainer.logArrivalAtParking(event);
			}
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();

		if (event.getActType().equalsIgnoreCase("parking")) {
			if (personParkingMode.get(personId)==false){
				parkingOccupancyMaintainer.logDepartureFromParking(event);
				personParkingMode.put(personId, true);
			} else {
				personParkingMode.put(personId, false);
			}
		}
	}



	public void performScoring() {
		
		// controler.getEvents().processEvent(new AgentMoneyEventImpl(3600.0,
		// event.getPersonId(), 3.4));
		
		// code snipet for adding scores:
		// events.processEvent(new AgentMoneyEvent(3600.0, person, 3.4));
	}

}
