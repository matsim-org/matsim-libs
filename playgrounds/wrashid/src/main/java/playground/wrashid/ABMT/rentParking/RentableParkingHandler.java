package playground.wrashid.ABMT.rentParking;

import org.matsim.contrib.parking.PC2.simulation.ParkingArrivalEvent;
import org.matsim.contrib.parking.PC2.simulation.ParkingArrivalEventHandler;
import org.matsim.contrib.parking.PC2.simulation.ParkingDepartureEvent;
import org.matsim.contrib.parking.PC2.simulation.ParkingDepartureEventHandler;
import org.matsim.contrib.parking.lib.DebugLib;


public class RentableParkingHandler implements ParkingArrivalEventHandler, ParkingDepartureEventHandler {

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ParkingArrivalEvent event) {
		DebugLib.emptyFunctionForSettingBreakPoint();
	}

	@Override
	public void handleEvent(ParkingDepartureEvent event) {
		DebugLib.emptyFunctionForSettingBreakPoint();
	}

}
