package playground.mkillat.pt_test;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;

public class MyEventHandlerCompleteTransitInformation implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler{

	private List <CompleteTransitRoute> completeTransitRoutes = new ArrayList <CompleteTransitRoute>();
	private List <CompleteTransitRoute> platzhalter = new ArrayList<CompleteTransitRoute>();
	private Id lineId;
	private Id routeId;
	private Scenario scenario;
	
	
	public MyEventHandlerCompleteTransitInformation (Scenario scenario, Id lineId, Id routeId){
		this.lineId = lineId;
		this.routeId = routeId;
		this.scenario = scenario;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
			if (event.getTransitLineId().equals(lineId) && event.getTransitRouteId().equals(routeId) && completeTransitRoutes.size()<3){
				
				List <Double> arrives= new ArrayList <Double> ();
				List <Double> departures = new ArrayList <Double>();
				
				CompleteTransitRoute aa = new CompleteTransitRoute(event.getDriverId(), event.getVehicleId(), event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId(), event.getTime(), arrives, departures);
				completeTransitRoutes.add(aa);

			
			}
			if (completeTransitRoutes.size()!=0){
				if(event.getVehicleId().equals(completeTransitRoutes.get(0).vehicleId) && event.getTime()> completeTransitRoutes.get(0).transitDriverStartTime){
			
				List <Double> arrives= new ArrayList <Double> ();
				List <Double> departures = new ArrayList <Double>();
			
				CompleteTransitRoute aa = new CompleteTransitRoute(event.getDriverId(), event.getVehicleId(), event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId(), event.getTime(), arrives, departures);
				platzhalter.add(aa);
		}
		}
		
		
	}
		@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
			if(completeTransitRoutes.size()!=0){
				if ((event.getVehicleId().equals(completeTransitRoutes.get(0).vehicleId)) && (event.getTime()>=completeTransitRoutes.get(0).transitDriverStartTime) ){
					if(completeTransitRoutes.size()!=1 && event.getTime()<completeTransitRoutes.get(1).transitDriverStartTime){
						completeTransitRoutes.get(0).arrives.add(event.getTime());
							
						}
				if(platzhalter.size()==0){
						completeTransitRoutes.get(0).arrives.add(event.getTime());
					
					}
				if(event.getVehicleId().equals(completeTransitRoutes.get(0).transitDriverStartTime) && event.getTime() < platzhalter.get(0).transitDriverStartTime){
					completeTransitRoutes.get(0).arrives.add(event.getTime());
				}
					}
			}
			
				
			
		
	}
		
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		if(completeTransitRoutes.size()!=0){
			if ((event.getVehicleId().equals(completeTransitRoutes.get(0).vehicleId)) && (event.getTime()>=completeTransitRoutes.get(0).transitDriverStartTime) ){
				if(completeTransitRoutes.size()!=1 && event.getTime()<completeTransitRoutes.get(1).transitDriverStartTime){
					completeTransitRoutes.get(0).departures.add(event.getTime());
						
					}
			if(platzhalter.size()==0){
					completeTransitRoutes.get(0).departures.add(event.getTime());
				
				}
			if(event.getVehicleId().equals(completeTransitRoutes.get(0).transitDriverStartTime) && event.getTime() < platzhalter.get(0).transitDriverStartTime){
				completeTransitRoutes.get(0).departures.add(event.getTime());
			}
				}
		}
		
	
}


	
	
	List <CompleteTransitRoute> getCompleteTransitRoute(){
		return completeTransitRoutes;
	}
}
