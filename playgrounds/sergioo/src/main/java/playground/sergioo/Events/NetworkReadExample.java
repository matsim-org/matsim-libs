package playground.sergioo.Events;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.ConfigUtils;

public class NetworkReadExample {

	public static void main(String[] args) {
		//getFilteredEquilNetLinks();
	}
	public static Map<Id,Link> getNetworkLinks(String networkFile, Coord center, double radius){ //read network
		ScenarioImpl scenanrioImpl = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()); // create a new scenario object     
		
		NetworkReaderMatsimV1 networkReaderMatsimV1 = new NetworkReaderMatsimV1(scenanrioImpl);//matsim function, need scenario object for reading
		
		
		networkReaderMatsimV1.parse(networkFile);
		
		// read the network file
		NetworkImpl network=scenanrioImpl.getNetwork();
		
		
		if (center==null){
			return network.getLinks(); //return all links without filtering, do it later
		} else {
			return getLinksWithinRadius(network.getLinks(),radius,center); //set radius of the targeted area
		}
		
	}
	public static Map<Id, Link> getLinksWithinRadius(Map<Id, Link> links, double radius, Coord center){
		HashMap<Id,Link> filteredLinks=new HashMap<Id, Link>();
			
		for (Id linkId:links.keySet()){
			Link link=links.get(linkId); //find all the links
						
			if (getDistance(link.getCoord(),center)<radius){ // filter links
				filteredLinks.put(linkId, link);
			}
		}
		return filteredLinks;
		
	}
	public static double getDistance(Coord coordA, Coord coordB){
		return Math.sqrt(((coordA.getX()-coordB.getX())*(coordA.getX()-coordB.getX()) + (coordA.getY()-coordB.getY())*(coordA.getY()-coordB.getY())));
	} //filter algorithm
	

}
