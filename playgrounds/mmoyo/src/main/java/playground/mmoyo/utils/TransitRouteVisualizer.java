package playground.mmoyo.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.run.OTFVis;
import org.matsim.transitSchedule.api.TransitRoute;

/**created a visual track of the transit route*/
public class TransitRouteVisualizer {
	
	public TransitRouteVisualizer(String configfile,String strTrRouteId){
		ScenarioImpl scenario = new DataLoader ().loadScenarioWithTrSchedule(configfile);
		TransitRoute transitRoute = new DataLoader().getTransitRoute(strTrRouteId,scenario.getTransitSchedule());
		if (transitRoute==null){
			throw new java.lang.NullPointerException("transit route does not exist: " + strTrRouteId);
		}
		
		//create net
		ScenarioImpl newScenario = new ScenarioImpl();
		NetworkImpl newNetwork = newScenario.getNetwork();

		//<- add initial link
		for(Id linkId: transitRoute.getRoute().getLinkIds()){		  
			Link link = scenario.getNetwork().getLinks().get(linkId);
			System.out.println(link.getId());		
			
			Node fromNode = null;
			if (!newNetwork.getNodes().containsKey(link.getFromNode().getId())){
				fromNode = newNetwork.createAndAddNode(link.getFromNode().getId(), link.getFromNode().getCoord());	
			}else{
				fromNode = newNetwork.getNodes().get(link.getFromNode().getId());
			}

			Node toNode= null;
			if (!newNetwork.getNodes().containsKey(link.getToNode().getId())){
				toNode= newNetwork.createAndAddNode(link.getToNode().getId(), link.getToNode().getCoord());
			}
			else{
				toNode = newNetwork.getNodes().get(link.getToNode().getId());
			}
			
			if (!newNetwork.getLinks().containsKey(link.getId())){
				newNetwork.createAndAddLink(link.getId(), fromNode, toNode, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
			}else{
				//newNetwork.getLinks().remove(link.getId());
				//newNetwork.getLinks().remove(newNetwork.getLinks().get(link.getId()));
				//newNetwork.removeLink(link.getId());

				//newNetwork.createAndAddLink(link.getId(), fromNode, toNode, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
				System.out.println("the link already exist! " + link.getId());
			}
		}
		
		//add last link
		
		String newNetFile = scenario.getConfig().controler().getOutputDirectory() + "/Net_" + strTrRouteId + ".xml";
		new NetworkWriter(newNetwork).write(newNetFile );
		new OTFVis().playNetwork(new String[]{newNetFile});
	}
	
	public static void main(String[] args) {
		String configFile = null;
		String strTrRouteId = null;

		if (args.length==1){
			configFile = args[0];
		}else{
			configFile= "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			strTrRouteId = "S-42.002.001.H";
		}
		new TransitRouteVisualizer(configFile, strTrRouteId);
	}

}
