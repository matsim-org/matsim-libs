package playground.wrashid.nan;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
public class NetworkReadExample {

	public static void main(String[] args) {
		//getFilteredEquilNetLinks();
	}
	public static Map<Id,Link> getNetworkLinks(String networkFile, Coord center, double radius){ //read network
		ScenarioImpl scenanrioImpl = new ScenarioImpl(); // create a new scenario object

		new MatsimNetworkReader(scenanrioImpl).readFile(networkFile);//matsim function, need scenario object for reading
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
