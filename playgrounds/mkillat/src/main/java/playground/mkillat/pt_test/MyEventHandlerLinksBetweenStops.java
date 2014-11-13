package playground.mkillat.pt_test;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;

public class MyEventHandlerLinksBetweenStops implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, LinkEnterEventHandler {
	
	private List <CompleteTransitRoute> completeTransitRoutes = new ArrayList <CompleteTransitRoute>();
	private Id stopIdA;
	private Id stopIdB;
	private Id lineId;
	private Id routeId;
	
	private double timeA=123456789.0;
	private double timeB=123456789.0;
	List <Id> linkIds = new ArrayList <Id>();
	
	public MyEventHandlerLinksBetweenStops ( Id stopIdA, Id stopIdB, Id lineId, Id routeId){
		this.stopIdA=stopIdA;
		this.stopIdB=stopIdB;
		this.lineId=lineId;
		this.routeId=routeId;
	
		
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (event.getTransitLineId().equals(lineId) && event.getTransitRouteId().equals(routeId)){

			List <Double> arrives= new ArrayList <Double> ();
			List <Double> departures = new ArrayList <Double>();
			
			CompleteTransitRoute aa = new CompleteTransitRoute(event.getDriverId(), event.getVehicleId(), event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId(), event.getTime(), arrives, departures);
			completeTransitRoutes.add(aa);
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {		
		if(completeTransitRoutes.size()!=0 && event.getFacilityId().equals(stopIdA) && event.getVehicleId().equals(completeTransitRoutes.get(0).vehicleId)){
			if(timeA==123456789.0){
				timeA=event.getTime();

			}
			
		}else{
			if(completeTransitRoutes.size()!=0 && event.getFacilityId().equals(stopIdB) && event.getVehicleId().equals(completeTransitRoutes.get(0).vehicleId)){
				if(timeB==123456789.0){
					timeB=event.getTime();
				}}
		}
		
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(completeTransitRoutes.size()!=0 && event.getVehicleId().equals(completeTransitRoutes.get(0).vehicleId) && event.getTime()>=timeA && event.getTime()<=timeB ){
			linkIds.add(event.getLinkId());
		}

		
	}

	List <Id> getLinks(){
		return linkIds;
	}

}
