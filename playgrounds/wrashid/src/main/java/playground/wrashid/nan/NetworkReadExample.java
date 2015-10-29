/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.wrashid.nan;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
public class NetworkReadExample {

	public static void main(String[] args) {
		//getFilteredEquilNetLinks();
	}
	public static Map<Id<Link>,? extends Link> getNetworkLinks(String networkFile, Coord center, double radius){ //read network
		MutableScenario scenanrioImpl = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig()); // create a new scenario object

		new MatsimNetworkReader(scenanrioImpl).readFile(networkFile);//matsim function, need scenario object for reading
		// read the network file
		Network network=scenanrioImpl.getNetwork();


		if (center==null){
			return network.getLinks(); //return all links without filtering, do it later
		} else {
			return getLinksWithinRadius(network.getLinks(),radius,center); //set radius of the targeted area
		}

	}
	public static Map<Id<Link>, ? extends Link> getLinksWithinRadius(Map<Id<Link>, ? extends Link> links, double radius, Coord center){
		HashMap<Id<Link>,Link> filteredLinks=new HashMap<Id<Link>, Link>();

		for (Id<Link> linkId:links.keySet()){
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
