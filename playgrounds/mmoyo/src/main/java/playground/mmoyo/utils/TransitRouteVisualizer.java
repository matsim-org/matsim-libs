package playground.mmoyo.utils;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.run.OTFVis;

/**created a visual track of the transit route*/
public class TransitRouteVisualizer {
	
	public TransitRouteVisualizer(String configfile,String strTrRouteId){
		DataLoader dataLoader = new DataLoader();
		ScenarioImpl scenario = dataLoader.loadScenarioWithTrSchedule(configfile);
		TransitRoute transitRoute = dataLoader.getTransitRoute(strTrRouteId, scenario.getTransitSchedule());
		if (transitRoute==null){
			throw new java.lang.NullPointerException("transit route does not exist: " + strTrRouteId);
		}
		
		//create route net
		NetworkImpl routeNet = new ScenarioImpl().getNetwork();

		//add also start and end links that normally are not include in transitRoute.getRoute().getLinkIds()!! 
		List<Id> linkList = transitRoute.getRoute().getLinkIds();
		linkList.add(0, transitRoute.getRoute().getStartLinkId());
		linkList.add(transitRoute.getRoute().getEndLinkId());
		
		StringBuffer sBuff = null;
		String p1 = "(";
		String p2 = ")";
		String r1 = "--";
		String r2 = "-->(";
		
		for(Id linkId: linkList){		  
			Link link = scenario.getNetwork().getLinks().get(linkId);
			
			if (sBuff==null){
				sBuff = new StringBuffer(p1 + link.getFromNode().getId() + p2);
			}
			sBuff.append(r1 + link.getId() + r2 + link.getToNode().getId() + p2);

			Node fromNode = null;
			if (!routeNet.getNodes().containsKey(link.getFromNode().getId())){
				fromNode = routeNet.createAndAddNode(link.getFromNode().getId(), link.getFromNode().getCoord());	
			}else{
				fromNode = routeNet.getNodes().get(link.getFromNode().getId());
			}

			Node toNode= null;
			if (!routeNet.getNodes().containsKey(link.getToNode().getId())){
				toNode= routeNet.createAndAddNode(link.getToNode().getId(), link.getToNode().getCoord());
			}
			else{
				toNode = routeNet.getNodes().get(link.getToNode().getId());
			}
			
			if (!routeNet.getLinks().containsKey(link.getId())){
				routeNet.createAndAddLink(link.getId(), fromNode, toNode, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
			}else{
				System.out.println("the link already exist! " + link.getId());
			}
		}
	
		System.out.println(sBuff.toString());
		String routeNetFile = scenario.getConfig().controler().getOutputDirectory() + "/Net_" + strTrRouteId + ".xml";
		new NetworkWriter(routeNet).write(routeNetFile );
		OTFVis.playNetwork_Swing(routeNetFile);
	}
	
	public static void main(String[] args) {
		String configFile = null;
		String strTrRouteId = null;

		if (args.length==1){
			configFile = args[0];
		}else{
			configFile= "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			strTrRouteId = "B-M44.101.901.H";
		}
		new TransitRouteVisualizer(configFile, strTrRouteId);
	}

}
