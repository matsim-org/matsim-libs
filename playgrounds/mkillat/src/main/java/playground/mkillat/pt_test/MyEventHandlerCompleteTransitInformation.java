package playground.mkillat.pt_test;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

public class MyEventHandlerCompleteTransitInformation implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler{

	private List <CompleteTransitRoute> completeTransitRoutes = new ArrayList <CompleteTransitRoute>();
	private Scenario scenario;
	private Id busId;
	
	public MyEventHandlerCompleteTransitInformation (Scenario scenario, Id id){
		this.scenario = scenario;
		this.busId = id;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (event.getDriverId().equals(busId)){

			List <Double> arrives= new ArrayList <Double> ();
			List <Double> departures = new ArrayList <Double>();
			
			CompleteTransitRoute aa = new CompleteTransitRoute(busId, event.getVehicleId(), event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId(), event.getTime(), arrives, departures);
			completeTransitRoutes.add(aa);
		}
		
	}
		@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
			for (int i = 0; i < completeTransitRoutes.size(); i++) {
				if ((event.getVehicleId().equals(completeTransitRoutes.get(i).vehicleId)) && (event.getTime()>=completeTransitRoutes.get(i).transitDriverStartTime)){
					if(completeTransitRoutes.size()!=i+1){
						double eventTime = event.getTime();
						double nextStartTime = completeTransitRoutes.get(i+1).transitDriverStartTime;
						if (nextStartTime>eventTime){
							completeTransitRoutes.get(i).arrives.add(eventTime);
						}
					}else{
						completeTransitRoutes.get(i).arrives.add(event.getTime());
						
					}
				}
				
			}
		
	}
		
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		for (int i = 0; i < completeTransitRoutes.size(); i++) {
			if ((event.getVehicleId().equals(completeTransitRoutes.get(i).vehicleId)) && (event.getTime()>=completeTransitRoutes.get(i).transitDriverStartTime)){
				if(completeTransitRoutes.size()!=i+1){
					double eventTime = event.getTime();
					double nextStartTime = completeTransitRoutes.get(i+1).transitDriverStartTime;
					if (nextStartTime>eventTime){
						completeTransitRoutes.get(i).departures.add(eventTime);
					}
				}else{
					completeTransitRoutes.get(i).departures.add(event.getTime());
					
				}
			}
			
		}
	
}


	
	
	List <CompleteTransitRoute> getCompleteTransitRoute(){
		return completeTransitRoutes;
	}
}
