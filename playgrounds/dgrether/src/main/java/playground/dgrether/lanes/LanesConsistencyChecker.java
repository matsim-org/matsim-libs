/* *********************************************************************** *
 * project: org.matsim.*
 * LanesConsistencyChecker
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.lanes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsImpl;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.MatsimLaneDefinitionsReader;

import playground.dgrether.consistency.ConsistencyChecker;


/**
 * @author dgrether
 *
 */
public class LanesConsistencyChecker implements ConsistencyChecker{
  
	private static final Logger log = Logger.getLogger(LanesConsistencyChecker.class);
	private Network network;
	private LaneDefinitions lanes;
	private boolean removeMalformed = false;
	
	public LanesConsistencyChecker(Network net, LaneDefinitions laneDefs) {
		this.network = net;
		this.lanes = laneDefs;
	}
	
	public void checkConsistency() {
		log.info("checking consistency...");
		List<Id> malformedLinkIds = new ArrayList<Id>();
		for (LanesToLinkAssignment l2l : this.lanes.getLanesToLinkAssignments().values()){
			//check link
			if (this.network.getLinks().get(l2l.getLinkId()) == null) {
				log.error("No link found for lanesToLinkAssignment with id "  + l2l.getLinkId());
				malformedLinkIds.add(l2l.getLinkId());
			}
			//check length
			else {
				Link link = this.network.getLinks().get(l2l.getLinkId());
				for (Lane l : l2l.getLanes().values()){
					if (link.getLength() < l.getStartsAtMeterFromLinkEnd()) {
						log.error("Link Id " + link.getId() + " is shorter than an assigned lane with id " + l.getId());
						malformedLinkIds.add(l2l.getLinkId());
					}
				}
			}
			
			//check toLinks
			//first check matching of Lane definitions -> Link
			for (Lane lane : l2l.getLanes().values()) {
				//check availability of toLink in network
				Map<Id, Lane> toLinkIdToLaneMap = new HashMap<Id, Lane>();
				if (lane.getToLinkIds() != null){
					for (Id toLinkId : lane.getToLinkIds()) {
						if (this.network.getLinks().get(toLinkId) == null){
							log.error("No link found in network for toLinkId " + toLinkId + " of laneId " + lane.getId() + " of link id " + l2l.getLinkId());
							malformedLinkIds.add(l2l.getLinkId());
						}
						//check if multiple lanes have the same toLink
						if (toLinkIdToLaneMap.containsKey(toLinkId)){
							//TODO improve error message
							log.error("On link Id " + l2l.getLinkId() + " exists more than one lane leading to Link Id " + toLinkId);
						}
						else {
							toLinkIdToLaneMap.put(toLinkId, lane);
						}
					}
				}
			}
			
			//second check matching of link's outlinks and lane's toLinks
			Link link = this.network.getLinks().get(l2l.getLinkId());
			log.info("Link id: " + l2l.getLinkId());
			Map<Id, ? extends Link> outLinksMap = link.getToNode().getOutLinks();
			Set<Id> linkLanes2LinkIds = new HashSet<Id>();
			for (Lane lane : l2l.getLanes().values()){
				if (lane.getToLinkIds() != null){
					linkLanes2LinkIds.addAll(lane.getToLinkIds());
				}
			}
			
//			if (outLinksMap.size() != linkLanes2LinkIds.size()){
//				log.error("Link " + l2l.getLinkId() + ": The number of toLinks is different in LanesToLinkAssignment (" + linkLanes2LinkIds.size()+ ") and " +
//						" in Link definition (" + outLinksMap.size() + ")");
//				log.error("  Link id: " + link.getId());
//				for (Link outLink : outLinksMap.values()){
//					log.error("    has outlink: " + outLink.getId());
//				}
//				log.error("");
//				for (Lane lane : l2l.getLanes().values()){
//					log.error("  Lane id: " + lane.getId());
//					if (lane.getToLinkIds() != null){
//						for (Id id : lane.getToLinkIds()){
//							log.error("    has toLinkId: " + id);
//						}
//					}
//				}
//			}
			
			for (Link outLink : outLinksMap.values()){
				log.info("    has outlink: " + outLink.getId());
				if (!linkLanes2LinkIds.contains(outLink.getId())){
					malformedLinkIds.add(l2l.getLinkId());
					log.error("The lanes of link " + link.getId() + " do not lead to all of the outlinks of the links toNode " + link.getToNode().getId() + " . The outlink " + outLink.getId()
					+ " is not reachable from the lanes of this link. ");
					log.error("");
					for (Lane lane : l2l.getLanes().values()){
						log.error("  Lane id: " + lane.getId());
						if (lane.getToLinkIds() != null){
							for (Id id : lane.getToLinkIds()){
								log.error("    has toLinkId: " + id);
							}
						}
					}
				}
			}
		}
		
		
		
		if (this.removeMalformed){
			for (Id id : malformedLinkIds) {
				this.lanes.getLanesToLinkAssignments().remove(id);
			}
		}
		log.info("checked consistency.");
	}
	
	public boolean isRemoveMalformed() {
		return removeMalformed;
	}

	
	public void setRemoveMalformed(boolean removeMalformed) {
		this.removeMalformed = removeMalformed;
	}

	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		String netFile = DgPaths.IVTCHBASE + "baseCase/network/ivtch-osm.xml";
//		String lanesFile = DgPaths.STUDIESDG + "signalSystemsZh/laneDefinitions.xml";
		String netFile = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/Cottbus-BA/network_wo_junctions.xml";
		String lanesFile = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/Cottbus-BA/lanes_cottbus_v20_jbol_c_wo_junctions.xml";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFile);
	  log.info("read network");
	  
	  
	  LaneDefinitions laneDefs = new LaneDefinitionsImpl();
		MatsimLaneDefinitionsReader laneReader = new MatsimLaneDefinitionsReader(laneDefs );
	  laneReader.readFile(lanesFile);
	  
	  LanesConsistencyChecker lcc = new LanesConsistencyChecker(net, laneDefs);
		lcc.setRemoveMalformed(false);
		lcc.checkConsistency();
		
//		MatsimLaneDefinitionsWriter laneWriter = new MatsimLaneDefinitionsWriter(laneDefs);
//		laneWriter.writeFile(lanesFile + ".new.xml");
	}
}
