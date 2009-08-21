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

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.lanes.basic.BasicLane;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.lanes.basic.BasicLaneDefinitionsImpl;
import org.matsim.lanes.basic.BasicLaneImpl;
import org.matsim.lanes.basic.BasicLanesToLinkAssignment;


/**
 * @author dgrether
 *
 */
public class LanesGenerator {
	
	private static final Logger log = Logger.getLogger(LanesGenerator.class);
	
	/**
	 * 
	 * @param spurSpurMapping knotennummer -> (vonspur 1->n nachspur)	
	 * @param knotenSpurLinkMapping knotennummer -> (spurnummer -> linkid)
	 * @return
	 */
	public BasicLaneDefinitions processLaneDefinitions(Map<Integer, Map<Integer, List<Integer>>> spurSpurMapping, 
			Map<Integer, Map<Integer, String>> knotenSpurLinkMapping) {
		//create the lanes ...
		BasicLaneDefinitions laneDefs = new BasicLaneDefinitionsImpl();
		for (Integer nodeId : spurSpurMapping.keySet()) {
			//for all 
			Map<Integer,  List<Integer>> vonSpurToSpurMap = spurSpurMapping.get(nodeId);
			
			for (Integer fromLaneId : vonSpurToSpurMap.keySet()) {
				//get and check the linkId
				String linkIdString = knotenSpurLinkMapping.get(nodeId).get(fromLaneId);
				if (!linkIdString.matches("[\\d]+")) {
					log.error("cannot create link id from string " + linkIdString + " for nodeId: " + nodeId + " and laneId " + fromLaneId);
					continue;
				}
				Id linkId = new IdImpl(linkIdString);
				//create the assignment
				BasicLanesToLinkAssignment assignment = laneDefs.getLanesToLinkAssignments().get(linkId);
				if (assignment == null){
					assignment = laneDefs.getBuilder().createLanesToLinkAssignment(linkId);
					laneDefs.addLanesToLinkAssignment(assignment);
				}
				//and the lane
				BasicLane lane = createLaneWithDefaults(fromLaneId);
				
				//add the toLinks
				List<Integer> toLanes = vonSpurToSpurMap.get(fromLaneId);
				for (Integer toLaneId : toLanes) {
					String toLinkIdString = knotenSpurLinkMapping.get(nodeId).get(toLaneId);
					if (!toLinkIdString.matches("[\\d]+")){
						log.error("cannot create toLink id from string " + toLinkIdString + " for nodeId: " + nodeId + " and toLaneId " + toLaneId);
						continue;
					}
					Id toLinkId = new IdImpl(toLinkIdString);
					lane.addToLinkId(toLinkId);
				}
				if((lane.getToLinkIds() != null) && !lane.getToLinkIds().isEmpty()){
					assignment.addLane(lane);
				}
			} //end for vonSpurToSpurMap			
		}
		return laneDefs;
	}
	
	private BasicLane createLaneWithDefaults(Integer fromLaneId) {
		Id laneId = new IdImpl(fromLaneId);
		BasicLane lane = new BasicLaneImpl(laneId);
		lane.setLength(45.0);
		lane.setNumberOfRepresentedLanes(1);
		return lane;
	}

}
