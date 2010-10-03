package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.core.basic.v01.IdImpl;

public class ExpTransRouteUtils {
	private final TransitSchedule trSchedule;
	private final ExperimentalTransitRoute expTrRoute;  
	private final Network network;
	private final TransitRoute transitRoute;
	private static final Logger log = Logger.getLogger(ExpTransRouteUtils.class);
	
	public ExpTransRouteUtils (Network network, TransitSchedule transitSchedule, ExperimentalTransitRoute experimentalTransitRoute){
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

		return transitRoute.getStops().subList(accessStopIndex, egressStopIndex + 1); //"sublist" excludes the last index, so itis necesse		
	}

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
	
	
	public static void main(String[] args) {
		String configFile; 
		
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		}
		
		String strTrLine = "B-296";
		String strTrRoute = "B-296.101.901.H";
		String strAccessFacility = "1605170.1";  // "1600045.2"; //"1625150.1";
		String strEgressFacility = "1605370.3";  // "1600045.1"; //"1610024.1";
			
		ScenarioImpl scenario = new DataLoader ().loadScenarioWithTrSchedule(configFile);
		TransitSchedule trSchedule = scenario.getTransitSchedule();
		TransitLine line = trSchedule.getTransitLines().get(new IdImpl(strTrLine));
		TransitRoute route = line.getRoutes().get(new IdImpl(strTrRoute));
		TransitStopFacility accessFacility = trSchedule.getFacilities().get(new IdImpl(strAccessFacility));
		TransitStopFacility egressFacility = trSchedule.getFacilities().get(new IdImpl(strEgressFacility));
		ExperimentalTransitRoute expTrRoute = new ExperimentalTransitRoute(accessFacility, line, route, egressFacility);
		
		ExpTransRouteUtils ptRouteUtill = new ExpTransRouteUtils(scenario.getNetwork(), trSchedule, expTrRoute);
		
		System.out.println("Access stop: " + ptRouteUtill.getAccessStop().getStopFacility().getId());
		System.out.println("Egress stop: " + ptRouteUtill.getEgressStop().getStopFacility().getId());
		
		//get Stops
		List<TransitRouteStop> stopList = ptRouteUtill.getStops();
		for (TransitRouteStop transitRouteStop : stopList){
			System.out.println(transitRouteStop.getStopFacility().getId());
		}
		
		//get links
		List<Link> linkList = ptRouteUtill.getLinks();
		for (Link link : linkList){
			System.out.println(link.getId());
		}
		
		
	}

}
