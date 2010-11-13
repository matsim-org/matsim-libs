package playground.mmoyo.Validators;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;


/**validates the correct sequence of links in the multimodal net for a given transitSchedule*/
public class TransitLineValidator {

	public TransitLineValidator(ScenarioImpl scenarioImpl) {
		NetworkImpl network = scenarioImpl.getNetwork();
		
		for (TransitLine transitLine : scenarioImpl.getTransitSchedule().getTransitLines().values()){
			for (TransitRoute transitRoute: transitLine.getRoutes().values()){
				
				int stopIndex=0;
				List <TransitRouteStop> stopList = transitRoute.getStops();
				
				TransitRouteStop firstStop = stopList.get(0);
				
				//validates the sequence of links
				Id lastLinkId = null;
				
				System.out.println("\n transitRoute: " + transitRoute.getId());
				int x=0;
				for (TransitRouteStop stop : stopList ){
					System.out.println("stopFactility: " + stop.getStopFacility().getId() + " link:"  +  stop.getStopFacility().getLinkId());
					x++;
				}

				for (Id inkId : transitRoute.getRoute().getLinkIds() ){
					System.out.println("route link Id:" + inkId);
				}
				
				/*
				for (Id linkId: transitRoute.getRoute().getLinkIds()){
					Link link = network.getLinks().get(linkId);					
					if(lastLinkId!=null){
						if (!link.getFromNode().getInLinks().containsKey(lastLinkId)){
							System.out.println("Error : " + transitRoute.getId() );
						}
					}
					
					System.out.println("LinkId : " + linkId + " " + "StopId : " + stopList.get(stopIndex++).getStopFacility().getId());
					//System.out.println("StopId : " + stopList.get(stopIndex++) );
					
					//validates sequence of stops
					TransitRouteStop stop = stopList.get(stopIndex);
					if(stopList.get(stopIndex).getStopFacility().getLinkId().equals(linkId)){
						stopIndex++;
					}
					lastLinkId = linkId;
				}
				//System.out.println(firstStop.getStopFacility().getLinkId().equals(transitRoute.getRoute().getStartLinkId())) ;
				
				//System.out.println("stopIndex: "+ " " + stopIndex + " stopList.size(): " + stopList.size());
				*/
			}
		}
		
		System.out.println("done.");
		
	}
	
	public static void main(String[] args) {
		String configFile;
		if (args.length>0){
			configFile= args[0]; 
		}else{
			configFile= "../playgrounds/mmoyo/output/config.xml"; 
		}
		
		ScenarioImpl scenarioImpl = new playground.mmoyo.utils.DataLoader().loadScenarioWithTrSchedule(configFile);
		new TransitLineValidator(scenarioImpl);
	}
}
