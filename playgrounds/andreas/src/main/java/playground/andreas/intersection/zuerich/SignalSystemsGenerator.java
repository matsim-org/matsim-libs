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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.signals.data.signalsystems.v20.SignalData;
import org.matsim.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signals.model.Signal;
import org.matsim.signals.model.SignalSystem;


/**
 * @author dgrether
 *
 */
public class SignalSystemsGenerator {
	
	private static final Logger log = Logger.getLogger(SignalSystemsGenerator.class);

	private Network network;
	private LaneDefinitions20 laneDefinitions;
	private SignalSystemsData signalSystems;

	public SignalSystemsGenerator(Network net, LaneDefinitions20 laneDefs, SignalSystemsData signalSystems2) {
		this.network = net;
		this.laneDefinitions = laneDefs;
		this.signalSystems = signalSystems2;
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
		for (Id<SignalSystem> signalSystemId : this.signalSystems.getSignalSystemData().keySet()) {
			Integer id = Integer.valueOf(signalSystemId.toString());
			if (!knotenLsaSpurMap.containsKey(id)) {
				malformedSignalSystems.add(signalSystemId);
			}
		}
		// remove malformed
		for (Id<SignalSystem> signalSystemId : malformedSignalSystems){
			log.warn("removed signal system id " + signalSystemId + " from signalSystemDefinitions because no knotenLsaSpurMapping can be found");
			this.signalSystems.getSignalSystemData().remove(signalSystemId);
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
		for (Id<SignalSystem> signalSystemId : this.signalSystems.getSignalSystemData().keySet()){
			System.out.println();
			log.info("##########################################################");
			log.info("processing signalSystemDefinition " + signalSystemId);
			log.info("##########################################################");
			Integer nodeId = Integer.parseInt(signalSystemId.toString());
			Map<Integer,  List<Integer>> lsaSpurMap = knotenLsaSpurMap.get(nodeId);
			Map<Integer, String> spurLinkMap = knotenSpurLinkMap.get(nodeId);

			SignalSystemData signalSystem = this.signalSystems.getSignalSystemData().get(signalSystemId);
			
			for (Integer signalGroupNr : lsaSpurMap.keySet()) {				
				Id<Signal> signalGroupId = Id.create(signalGroupNr, Signal.class);
				//first we get the link id for the spur
				String linkIdString = spurLinkMap.get(signalGroupNr);
				
				if ((linkIdString != null) && linkIdString.matches("[\\d]+")){
					//if this is valid....
					Id<Link> linkId = Id.create(linkIdString, Link.class);
					//check if there is already a SignalGroupDefinition
					
					SignalData signal = signalSystems.getFactory().createSignalData(signalGroupId);
					signal.setLinkId(linkId);
					signalSystem.addSignalData(signal);
						
						//add lanes and toLinks
						List<Integer> spuren = lsaSpurMap.get(signalGroupNr);
						for (Integer spurIdInteger : spuren) {
							//lanes 
							Id<Lane> spurId = Id.create(spurIdInteger, Lane.class);
							LanesToLinkAssignment20 l2lAssignment = this.laneDefinitions.getLanesToLinkAssignments().get(linkId);
							if ((l2lAssignment != null) 
									&& l2lAssignment.getLanes().containsKey(spurId)){
								signal.addLaneId(spurId);
								
								//toLinks no longer have to be added as they are stored in the lane anyway
//								for (Id toLinkId : l2lAssignment.getLanes().get(spurId).getToLinkIds()){
//									sg.addToLinkId(toLinkId);
//								}
							}
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
