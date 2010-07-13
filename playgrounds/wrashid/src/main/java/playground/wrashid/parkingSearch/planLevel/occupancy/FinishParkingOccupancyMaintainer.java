package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

public class FinishParkingOccupancyMaintainer implements AfterMobsimListener {

	private ParkingOccupancyMaintainer parkingOccupancyMaintainer;

	public FinishParkingOccupancyMaintainer(ParkingOccupancyMaintainer parkingOccupancyMaintainer) {
		this.parkingOccupancyMaintainer = parkingOccupancyMaintainer;
	}

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		parkingOccupancyMaintainer.closeAllLastParkings();
	}

}
