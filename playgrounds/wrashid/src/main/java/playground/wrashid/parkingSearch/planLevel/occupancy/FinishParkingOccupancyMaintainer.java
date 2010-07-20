package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.parkingSearch.planLevel.analysis.ParkingOccupancyAnalysisWriter;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class FinishParkingOccupancyMaintainer implements AfterMobsimListener {

	private ParkingOccupancyMaintainer parkingOccupancyMaintainer;

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		ParkingRoot.getParkingOccupancyMaintainer().closeAllLastParkings();
		
		// write also the occupancy statistics at this stage of the simulation

		ParkingOccupancyAnalysisWriter poaWriter = new ParkingOccupancyAnalysisWriter(ParkingRoot.getParkingOccupancyMaintainer()
				.getParkingOccupancyBins(), ParkingRoot.getParkingCapacity());
		poaWriter.write(event.getControler().getControlerIO()
				.getIterationFilename(event.getControler().getIterationNumber(), "parkingOccupancyStatistics.txt"));
	}

}
