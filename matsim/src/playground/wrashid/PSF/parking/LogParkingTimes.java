package playground.wrashid.PSF.parking;

import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicActivityEndEvent;
import org.matsim.api.basic.v01.events.BasicActivityStartEvent;
import org.matsim.api.basic.v01.events.handler.BasicActivityEndEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicActivityStartEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.events.handler.ActivityEndEventHandler;
import org.matsim.core.events.handler.ActivityStartEventHandler;

public class LogParkingTimes implements ActivityStartEventHandler, ActivityEndEventHandler {

	Controler controler;
	HashMap<Id, ParkingTimes> parkingTimes = new HashMap<Id, ParkingTimes>();

	public LogParkingTimes(Controler controler) {
		super();
		this.controler = controler;
	}

	public void handleEvent(ActivityStartEventImpl event) {
		// log the (start) time when the car departs
		Id personId = event.getPersonId();
		if (event.getActType().equalsIgnoreCase("parkingDeparture")) {
			ParkingTimes pTime = parkingTimes.get(personId);

			if (pTime != null) {
				/*
				 * this is not the first time we are departing, which means that the car was parked before 
				 */ 
				pTime.addParkLog(new ParkLog(event.getAct(),pTime.getLastParkingArrivalTime(),event.getTime()));
			} else {
				/*
				 * this means, that this is the first time the car departs (e.g.
				 * from home)
				 * 
				 * - set the time of leaving the parking for the first time
				 */
				
				parkingTimes.put(personId, new ParkingTimes());
				pTime=parkingTimes.get(personId);
				
				pTime.setFirstParkingDepartTime(event.getTime());
			}
		}
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	public void handleEvent(ActivityEndEventImpl event) {
		// log the (end) time when the car has been parked
		Id personId = event.getPersonId();
		if (event.getActType().equalsIgnoreCase("parkingArrival")) {
			ParkingTimes pTime = parkingTimes.get(personId);
			if (pTime==null) {
				parkingTimes.put(personId, new ParkingTimes());
				pTime=parkingTimes.get(personId);
			}
	
			pTime.setCarLastTimeParked(event.getTime());
			pTime.setCarLastTimeParkedActivity(event.getAct());
		}
	}

	// public Leg getNextLeg(String personId){
	// controler.getPopulation().getPersons().get(personId).getSelectedPlan().getNextLeg(act)
	// }

	public HashMap<Id, ParkingTimes> getParkingTimes() {
		return parkingTimes;
	}

	

	

}
