package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class PtRouteUtill {
	private final TransitSchedule trSchedule;
	private final ExperimentalTransitRoute expTrRoute;  
	private final Network network;
	private final TransitRoute transitRoute;
	private static final Logger log = Logger.getLogger(PtRouteUtill.class);
	
	public PtRouteUtill (Network network, TransitSchedule transitSchedule, ExperimentalTransitRoute experimentalTransitRoute){
		this.network = network;
		this.trSchedule = transitSchedule;
		this.expTrRoute = experimentalTransitRoute;
		this.transitRoute = this.trSchedule.getTransitLines().get(this.expTrRoute.getLineId()).getRoutes().get(this.expTrRoute.getRouteId());		
	}
	
	public double getExpRouteDistance(){
		double distance= 0;
		for (Link link : this.getLinks()){
			distance += link.getLength();
		}
		return distance;  	//<-compare result with org.matsim.core.utils.misc.RouteUtils.calcDistance
	}
	
	public TransitRouteStop getAccessStop (){
		return this.transitRoute.getStop(this.trSchedule.getFacilities().get(expTrRoute.getAccessStopId())); 
	} 
	
	public TransitRouteStop getEgressStop (){
		return this.transitRoute.getStop(this.trSchedule.getFacilities().get(expTrRoute.getEgressStopId()));
	} 

	public int getAccessStopIndex(){
		int AccessStopIndex = transitRoute.getStops().indexOf(this.getAccessStop());
		if (AccessStopIndex == -1){
			throw new RuntimeException("first stop of transitRoute does not exit: " + this.expTrRoute.getAccessStopId() ); 
		} 
		return AccessStopIndex;
	}
	
	public int getEgressStopIndex(){
		int EgressStopIndex = transitRoute.getStops().indexOf(this.getEgressStop());
		if (EgressStopIndex == -1){
			throw new RuntimeException("Egress stop of transitRoute does not exit: " + this.expTrRoute.getEgressStopId() ); 
		} 
		return EgressStopIndex;
	}
	
	public List<TransitRouteStop> getStops(){
		if (this.getAccessStopIndex() > this.getEgressStopIndex()){
			//throw new RuntimeException("Egress stop is located before access stop: " + this.expTrRoute.getRouteDescription()); 
			log.error("Egress stop is located before access stop: " + this.expTrRoute.getRouteDescription());
			return null;
		} 
		return transitRoute.getStops().subList(this.getAccessStopIndex(), this.getEgressStopIndex());		
		/*
		List<TransitRouteStop> stopList= new ArrayList<TransitRouteStop>();
		for (int stopIndex = firstStopIndex; stopIndex <= lastStopIndex; stopIndex++) {
			stopList.add(this.transitRoute.getStops().get(stopIndex));
		}
		return stopList;
		*/
	}

	public List<Link> getLinks(){
		int firstLinkIndex = this.transitRoute.getRoute().getLinkIds().indexOf(this.expTrRoute.getStartLinkId());
		int lastLinkIndex = this.transitRoute.getRoute().getLinkIds().indexOf(this.expTrRoute.getEndLinkId());
		
		if (firstLinkIndex == -1){
			throw new RuntimeException("first link of transitRoute does not exit: " + this.expTrRoute.getStartLinkId() ); 
		} 
		if (lastLinkIndex == -1){
			throw new RuntimeException("last link of transitRoute does not exit: " + this.expTrRoute.getEndLinkId() );
		}

		//<- the description should include also initial and final node
		List<Link> linkList= new ArrayList<Link>();
		for (int linkIndex = firstLinkIndex; linkIndex <= lastLinkIndex; linkIndex++) {
			for (Id linkId : this.transitRoute.getRoute().getLinkIds()){
				linkList.add(this.network.getLinks().get(linkId));
			}
		}
	
		return linkList;
	}
		
}
