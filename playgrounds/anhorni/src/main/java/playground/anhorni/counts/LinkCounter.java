/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.counts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

public class LinkCounter {
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Counts counts;
	private double radius;
	private String nodeId;
	
	private final static Logger log = Logger.getLogger(LinkCounter.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LinkCounter counter = new LinkCounter();
		if (args.length == 2 ) {
			counter.init(args[0], args[1], -1.0, "");
			counter.count();
			
		}
		else if (args.length == 4 ) {
			counter.init(args[0], args[1], Double.parseDouble(args[2]), args[3]);
			counter.count();
		}
		else {
			log.info("please provide the correct arguments!");
		}
		
	}
	
	
	public void init(String networkFile, String countsFile, double radius, String nodeId) {
		this.radius = radius;
		this.nodeId = nodeId;
		
		log.info("read netork ...");
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
		log.info("read counts ..."); 
		this.counts = new Counts();
		MatsimCountsReader countsReader = new MatsimCountsReader(counts);
		countsReader.readFile(countsFile);
	}
	
	public void count() {
		int astraCounter = 0;
		int otherCounter = 0;
		int counter = 0;
		int nextMsg = 1;
		
		for (Id<Link> countId : this.counts.getCounts().keySet()) {
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" count # " + counter);
			}
			Count count = this.counts.getCounts().get(countId);
			if (checkInside(count)) {
				if (count.getCsId().startsWith("ASTRA")) astraCounter++;
				else otherCounter++;
			}			
		}
		log.info("ASTRA links: " + astraCounter);
		log.info("other links: " + otherCounter);
	}
	
	private boolean checkInside(Count count) {
		if (this.radius < 0.0) return true;
		else {	
			Node centerNode = this.scenario.getNetwork().getNodes().get(Id.create(this.nodeId, Node.class));
			Link link = this.scenario.getNetwork().getLinks().get(count.getLocId());
			
			if (link == null) {
				log.info("Link not found " + count.getLocId().toString() + " station " + count.getCsId().toString());
				return false;
			}
			Coord coordLink = link.getCoord();

			if (CoordUtils.calcDistance(centerNode.getCoord(), coordLink) < this.radius) return true;
			else return false;
		}
	}	
}
