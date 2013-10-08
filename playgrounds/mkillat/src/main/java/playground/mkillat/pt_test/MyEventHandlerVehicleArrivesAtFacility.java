package playground.mkillat.pt_test;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;

public class MyEventHandlerVehicleArrivesAtFacility implements VehicleArrivesAtFacilityEventHandler {

	List <Id> vehicleArrivestAtFacilityTimes;
	Scenario scenario;
	List <Id> facilityIds;
	
	
	public MyEventHandlerVehicleArrivesAtFacility(Scenario scenario, List <Id> facilityIds){
		this.scenario = scenario;
		this.facilityIds = facilityIds;

	}
	
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		for(int i=0;i<facilityIds.size(); i++  ){
			if(event.getFacilityId().equals(facilityIds.get(i))){
				System.out.println("ich hab was gefunden: " + event.getVehicleId());
//				vehicleArrivestAtFacilityTimes.add(event.getVehicleId());
			}
		}
	}
	
	List <Id> getVehicleArrivestAtFacilityTime(){
		return vehicleArrivestAtFacilityTimes;
	}

}
