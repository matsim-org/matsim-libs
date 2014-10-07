package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class ExpTransRouteUtils {
	private final TransitSchedule trSchedule;
	private final ExperimentalTransitRoute expTrRoute;  
	private final Network network;
	private final TransitRoute transitRoute;
	private static final Logger log = Logger.getLogger(ExpTransRouteUtils.class);
	
	public ExpTransRouteUtils (final Network network, final TransitSchedule transitSchedule, final ExperimentalTransitRoute experimentalTransitRoute){
		this.network = network;
		this.trSchedule = transitSchedule;
		this.expTrRoute = experimentalTransitRoute;
		this.transitRoute = this.trSchedule.getTransitLines().get(this.expTrRoute.getLineId()).getRoutes().get(this.expTrRoute.getRouteId());		
	}
	
	public double getExpRouteDistance(){
		double distance= 0;
		//for (Link link : this.getLinks()){    //wrong. the first link does not count for route distance because it is the incoming link of departing node
		for(int i=1; i<this.getLinks().size();i++ ){      //in transit schedule, the stop linkRefId is the incoming link. Then, the last link does count for distance
			distance += this.getLinks().get(i).getLength();
		}
		return distance; //since this method include the last transit link in distance calculation, it does not match with org.matsim.core.utils.misc.RouteUtils.calcDistance. 
	}
	
	/*
	 returns the time required to travel along the route according to the transit schedule information.
	  It might not  match the leg travel time because the waiting time can be included in leg travel time
	  from boarding stop departure time to destination stop arrival time
	 */ 
	public double getScheduledTravelTime(){
		double trTime=0;
		List<TransitRouteStop> stops = this.getStops();
		TransitRouteStop prevStop = null;
		
		for (TransitRouteStop stop : stops){
			if (prevStop!=null){
				trTime += stop.getDepartureOffset() - prevStop.getDepartureOffset() ;	
			}
			prevStop = stop;
		}
		TransitRouteStop lastStop= stops.get(stops.size()-1); //Subtract the last offset, because the passenger arriving does not care for next departure time anymore
		trTime -= lastStop.getDepartureOffset() - lastStop.getArrivalOffset(); 
		return trTime;
	}
	
	public TransitRouteStop getAccessStop (){
		return this.transitRoute.getStop(this.trSchedule.getFacilities().get(expTrRoute.getAccessStopId())); 
	} 
	
	public TransitRouteStop getEgressStop (){
		return this.transitRoute.getStop(this.trSchedule.getFacilities().get(expTrRoute.getEgressStopId()));
	} 

	public int getAccessStopIndex(){
		//Warning: it is possible that a stop appears more than once, this index correspond to the first occurrence
		int AccessStopIndex = transitRoute.getStops().indexOf(this.getAccessStop());
		if (AccessStopIndex == -1){
			throw new RuntimeException("first stop of transitRoute does not exit: " + this.expTrRoute.getAccessStopId() ); 
		} 
		return AccessStopIndex;
	}
	
	public int getEgressStopIndex(){
		//Warning: it is possible that a stop appears more than once, this index correspond to the first occurrence
		int EgressStopIndex = transitRoute.getStops().indexOf(this.getEgressStop());
		if (EgressStopIndex == -1){
			throw new RuntimeException("Egress stop of transitRoute does not exit: " + this.expTrRoute.getEgressStopId() ); 
		} 
		return EgressStopIndex;
	}

	public List<TransitRouteStop> getStops(){
		int accessStopIndex = this.getAccessStopIndex();
		int egressStopIndex = this.getEgressStopIndex();
		
		////////////////////////////////////////////////////////
		if (accessStopIndex > egressStopIndex){
			//this may happen in case that the route has a repeated stop (for example when they make a circle)
			//look up again now after the AccessStopIndex
			boolean found = false; 
			for (int i=accessStopIndex+1; i< this.transitRoute.getStops().size(); i++){
				if (this.transitRoute.getStops().get(i).getStopFacility().getId() == this.getEgressStop().getStopFacility().getId()){
					egressStopIndex = i;
					found= true;
					break;
				}
			}
			
			if (!found){
				log.error("Egress stop is located before access stop: " + this.expTrRoute.getRouteDescription());
				return null;
			}
		}
		////////////////////////////////////////////////////////

		return transitRoute.getStops().subList(accessStopIndex, egressStopIndex + 1); //"sublist" excludes the last index, so this is necessary		
	}

	public List<Id> getStopsIds (){
		List<TransitRouteStop> stopList=  this.getStops();
		List<Id> stopIdList=  new ArrayList<Id>();
		for (TransitRouteStop transitRouteStop : stopList){
			stopIdList.add(transitRouteStop.getStopFacility().getId());
		}
		return stopIdList;
	} 
	
	/**includes first and last link of the exp transit route*/  
	public List<Link> getLinks(){
		List<Id> completeIdList = new ArrayList<Id>();
		completeIdList.add(this.transitRoute.getRoute().getStartLinkId()); 
		completeIdList.addAll(1, this.transitRoute.getRoute().getLinkIds());
		completeIdList.add(this.transitRoute.getRoute().getEndLinkId());
		
		int firstLinkIndex = completeIdList.indexOf(this.expTrRoute.getStartLinkId());
		int lastLinkIndex = completeIdList.indexOf(this.expTrRoute.getEndLinkId());
	
		if (firstLinkIndex == -1){
			throw new RuntimeException("first link of transitRoute does not exit: " + this.expTrRoute.getStartLinkId() ); 
		} 
		if (lastLinkIndex == -1){
			System.err.println(this.expTrRoute.getRouteDescription());
			System.err.println(completeIdList.toString());
			throw new RuntimeException("last link of transitRoute does not exit: " + this.expTrRoute.getEndLinkId() );
		}
		
		//<- the description should include also initial and final node
		List<Link> linkList= new ArrayList<Link>();
		for (int linkIndex = firstLinkIndex; linkIndex <= lastLinkIndex; linkIndex++) {
			Id linkId = completeIdList.get(linkIndex);
			linkList.add(this.network.getLinks().get(linkId));
		}
	
		return linkList;
	}
	
	/**
	 * Returns the complete transit route link list including start and end Links
	 */
	public List<Link> getAllLinks (){
		List<Link> allLinkList  = new ArrayList<Link>();
		allLinkList.add(this.network.getLinks().get(this.transitRoute.getRoute().getStartLinkId()));
		for (Id id : this.transitRoute.getRoute().getLinkIds() ){
			allLinkList.add(this.network.getLinks().get(id));
		}
		allLinkList.add(this.network.getLinks().get(this.transitRoute.getRoute().getEndLinkId()));
		return allLinkList;
	}
	
	public static void main(String[] args) {
		String configFile; 
		
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "";
		}
		
		String strTrLine = "B-296";
		String strTrRoute = "B-296.101.901.H";
		String strAccessFacility = "1605170.1";  // "1600045.2"; //"1625150.1";
		String strEgressFacility = "1605370.3";  // "1600045.1"; //"1610024.1";
			
		ScenarioImpl scenario = new DataLoader ().loadScenario(configFile);
		TransitSchedule trSchedule = scenario.getTransitSchedule();
		TransitLine line = trSchedule.getTransitLines().get(Id.create(strTrLine, TransitLine.class));
		TransitRoute route = line.getRoutes().get(Id.create(strTrRoute, TransitRoute.class));
		TransitStopFacility accessFacility = trSchedule.getFacilities().get(Id.create(strAccessFacility, TransitStopFacility.class));
		TransitStopFacility egressFacility = trSchedule.getFacilities().get(Id.create(strEgressFacility, TransitStopFacility.class));
		ExperimentalTransitRoute expTrRoute = new ExperimentalTransitRoute(accessFacility, line, route, egressFacility);
		
		ExpTransRouteUtils ptRouteUtill = new ExpTransRouteUtils(scenario.getNetwork(), trSchedule, expTrRoute);
		
		System.out.println("Access stop: " + ptRouteUtill.getAccessStop().getStopFacility().getId());
		System.out.println("Egress stop: " + ptRouteUtill.getEgressStop().getStopFacility().getId());
		
		System.out.println(" ");
		//get Stops
		List<TransitRouteStop> stopList = ptRouteUtill.getStops();
		for (TransitRouteStop transitRouteStop : stopList){
			System.out.print(transitRouteStop.getStopFacility().getId() + " ");
		}
		
		System.out.println(" ");
		//again
		for(Id id : ptRouteUtill.getStopsIds()){
			System.out.print(id + " ");
		}
		
		
		//get links
		List<Link> linkList = ptRouteUtill.getLinks();
		for (Link link : linkList){
			System.out.print(link.getId()+ " ");
		}
		
		
	}

}
