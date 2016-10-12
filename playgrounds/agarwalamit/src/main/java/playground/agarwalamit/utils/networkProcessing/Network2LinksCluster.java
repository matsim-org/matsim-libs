/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.utils.networkProcessing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class Network2LinksCluster {

	public Network2LinksCluster(String networkFile) {
		this.networkFile = networkFile;
	}

	private final String networkFile;
	private final Map<String, Set<Id<Link>>> linkCluster = new HashMap<>();


	public static void main(String[] args) {
		String networkFile = "/Users/amit/Documents/repos/runs-svn/siouxFalls/input/SiouxFalls_networkWithRoadType.xml.gz";
		Network2LinksCluster lg = new Network2LinksCluster(networkFile);
		lg.run();
	}

	private void run(){
		Network network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();
		
		for (Link l :network.getLinks().values()){
			
			String linkAttributes = this.getLinkInfoAsString(l);
			
			if(linkCluster.containsKey(linkAttributes)){
				Set<Id<Link>> linkIds =  linkCluster.get(linkAttributes);
				linkIds.add(l.getId());
				linkCluster.put(linkAttributes, linkIds);
			} else {
				Set<Id<Link>> linkIds = new HashSet<>();
				linkIds.add(l.getId());
				linkCluster.put(linkAttributes, linkIds);
			}
		}
		System.out.println("Number of links are "+ network.getLinks().size());
		System.out.println("Number of clusters are "+ linkCluster.size());
		
		int sum =0;
		for(String str : linkCluster.keySet()){
			sum += linkCluster.get(str).size();
		}
		
		System.out.println("Number of links (from cluster map) are "+ sum);
	}

	private String getLinkInfoAsString(Link l){
		return Math.rint(10*Math.floor(l.getCapacity()/10))+"\t"+Math.rint(l.getFreespeed())+"\t"+Math.rint(5*Math.floor(l.getLength()/5))+"\t"+Math.rint(l.getNumberOfLanes());
	}
	
	public Map<String, Set<Id<Link>>> getLinkCluster(){
		return this.linkCluster;
	}
}
