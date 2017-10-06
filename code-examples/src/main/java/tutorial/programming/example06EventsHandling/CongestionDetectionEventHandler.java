package tutorial.programming.example06EventsHandling;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;
/**
 * This EventHandler implementation counts the travel time of
 * all agents and provides the average travel time per
 * agent.
 * Actually, handling Departures and Arrivals should be sufficient for this (may 2014)
 * @author dgrether
 *
 */
public class CongestionDetectionEventHandler implements LinkEnterEventHandler,
	LinkLeaveEventHandler, PersonDepartureEventHandler{
	
	private Map<Id<Vehicle>,Double> earliestLinkExitTime = new HashMap<>() ;
	private Network network;
	
	public CongestionDetectionEventHandler( Network network ) {
		this.network = network ;
	}

	@Override
	public void reset(int iteration) {
		this.earliestLinkExitTime.clear(); 
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Link link = network.getLinks().get( event.getLinkId() ) ;
		double linkTravelTime = link.getFreespeed( event.getTime() ) / link.getLength() ; 
		this.earliestLinkExitTime.put( event.getVehicleId(), event.getTime() + linkTravelTime ) ;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		double excessTravelTime = event.getTime() - this.earliestLinkExitTime.get( event.getVehicleId() ) ; 
		System.out.println( "excess travel time: " + excessTravelTime ) ;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id<Vehicle> vehId = Id.create( event.getPersonId(), Vehicle.class ) ; // unfortunately necessary since vehicle departures are not uniformly registered
		this.earliestLinkExitTime.put( vehId, event.getTime() ) ;
	}
}
