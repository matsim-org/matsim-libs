/* *********************************************************************** *
 * project: org.matsim.*
 * DgCalculateSignalGroups
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsDataFactory;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsDataImpl;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;


/**
 * @author dgrether
 *
 */
public class DgCalculateSignalGroups {
	
	public enum Direction {BACK, LEFT, RIGHT, STRAIGHT;}

	private class SignalMetaData {
		
		private SignalData signal;
		private List<Id> toLinkIds;

		private Set<Direction> directions = new HashSet<Direction>();
		
		public SignalMetaData(SignalData signal) {
			this.signal = signal;
		}

		public boolean isLeftTurnOnly() {
			return (!this.directions.contains(Direction.RIGHT)) && (!this.directions.contains(Direction.STRAIGHT));
		}

		public void setToLinkIds(List<Id> toLinkIds) {
			this.toLinkIds = toLinkIds;
		}

		public void addDirection(Direction dir) {
			this.directions.add(dir);
			log.error("singal " + signal.getId() + " got direction " + dir);
		}
		
	};
	
	private static final Logger log = Logger.getLogger(DgCalculateSignalGroups.class);

	private Network net;
	private LaneDefinitions lanes;
	private SignalSystemsData signalSystems;
	
	private double right = -Math.PI/4;
	private double left = Math.PI/4;

	
	
	public DgCalculateSignalGroups(SignalSystemsData signalSystems, Network net){
		this.signalSystems = signalSystems;
		this.net = net;
	}
	
	public DgCalculateSignalGroups(SignalSystemsData signalSystems2, NetworkImpl network,
			LaneDefinitions laneDefinitions) {
		this(signalSystems2, network);
		this.lanes = laneDefinitions;
	}
	

	public SignalGroupsData calculateSignalGroupsData() {
		SignalGroupsData groupsData = new SignalGroupsDataImpl();
		for (SignalSystemData ssd :  this.signalSystems.getSignalSystemData().values()){
			this.calculateGroups4SignalSystem(ssd, groupsData);
		}
		return groupsData;
	}
	
	private List<Id> getToLinkIdsOfSignal(SignalData signal){
		//get the toLinks of a signal
		List<Id> toLinkIds = new ArrayList<Id>();
		if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()){ //the signal is on one or several lanes -> the toLinks are retrieved from there
			LanesToLinkAssignment l2l = this.lanes.getLanesToLinkAssignments().get(signal.getLinkId());
			for (Id laneId : signal.getLaneIds()){
				toLinkIds.addAll(l2l.getLanes().get(laneId).getToLinkIds());
			}
		}
		else { //the signal is on a link -> the toLinks are retrieved from the node
		  Link l = this.net.getLinks().get(signal.getLinkId());
		  toLinkIds.addAll(l.getToNode().getOutLinks().keySet());
		}
		return toLinkIds;
	}
	
	private SignalMetaData createMetaData4Signal(SignalData signal, Link link){
		List<Id> toLinkIds = this.getToLinkIdsOfSignal(signal);
		SignalMetaData md = new SignalMetaData(signal);
		md.setToLinkIds(toLinkIds);
		
		for (Id toLinkId : toLinkIds) {
			Link toLink = this.net.getLinks().get(toLinkId);
			if (!(toLink.getToNode().equals(link.getFromNode()))){
				double angle = this.calculateAngle(this.getVector(link), this.getVector(toLink));
				log.debug("angle " + angle / Math.PI * 180.0 + " between link " + link.getId() + " and toLink " + toLink.getId());
				if (angle <= right){
					md.addDirection(Direction.RIGHT);
				}
				else if (angle >= left){
					md.addDirection(Direction.LEFT);
				}
				else {
					md.addDirection(Direction.STRAIGHT);
				}
			}
			else {
				md.addDirection(Direction.BACK);
			}
		}
		return md;
	}

	
	
	private SignalGroupData createAndAddSignalGroup(SignalGroupsData groupsData, Id systemId, int groupId){
		SignalGroupsDataFactory groupsFactory = groupsData.getFactory();
		SignalGroupData group = groupsFactory.createSignalGroupData(systemId, new IdImpl(groupId));
		groupsData.addSignalGroupData(group);
		log.debug("created signal group id " + group.getId() + " for system id " + group.getSignalSystemId());
		return group;
	}
	
	
	private void calculateGroups4SignalSystem(SignalSystemData ssd, SignalGroupsData groupsData) {
		
		//search all links that have a signal attached
		//and do some further preprocessing / indexing
		Set<Link> signalizedLinkSet = new HashSet<Link>();
		Map<Link, Set<SignalData>> link2SignalMap = new HashMap<Link, Set<SignalData>>();
		Map<Id, SignalMetaData> signalIdMetadataMap = new HashMap<Id, SignalMetaData>();
		for (SignalData signal : ssd.getSignalData().values()){
			log.error("preprocessing signal : " + signal.getId());
			Link link = this.net.getLinks().get(signal.getLinkId());
			if (link == null) {
				throw new IllegalStateException("link id " + signal.getLinkId() + " not found in network");
			}
			signalizedLinkSet.add(link);

			if (!link2SignalMap.containsKey(link)){
				link2SignalMap.put(link, new HashSet<SignalData>());
			}
			link2SignalMap.get(link).add(signal);
			
			SignalMetaData md = this.createMetaData4Signal(signal, link);
			signalIdMetadataMap.put(signal.getId(), md);
		}
		
		
		//algorithm starts
		
		Set<SignalData> processedSignals = new HashSet<SignalData>();
		Set<Link> processedLinks = new HashSet<Link>();
		int groupIdCounter = 1;
		SignalGroupData group;
//		if (signalizedLinkSet.size() == 3){
			for (Link signalizedLink : signalizedLinkSet){
				log.debug("Processing signalized Link : " + signalizedLink.getId());
				Link oppositeInLink = this.calculateCorrespondingInLink(signalizedLink);
				
				if (oppositeInLink == null){
					log.debug("  No opposite direction in Link found...");
					group = this.createAndAddSignalGroup(groupsData, ssd.getId(), groupIdCounter);
					groupIdCounter++;
					for (SignalData signal : link2SignalMap.get(signalizedLink)){
						group.addSignalId(signal.getId());
						log.debug("    added signal: " + signal.getId() + " to group " + group.getId());
					}
				}
				else { // there is  a opposite direction
					if (!processedLinks.contains(signalizedLink) && !processedLinks.contains(oppositeInLink)){
						log.debug("  Found Link Id " + oppositeInLink.getId() + " as opposite direction");
						group = this.createAndAddSignalGroup(groupsData, ssd.getId(), groupIdCounter);
						groupIdCounter++;
						
						List<SignalData> allsignals = new LinkedList<SignalData>();
						allsignals.addAll(link2SignalMap.get(signalizedLink));
						allsignals.addAll(link2SignalMap.get(oppositeInLink));
						
						List<SignalData> leftTurnSignals = new LinkedList<SignalData>();
						//first group the signals that are for right or straight ahead direction
						for (SignalData signal : allsignals){
							SignalMetaData md = signalIdMetadataMap.get(signal.getId());
							if (md.isLeftTurnOnly()) {
								leftTurnSignals.add(signal);
								
							}
							else {
								group.addSignalId(signal.getId());
								log.debug("    added signal: " + signal.getId() + " to group " + group.getId());
							}
						}
						//TODO this could be optimized by adding the 
						if (!leftTurnSignals.isEmpty()){
							group = this.createAndAddSignalGroup(groupsData, ssd.getId(), groupIdCounter);
							groupIdCounter++;
							for (SignalData leftSignal : leftTurnSignals){
								group.addSignalId(leftSignal.getId());
								log.debug("    added signal: " + leftSignal.getId() + " to group " + group.getId());
							}
						}
						
						processedLinks.add(oppositeInLink);
						processedLinks.add(signalizedLink);
					}
				}
			}
	}
	
	
	private double calculateAngle(Coord vec1, Coord vec2){
		double thetaInLink = Math.atan2(vec1.getY(), vec1.getX());
		
		double thetaCorrLink = Math.atan2(vec2.getY(), vec2.getX());
		double thetaDiff = thetaCorrLink - thetaInLink;
		if (thetaDiff < -Math.PI){
			thetaDiff += 2 * Math.PI;
		} else if (thetaDiff > Math.PI){
			thetaDiff -= 2 * Math.PI;
		}
		return thetaDiff;
	}
	
	
	/**
	 * Berechnet für den gegebenen link den geradeauslink bzw. dessen rückrichtung
	 * im wesentlichen gedanken von seite 24 daroeder
	 * @return may return null
	 */
	private Link calculateCorrespondingInLink(Link link){
		Coord vectorInLink = getVector(link);
		double temp = Math.PI *3/8;
		Map<Link, Double> inLinkDeltaAngleMap = new HashMap<Link, Double>();
				
		for (Link inLink : link.getToNode().getInLinks().values()){
			Coord coordCorrLink = getVector(inLink);
			double angle = this.calculateAngle(vectorInLink, coordCorrLink);
//			log.debug("angle " + angle / Math.PI * 180.0 + " between link " + link.getId() + " and " + inLink.getId());
			inLinkDeltaAngleMap.put(inLink, Math.abs(angle));
		}
		
		//we search for the link with the smallest angel
		Link l = null;
		for (Entry<Link, Double> e :  inLinkDeltaAngleMap.entrySet()) {
			if ((Math.PI - e.getValue()) < temp ){
				temp = Math.PI - e.getValue();
				//hä? das blick ich nicht so ganz, ist auch nicht dokumentiert
				if (link.getToNode().getInLinks().size() > 2){
					l = e.getKey();
				}
				else{
					l = null;
				}
			}
		}
		return l;
	}
	
	private Coord getVector(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();		
		return new CoordImpl(x, y);
	}

}
