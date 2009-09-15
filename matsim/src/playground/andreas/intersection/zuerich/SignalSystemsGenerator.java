/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsGenerator
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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.signalsystems.basic.BasicSignalGroupDefinition;
import org.matsim.signalsystems.basic.BasicSignalSystems;


/**
 * @author dgrether
 *
 */
public class SignalSystemsGenerator {
	
	private static final Logger log = Logger.getLogger(SignalSystemsGenerator.class);

	private Network network;
	private BasicLaneDefinitions laneDefinitions;
	private BasicSignalSystems signalSystems;

	public SignalSystemsGenerator(Network net, BasicLaneDefinitions laneDefs, BasicSignalSystems signalSystems) {
		this.network = net;
		this.laneDefinitions = laneDefs;
		this.signalSystems = signalSystems;
	}
	
	private void preprocessKnotenLsaSpurMap(Map<Integer, Map<Integer, List<Integer>>> knotenLsaSpurMap, 
			Map<Integer, Map<Integer, String>> knotenSpurLinkMap) {
		//first check for all nodes of knotenLSASpurMap if there is a corresponding node in knotenSpurLinkMap
		List<Integer> malformedNodeIds = new ArrayList<Integer>();
		for (Integer knotenId : knotenLsaSpurMap.keySet()){
			if (!knotenSpurLinkMap.containsKey(knotenId)){
				malformedNodeIds.add(knotenId);
			}
			else {
				Map<Integer, List<Integer>> lsaSpurMap = knotenLsaSpurMap.get(knotenId);
				for (Integer lsaId : lsaSpurMap.keySet()){
					List<Integer> spurList = lsaSpurMap.get(lsaId);
					for (Integer spurId : spurList) {
						if (!knotenSpurLinkMap.get(knotenId).containsKey(spurId)){
							log.error("No spur -> link mapping found for knoten id " + knotenId + " lsa id " + lsaId + " and spur id " + spurId);
							//nothing to do because data is valid
						}
					}
				}
			}
		}
		for (Integer knotenId : malformedNodeIds){
			log.warn("removed knoten id " + knotenId + " because no knoten -> spur -> link mapping can be found");
			knotenLsaSpurMap.remove(knotenId);
		}
	}

	private void preprocessSystemDefinitions(Map<Integer, Map<Integer, List<Integer>>> knotenLsaSpurMap) {
		List<Integer> malformedNodeIds = new ArrayList<Integer>();
		
		for (Integer knotenId : knotenLsaSpurMap.keySet()){
			Map<Integer, List<Integer>> lsaSpurMap = knotenLsaSpurMap.get(knotenId);
			for (Integer lsaIdInt : lsaSpurMap.keySet()){
				Id lsaId = new IdImpl(lsaIdInt);
				if (!this.signalSystems.getSignalSystemDefinitions().containsKey(lsaId)){
					log.error("No signal system definition found for node id " + knotenId + " -> lsa id " 
							+ lsaId + " ! removing node data!");
					malformedNodeIds.add(knotenId);
				}
			}
		}
		for (Integer knotenId : malformedNodeIds){
			log.warn("removed knoten id " + knotenId + " because no signal system definition can be found for knoten -> lsa mapping ");
			knotenLsaSpurMap.remove(knotenId);
		}
	}

	
	
	private void preprocessData(Map<Integer, Map<Integer, List<Integer>>> knotenLsaSpurMap, 
			Map<Integer, Map<Integer, String>> knotenSpurLinkMap, Map<Integer, Integer> knotenLsaMap){
		log.info("starting preprocessing...");
		
		preprocessKnotenLsaSpurMap(knotenLsaSpurMap, knotenSpurLinkMap);
		preprocessSystemDefinitions(knotenLsaSpurMap);
		
		
		
		log.info("finished preprocessing!");	
	}
	
	

	public void processSignalSystems(Map<Integer, Map<Integer, List<Integer>>> knotenLsaSpurMap, 
			Map<Integer, Map<Integer, String>> knotenSpurLinkMap, Map<Integer, Integer> knotenLsaMap){
		System.out.println();
		log.info("##########################################################");
		log.info("Creating signal systems group definitions");
		log.info("##########################################################");

		preprocessData(knotenLsaSpurMap, knotenSpurLinkMap, knotenLsaMap);
		
		//create the signal groups
		for (Integer nodeId : knotenLsaSpurMap.keySet()) {
			System.out.println();
			log.info("##########################################################");
			log.info("processing node id " + nodeId);
			log.info("##########################################################");
			Map<Integer,  List<Integer>> lsaSpurMap = knotenLsaSpurMap.get(nodeId);
			Map<Integer, String> spurLinkMap = knotenSpurLinkMap.get(nodeId);
			
			for (Integer lsaId : lsaSpurMap.keySet()) {				
				//first we get the link id for the spur
				String linkIdString = spurLinkMap.get(lsaId);
				
				if ((linkIdString != null) && linkIdString.matches("[\\d]+")){
					//if this is valid....
					Id linkId = new IdImpl(linkIdString);
					//check if there is already a SignalGroupDefinition
					BasicSignalGroupDefinition sg = signalSystems.getSignalGroupDefinitions().get(linkId);
					// if not, create one SingalGroupDefinition for each link
					if (sg == null){
						/* this was a rather complicated way of doing the same twice
//						Integer spurId = knotenLsaSpurMap.get(nodeId).get(lsaId).get(0);
//						String spurLinkId = knotenSpurLinkMap.get(nodeId).get(spurId);
//						Id linkRefId = new IdImpl(spurLinkId);
						//old signalGroupDefId was new IdImpl(fromSGId.intValue())*/
						//this is much easier
						Id signalSystemDefinitionId = new IdImpl(lsaId.intValue());
						//if the signalSystem is not existing abort
						if (!this.signalSystems.getSignalSystemDefinitions().containsKey(signalSystemDefinitionId)){
							log.error("Cannot create SignalGroupDefinition id " + linkId + 
									" for link " + linkId + " cause the referenced signal system id " + signalSystemDefinitionId + " is not existing!");
							continue;
						}
						sg = signalSystems.getBuilder().createSignalGroupDefinition(linkId, linkId);
						sg.setSignalSystemDefinitionId(signalSystemDefinitionId);
					}
					
					//add lanes and toLinks
//					sg = basicSGs.get(new IdImpl(laneLinkMapping.get(nodeId).get(fromSGId)));
					List<Integer> spuren = lsaSpurMap.get(lsaId);
					for (Integer spurIdInteger : spuren) {
						String spurLinkIdString = spurLinkMap.get(spurIdInteger);
						if ((spurLinkIdString != null) && spurLinkIdString.matches("[\\d]+")){
							//lanes 
							Id spurId = new IdImpl(spurIdInteger);
							if((sg.getLaneIds() == null) || !sg.getLaneIds().contains(spurId)){
								sg.addLaneId(spurId);
							}
							//toLinks
							if((sg.getToLinkIds() == null) || !sg.getToLinkIds().contains(new IdImpl(spurLinkMap.get(spurIdInteger)))){
								sg.addToLinkId(new IdImpl(spurLinkMap.get(spurIdInteger)));	
							}
						}
						else {
							log.error("Cannot find spur -> link mapping for spur id " + spurIdInteger);
						}
					}
					if(sg.getLaneIds() != null){
							signalSystems.addSignalGroupDefinition(sg);
					}
				} //end if
				else {
					log.error("Cannot create signalGroupDefinition for nodeId " + nodeId + " and lsaId " + lsaId + " cause the " +
							" link id string is" + linkIdString);
				}
			} //end for			
		}
	}
	
	

}
