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
package playground.andreas.intersection.zuerich.lanes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v20.LaneDefinitionsReader;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;

/**
 * @author dgrether copied from playground of dgrether
 *
 */
public class LanesConsistencyChecker implements ConsistencyChecker{
  
	private static final Logger log = Logger.getLogger(LanesConsistencyChecker.class);
	private Network network;
	private LaneDefinitions20 lanes;
	private boolean removeMalformed = false;
	
	public LanesConsistencyChecker(Network net, LaneDefinitions20 laneDefs) {
		this.network = net;
		this.lanes = laneDefs;
	}
	
	@Override
	public void checkConsistency() {
		log.info("checking consistency...");
		List<Id> malformedLinkIds = new ArrayList<Id>();
		for (LanesToLinkAssignment20 l2l : this.lanes.getLanesToLinkAssignments().values()){
			//check if link exists for each assignment of one or more lanes to a link
			if (this.network.getLinks().get(l2l.getLinkId()) == null) {
				log.error("No link found for lanesToLinkAssignment on link Id(linkIdRef): "  + l2l.getLinkId());
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
			
			//check toLinks or toLanes specified in the lanes 
			for (Lane lane : l2l.getLanes().values()) {
				if (lane.getToLaneIds() != null) {
					for (Id toLaneId : lane.getToLaneIds()){
						if (! l2l.getLanes().containsKey(toLaneId)){
							log.error("Error: toLane not existing:");
							log.error("  Lane Id: " + lane.getId() + " on Link Id: " + l2l.getLinkId() + 
									" leads to Lane Id: " + toLaneId + " that is not existing!");
						}
					}
				}
				//check availability of toLink in network
				else if (lane.getToLinkIds() != null){
					for (Id toLinkId : lane.getToLinkIds()) {
						if (this.network.getLinks().get(toLinkId) == null){
							log.error("No link found in network for toLinkId " + toLinkId + " of laneId " + lane.getId() + " of link id " + l2l.getLinkId());
							malformedLinkIds.add(l2l.getLinkId());
						}
					}
				}
			}
			
			//second check matching of link's outlinks and lane's toLinks
			Link link = this.network.getLinks().get(l2l.getLinkId());
			log.info("Link id: " + l2l.getLinkId());
			Map<Id<Link>, ? extends Link> outLinksMap = link.getToNode().getOutLinks();
			Set<Id> linkLanes2LinkIdSet = new HashSet<Id>();
			for (Lane lane : l2l.getLanes().values()){
				if (lane.getToLinkIds() != null){
					linkLanes2LinkIdSet.addAll(lane.getToLinkIds());
				}
			}
			
			for (Link outLink : outLinksMap.values()){
				log.info("\t\thas outlink: " + outLink.getId());
				if (!linkLanes2LinkIdSet.contains(outLink.getId())){
					malformedLinkIds.add(l2l.getLinkId());
					log.error("Error: Lane Outlink: ");
					log.error("\t\tThe lanes of link " + link.getId() + " do not lead to all of the outlinks of the links toNode " + link.getToNode().getId() + " . The outlink " + outLink.getId()
					+ " is not reachable from the lanes of this link. ");
					for (Lane lane : l2l.getLanes().values()){
						log.error("\t\tLane id: " + lane.getId());
						if (lane.getToLinkIds() != null){
							for (Id id : lane.getToLinkIds()){
								log.error("\t\t\t\thas toLinkId: " + id);
							}
						}
						log.error("End: Lane Outlink Error Message");
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
		Network net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFile);
	  log.info("read network");
	  
		LaneDefinitionsReader laneReader = new LaneDefinitionsReader(scenario);
	  laneReader.readFile(lanesFile);
	  
	  LanesConsistencyChecker lcc = new LanesConsistencyChecker(net, scenario.getLanes());
		lcc.setRemoveMalformed(false);
		lcc.checkConsistency();
		
//		MatsimLaneDefinitionsWriter laneWriter = new MatsimLaneDefinitionsWriter(laneDefs);
//		laneWriter.writeFile(lanesFile + ".new.xml");
	}
}
