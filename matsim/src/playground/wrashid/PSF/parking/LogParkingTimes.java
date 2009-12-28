package playground.wrashid.PSF.parking;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.handler.DeprecatedActivityEndEventHandler;
import org.matsim.core.events.handler.DeprecatedActivityStartEventHandler;

public class LogParkingTimes implements DeprecatedActivityStartEventHandler, DeprecatedActivityEndEventHandler {

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
				 * this is not the first time we are departing, which means that
				 * the car was parked before
				 */
				pTime.addParkLog(new ParkLog(event.getAct(), pTime.getLastParkingArrivalTime(), event.getTime()));
			} else {
				/*
				 * this means, that this is the first time the car departs (e.g.
				 * from home)
				 * 
				 * - set the time of leaving the parking for the first time
				 */

				parkingTimes.put(personId, new ParkingTimes());
				pTime = parkingTimes.get(personId);

				pTime.setFirstParkingDepartTime(event.getTime());
			}
		}
	}

	public void reset(int iteration) {
		parkingTimes = new HashMap<Id, ParkingTimes>();
	}

	public void handleEvent(ActivityEndEventImpl event) {
		// log the (end) time when the car has been parked
		Id personId = event.getPersonId();
		if (event.getActType().equalsIgnoreCase("parkingArrival")) {
			ParkingTimes pTime = parkingTimes.get(personId);
			if (pTime == null) {
				parkingTimes.put(personId, new ParkingTimes());
				pTime = parkingTimes.get(personId);
			}

			// if the day is long than 24 hours, then wrap the time
			/*
			 * if (event.getTime()>86400){ double time=event.getTime(); while
			 * (time>86400){ time-=86400; } pTime.setCarLastTimeParked(time); }
			 * else {
			 * 
			 * }
			 */
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
