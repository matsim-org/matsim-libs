package playground.wrashid.parkingSearch.planLevel;

import org.matsim.core.controler.Controler;

import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.parkingSearch.planLevel.init.InitializeParkings;
import playground.wrashid.parkingSearch.planLevel.occupancy.FinishParkingOccupancyMaintainer;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingBookKeeper;

public class BaseControlerScenario {
	
	public ParkingBookKeeper parkingBookKeeper;

	public BaseControlerScenario(Controler controler) {
		controler.setOverwriteFiles(true);

		// add controler for initialization
		controler.addControlerListener(new InitializeParkings());

		// add handlers (e.g. parking book keeping)
		EventHandlerAtStartupAdder eventHandlerAdder = new EventHandlerAtStartupAdder();
		this.parkingBookKeeper = new ParkingBookKeeper(controler);
		eventHandlerAdder.addEventHandler(parkingBookKeeper);
		controler.addControlerListener(eventHandlerAdder);
		
		controler.addControlerListener(new FinishParkingOccupancyMaintainer(parkingBookKeeper.getParkingOccupancyMaintainer()));
		
		
	}

}
