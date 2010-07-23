package playground.mmoyo.utils;

import java.util.Iterator;
import java.util.Map.Entry;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.run.OTFVis;

/**created a visual track of the transit route*/
public class TransitRouteVisualizer {
	
	public TransitRouteVisualizer(String config,String strTrRouteId){
		
		//find transitRoute
		ScenarioImpl scenario = new TransScenarioLoader ().loadScenario(config);
		TransitRoute transitRoute = null;
		Iterator<Entry<Id, TransitLine>> iter = scenario.getTransitSchedule().getTransitLines().entrySet().iterator();
		while(transitRoute==null && iter.hasNext()){
			TransitLine transitLine= iter.next().getValue();
			Id transitRouteId = new IdImpl(strTrRouteId);
			transitRoute = transitLine.getRoutes().get(transitRouteId);
		}
		if (transitRoute==null){
			throw new java.lang.NullPointerException("transit route does not exist: " + strTrRouteId);
		}
		///////////////////////////////////////
		
		//create net
		ScenarioImpl newScenario = new ScenarioImpl();
		NetworkLayer newNetwork = newScenario.getNetwork();

		
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
		String config = null;
		String strTrRouteId = null;

		if (args.length==1){
			config = args[0];
		}else{
			config= "../playgrounds/mmoyo/output/trRoutVis/config.xml";
			strTrRouteId = "B-296.101.901.H";
		}
		new TransitRouteVisualizer(config, strTrRouteId);
	}

}
