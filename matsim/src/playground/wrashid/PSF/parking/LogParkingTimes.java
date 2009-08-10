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
import org.matsim.core.events.AgentMoneyEvent;

public class LogParkingTimes implements BasicActivityStartEventHandler, BasicActivityEndEventHandler {

	Controler controler;
	HashMap<Id, ParkingTimes> parkingTimes = new HashMap<Id, ParkingTimes>();

	public LogParkingTimes(Controler controler) {
		super();
		this.controler = controler;
	}

	public void handleEvent(BasicActivityStartEvent event) {
		// log the (start) time when the car departs
		Id personId = event.getPersonId();
		if (event.getActType().equalsIgnoreCase("parkingDepart")) {
			ParkingTimes pTime = parkingTimes.get(personId);

			if (pTime != null) {
				/*
				 * this is not the first time we are departing, which means that the car was parked before 
				 */ 
				pTime.addParkLog(new ParkLog(event.getLinkId(),pTime.getCarLastTimeParked(),event.getTime()));
			} else {
				/*
				 * this means, that this is the first time the car departs (e.g.
				 * from home) and we can ignore this
				 */
			}
		}
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	public void handleEvent(BasicActivityEndEvent event) {
		// log the (end) time when the car has been parked
		Id personId = event.getPersonId();
		if (event.getActType().equalsIgnoreCase("parkingArrival")) {
			if (!parkingTimes.containsKey(personId)) {
				parkingTimes.put(personId, new ParkingTimes());
			}
			ParkingTimes pTime = parkingTimes.get(personId);

			pTime.setCarLastTimeParked(event.getTime());
			pTime.setCarLastTimeParkedLinkId(event.getLinkId());
		}
	}

	// public Leg getNextLeg(String personId){
	// controler.getPopulation().getPersons().get(personId).getSelectedPlan().getNextLeg(act)
	// }

	public HashMap<Id, ParkingTimes> getParkingTimes() {
		return parkingTimes;
	}

	

}
