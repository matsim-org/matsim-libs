package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class FinishParkingOccupancyMaintainer implements AfterMobsimListener {

	private ParkingOccupancyMaintainer parkingOccupancyMaintainer;

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		ParkingRoot.getParkingOccupancyMaintainer().closeAllLastParkings();
	}

}
