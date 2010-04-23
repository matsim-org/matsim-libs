/* *********************************************************************** *
 * project: org.matsim.*
 * LanesGenerator
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
package playground.andreas.intersection.zuerich;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsImpl;
import org.matsim.lanes.LaneImpl;
import org.matsim.lanes.LanesToLinkAssignment;


/**
 * @author dgrether
 *
 */
public class LanesGenerator {
	
	private static final Logger log = Logger.getLogger(LanesGenerator.class);
	private Network network;
	
	
	private void preprocessKnotenSpurLinkMapping(Map<Integer, Map<Integer, String>> knotenSpurLinkMapping){
		List<Tuple<Integer, Integer>> malformedNodeSpurIds = new ArrayList<Tuple<Integer, Integer>>();
		//detect non valid linkIds
		for (Integer nodeId : knotenSpurLinkMapping.keySet()){
			Map<Integer, String> spur2LinkMap = knotenSpurLinkMapping.get(nodeId);
			for (Integer spurId : spur2LinkMap.keySet()){
				String linkId = spur2LinkMap.get(spurId);
				if (!linkId.matches("[\\d]+")) {
					log.warn("no valid link id of string " + linkId+ " for nodeId: " + nodeId + " and spurId " + spurId + ". entry is removed from maps");
					malformedNodeSpurIds.add( new Tuple<Integer, Integer>(nodeId, spurId));
				}
			}
		}
		//remove from the maps
		for (Tuple<Integer, Integer> t : malformedNodeSpurIds){
			//spur2LinkMap
			Map<Integer, String> spur2LinkMap = knotenSpurLinkMapping.get(t.getFirst());
			spur2LinkMap.remove(t.getSecond());
			if (spur2LinkMap.isEmpty()){
				knotenSpurLinkMapping.remove(t.getFirst());
			}
		}
	}
	
	/**
	 * cleans the knoten -> vonspur -> nachspur mapping after the knoten -> spur -> link mapping has been cleaned
	 */
	private void cleanKnotenVonSpurNachSpurMapping(Map<Integer, Map<Integer, List<Integer>>> knotenVonSpurNachSpurMapping,
			Map<Integer, Map<Integer, String>> knotenSpurLinkMapping) {
		List<Tuple<Integer, Integer>> malformedNodeSpurIds = new ArrayList<Tuple<Integer, Integer>>();
		List<Integer> malformedNodeIds = new ArrayList<Integer>();

		for (Integer knotenId : knotenVonSpurNachSpurMapping.keySet()){
			if (!knotenSpurLinkMapping.containsKey(knotenId)){
				malformedNodeIds.add(knotenId);
			}
			else {
				Map<Integer, List<Integer>> vonSpurNachSpurMap = knotenVonSpurNachSpurMapping.get(knotenId);
				for (Integer vonSpurId : vonSpurNachSpurMap.keySet()){
					//check vonSpur
					if (!knotenSpurLinkMapping.get(knotenId).containsKey(vonSpurId)){
						malformedNodeSpurIds.add(new Tuple<Integer, Integer>(knotenId, vonSpurId));
					}
					else {
						List<Tuple<Integer, Integer>> malformedNachSpur = new ArrayList<Tuple<Integer, Integer>>();
						//detect
						for (Integer nachSpur : vonSpurNachSpurMap.get(vonSpurId)) {
							if (!knotenSpurLinkMapping.get(knotenId).containsKey(nachSpur)){
								malformedNachSpur.add(new Tuple<Integer, Integer>(vonSpurId, nachSpur));
							}
						}
						//remove
						for (Tuple<Integer, Integer> t : malformedNachSpur){
							vonSpurNachSpurMap.get(t.getFirst()).remove(t.getSecond());
						}
					}

				}
			}
		}

		for (Integer id : malformedNodeIds){
			knotenVonSpurNachSpurMapping.remove(id);
		}

		for (Tuple<Integer, Integer> t : malformedNodeSpurIds){
			if (knotenVonSpurNachSpurMapping.containsKey(t.getFirst())){
				knotenVonSpurNachSpurMapping.get(t.getFirst()).remove(t.getSecond());
				if (knotenVonSpurNachSpurMapping.get(t.getFirst()).isEmpty()){
					knotenVonSpurNachSpurMapping.remove(t.getFirst());
				}
			}
		}
	}
	

	/**
	 * Prints ids of  two lanes if they are mapped to the same link
	 * @param knotenVonSpurNachSpurMapping
	 * @param knotenSpurLinkMapping
	 */
	private void mergeLanesOnSameLink(Map<Integer, Map<Integer, List<Integer>>> knotenVonSpurNachSpurMapping,
			Map<Integer, Map<Integer, String>> knotenSpurLinkMapping) {
		
		Map<Integer, Tuple<Integer, Integer>> knotenVonSpurNachSpurToMergeMap = new HashMap<Integer, Tuple<Integer, Integer>>();
		//detect mappings
		for (Integer nodeId : knotenVonSpurNachSpurMapping.keySet()){
			Map<Integer, List<Integer>> vonSpurNachSpurMap = knotenVonSpurNachSpurMapping.get(nodeId);
			for (Integer vonSpurId : vonSpurNachSpurMap.keySet()){
				String vonSpurLinkId = knotenSpurLinkMapping.get(nodeId).get(vonSpurId);
				List<Integer> nachSpurList = vonSpurNachSpurMap.get(vonSpurId);
				for (Integer nachSpurId : nachSpurList){
					String nachSpurLinkId = knotenSpurLinkMapping.get(nodeId).get(nachSpurId);
					if (vonSpurLinkId.equalsIgnoreCase(nachSpurLinkId)){		
						log.info("Found vonSpurNachSpur mapping on same link id " + vonSpurLinkId + " at node id " 
								+ nodeId + " vonSpur id " + vonSpurId + " nachSpur id " + nachSpurId);
						knotenVonSpurNachSpurToMergeMap.put(nodeId, new Tuple<Integer, Integer>(vonSpurId, nachSpurId));
					}
				}
			}
		}
	}
	
	
	/**
	 * Calls several preprocessing procedures, the order of the calls is important!
	 * @param knotenVonSpurNachSpurMapping
	 * @param knotenSpurLinkMapping
	 */
	private void preprocessData(Map<Integer, Map<Integer, List<Integer>>> knotenVonSpurNachSpurMapping,
			Map<Integer, Map<Integer, String>> knotenSpurLinkMapping) {
		preprocessKnotenSpurLinkMapping(knotenSpurLinkMapping);
		cleanKnotenVonSpurNachSpurMapping(knotenVonSpurNachSpurMapping, knotenSpurLinkMapping);
		mergeLanesOnSameLink(knotenVonSpurNachSpurMapping, knotenSpurLinkMapping);
	}



	/**
	 * Preprocess and create lanes for zh mappings
	 * @param spurSpurMapping knotennummer -> (vonspur 1->n nachspur)	
	 * @param knotenSpurLinkMapping knotennummer -> (spurnummer -> linkid)
	 * @return
	 */
	public LaneDefinitions processLaneDefinitions(Map<Integer, Map<Integer, List<Integer>>> spurSpurMapping, 
			Map<Integer, Map<Integer, String>> knotenSpurLinkMapping) {
		
		preprocessData(spurSpurMapping, knotenSpurLinkMapping);
		
		//create the lanes ...
		LaneDefinitions laneDefs = new LaneDefinitionsImpl();
		for (Integer nodeId : spurSpurMapping.keySet()) {
			System.out.println();
			log.info("##########################################################");
			log.info("processing node id " + nodeId);
			log.info("##########################################################");
			
			//for all 
			Map<Integer,  List<Integer>> vonSpurToSpurMap = spurSpurMapping.get(nodeId);
			
			for (Integer fromLaneId : vonSpurToSpurMap.keySet()) {
				//get and check the linkId
				System.out.println();
				log.info("processing link id for lane id " + fromLaneId);
				String linkIdString = knotenSpurLinkMapping.get(nodeId).get(fromLaneId);
				if (!linkIdString.matches("[\\d]+")) {
					log.error("cannot create link id from string " + linkIdString + " for nodeId: " + nodeId + " and laneId " + fromLaneId);
					continue;
				}
				
				Id linkId = new IdImpl(linkIdString);
				//create the assignment
				LanesToLinkAssignment assignment = laneDefs.getLanesToLinkAssignments().get(linkId);
				if (assignment == null){
					assignment = laneDefs.getFactory().createLanesToLinkAssignment(linkId);
					laneDefs.addLanesToLinkAssignment(assignment);
				}
				//and the lane
				Lane lane = createLaneWithDefaults(fromLaneId);
				
				//reset the length if there is a network available
				log.info("checking the length of the lane..." );
				if (this.network != null){
					Link link = this.network.getLinks().get(linkId);
					double linkLaneFraction = 0.5 * link.getLength();
					if (lane.getStartsAtMeterFromLinkEnd() > linkLaneFraction){
						lane.setStartsAtMeterFromLinkEnd(linkLaneFraction);
						log.info("Reset lane length of lane Id : " + lane.getId() + " of Link Id " + linkId + " to " + linkLaneFraction);
					}
				}
				
				//add the toLinks
				log.info("adding toLinks ...");
				if (vonSpurToSpurMap.containsKey(fromLaneId)){
					List<Integer> toLanes = vonSpurToSpurMap.get(fromLaneId);
					for (Integer toLaneId : toLanes) {
						String toLinkIdString = knotenSpurLinkMapping.get(nodeId).get(toLaneId);
						if (!toLinkIdString.matches("[\\d]+")){
							log.error("cannot create toLink id from string " + toLinkIdString + " for nodeId: " + nodeId + " and toLaneId " + toLaneId);
							continue;
						}
						//ignore lane if toLane is on the same link
						if (toLinkIdString.equalsIgnoreCase(linkIdString)){
							log.error("Found vonSpurNachSpur mapping on same link id " + linkIdString + " at node id " 
									+ nodeId + " vonSpur id " + fromLaneId + " nachSpur id " + toLaneId + " . Lane will be ignored");
							continue;
						}
						
						Id toLinkId = new IdImpl(toLinkIdString);
						if (this.network != null){
							Link link = this.network.getLinks().get(linkId);
							if (!link.getToNode().getOutLinks().containsKey(toLinkId)){
								log.error("for lane id " + lane.getId() + " at link id " + linkId + " the toLane id " + toLaneId +  " at toLink id " + toLinkId + " is determined, but this is not an outlink of the link's tonode.");
							}
						}
						lane.addToLinkId(toLinkId);
					}
				}
				else {
				  log.warn("no valid tolanes...");
				}
				
				//add or remove the assignment
				if((lane.getToLinkIds() != null) && !lane.getToLinkIds().isEmpty()){
					assignment.addLane(lane);
					log.info("successfully added lane to lanes to link assignment!");
				}
				else if (assignment.getLanes().isEmpty()){
					log.info("removed lanes to link assignment cause it has no lanes yet!");
					laneDefs.getLanesToLinkAssignments().remove(assignment.getLinkId());
				}
			} //end for vonSpurToSpurMap			
		}
		return laneDefs;
	}
	


	private Lane createLaneWithDefaults(Integer fromLaneId) {
		Id laneId = new IdImpl(fromLaneId);
		Lane lane = new LaneImpl(laneId);
		lane.setStartsAtMeterFromLinkEnd(45.0);
		lane.setNumberOfRepresentedLanes(1);
		return lane;
	}

	public void setNetwork(Network net) {
		this.network = net;
	}

}
