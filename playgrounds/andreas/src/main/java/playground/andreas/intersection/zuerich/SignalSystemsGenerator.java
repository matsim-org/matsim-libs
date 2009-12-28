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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.lanes.basic.BasicLanesToLinkAssignment;
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
		List<Id> malformedSignalSystems = new ArrayList<Id>();
		//check if for each created signal system a knotenLsaSpur mapping exists
		for (Id signalSystemId : this.signalSystems.getSignalSystemDefinitions().keySet()) {
			Integer id = Integer.valueOf(signalSystemId.toString());
			if (!knotenLsaSpurMap.containsKey(id)) {
				malformedSignalSystems.add(signalSystemId);
			}
		}
		// remove malformed
		for (Id signalSystemId : malformedSignalSystems){
			log.warn("removed signal system id " + signalSystemId + " from signalSystemDefinitions because no knotenLsaSpurMapping can be found");
			this.signalSystems.getSignalSystemDefinitions().remove(signalSystemId);
		}
		
		
		
	}

	
	
	private void preprocessData(Map<Integer, Map<Integer, List<Integer>>> knotenLsaSpurMap, 
			Map<Integer, Map<Integer, String>> knotenSpurLinkMap){
		log.info("starting preprocessing...");
		
		preprocessKnotenLsaSpurMap(knotenLsaSpurMap, knotenSpurLinkMap);
		preprocessSystemDefinitions(knotenLsaSpurMap);
		
		
		
		log.info("finished preprocessing!");	
	}
	
	

	public void processSignalSystems(Map<Integer, Map<Integer, List<Integer>>> knotenLsaSpurMap, 
			Map<Integer, Map<Integer, String>> knotenSpurLinkMap){
		System.out.println();
		log.info("##########################################################");
		log.info("Creating signal systems group definitions");
		log.info("##########################################################");

		preprocessData(knotenLsaSpurMap, knotenSpurLinkMap);
		
		
		//create the signal groups
		for (Id signalSystemId : this.signalSystems.getSignalSystemDefinitions().keySet()){
			System.out.println();
			log.info("##########################################################");
			log.info("processing signalSystemDefinition " + signalSystemId);
			log.info("##########################################################");
			Integer nodeId = Integer.parseInt(signalSystemId.toString());
			Map<Integer,  List<Integer>> lsaSpurMap = knotenLsaSpurMap.get(nodeId);
			Map<Integer, String> spurLinkMap = knotenSpurLinkMap.get(nodeId);

			
			for (Integer signalGroupNr : lsaSpurMap.keySet()) {				
				Id signalGroupId = new IdImpl(signalGroupNr);
				//first we get the link id for the spur
				String linkIdString = spurLinkMap.get(signalGroupNr);
				
				if ((linkIdString != null) && linkIdString.matches("[\\d]+")){
					//if this is valid....
					Id linkId = new IdImpl(linkIdString);
					//check if there is already a SignalGroupDefinition
					if (!this.signalSystems.getSignalGroupDefinitions().containsKey(signalGroupId)){
						BasicSignalGroupDefinition sg	= signalSystems.getFactory().createSignalGroupDefinition(linkId, signalGroupId);
						sg.setSignalSystemDefinitionId(signalSystemId);
						
						//add lanes and toLinks
						List<Integer> spuren = lsaSpurMap.get(signalGroupNr);
						for (Integer spurIdInteger : spuren) {
							//lanes 
							Id spurId = new IdImpl(spurIdInteger);
							BasicLanesToLinkAssignment l2lAssignment = this.laneDefinitions.getLanesToLinkAssignments().get(linkId);
							if ((l2lAssignment != null) 
									&& l2lAssignment.getLanes().containsKey(spurId)){
								if((sg.getLaneIds() == null) || !sg.getLaneIds().contains(spurId)){
									sg.addLaneId(spurId);
								}
								
								//toLinks
								for (Id toLinkId : l2lAssignment.getLanes().get(spurId).getToLinkIds()){
									sg.addToLinkId(toLinkId);
								}
							}
						}
					}
					else {
						log.error("cannot create signalGroup twice for signal system id " + signalSystemId + " and signalGroupId " + signalGroupId);
					}
				}
				else {
					log.error("Cannot create signalGroupDefinition for node/signalSystem Id " + nodeId + " and signalGroupNr " + signalGroupNr + " cause the " +
							" link id string is" + linkIdString);
				}
			}
		}
	}
	
	

}
