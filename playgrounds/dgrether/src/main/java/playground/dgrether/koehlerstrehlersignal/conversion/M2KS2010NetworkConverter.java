/* *********************************************************************** *
 * project: org.matsim.*
 * DgMatsim2KoehlerStrehler2010ModelConverter
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
package playground.dgrether.koehlerstrehlersignal.conversion;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneData20;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.model.Signal;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.vis.vecmathutils.VectorUtils;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgGreen;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Class to convert a MATSim network into a KS-model network with crossings and streets.
 * BTU Cottbus needs this network format to optimize signal plans with CPLEX.
 * 
 * @author dgrether
 * @author tthunig
 *
 */
public class M2KS2010NetworkConverter {
	
	private static final Logger log = Logger.getLogger(M2KS2010NetworkConverter.class);
	
	private Integer cycle = null;
//	public static final String DEFAULT_PROGRAM_ID = "4711";

	private DgKSNetwork dgNetwork;
	private double timeInterval;

	private DgIdConverter idConverter;

	private Set<Id<Link>> signalizedLinks;
	
	private Envelope signalsBoundingBox;
	
	public M2KS2010NetworkConverter(DgIdConverter idConverter){
		this.idConverter = idConverter;
	}
	
	public DgKSNetwork convertNetworkLanesAndSignals(Network network, LaneDefinitions20 lanes, 
			SignalsData signals, double startTime, double endTime) {
		return this.convertNetworkLanesAndSignals(network, lanes, signals, null, startTime, endTime);
	}
	
	/**
	 * converts the given matsim network into a ks-model network with crossings and streets and returns it
	 * 
	 * @param network the matsim network to convert
	 * @param lanes
	 * @param signals
	 * @param signalsBoundingBox nodes within this envelop will be extended spatially
	 * @param startTime of the simulation
	 * @param endTime of the simulation
	 * @return the corresponding ks-model network
	 */
	public DgKSNetwork convertNetworkLanesAndSignals(Network network, LaneDefinitions20 lanes,
			SignalsData signals, Envelope signalsBoundingBox, double startTime, double endTime) {
		log.info("Checking cycle time...");
		this.cycle = readCycle(signals);
		log.info("cycle set to " + this.cycle);
		signalizedLinks = this.getSignalizedLinkIds(signals.getSignalSystemsData());
		log.info("Converting network ...");
		this.timeInterval = endTime - startTime;
		this.signalsBoundingBox = signalsBoundingBox;
		this.dgNetwork = this.convertNetwork(network, lanes, signals);
		log.info("Network converted.");
		return this.dgNetwork ;
	}


	private int readCycle(SignalsData signalsData){
		Integer c = null;
		for (SignalSystemControllerData ssc :  signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().values()) {
			for (SignalPlanData plan : ssc.getSignalPlanData().values()){
				if (c == null){
					c = plan.getCycleTime();
				}
				else if (c != plan.getCycleTime()){
					throw new IllegalStateException("Signal plans must have a common cycle time!");
				}
			}
		}
		return c;
	}
	
	/*
	 * conversion of extended nodes:
	 *   fromLink -> toLink : 2 crossing nodes + 1 light
	 */
	private DgKSNetwork convertNetwork(Network net, LaneDefinitions20 lanes, SignalsData signalsData) {
		DgKSNetwork ksnet = new DgKSNetwork();
		/* create a crossing for each node (crossing id generated from node id).
		 * add the single crossing node for each not extended crossing (crossing node id generated from node id).
		 */
		this.convertNodes2Crossings(ksnet, net);
		/*
		 * convert all links to streets (street id generated from link id).
		 * add extended crossing nodes for the already created corresponding extended crossings
		 * (extended crossing node id generated from adjacent link id).
		 */
		this.convertLinks2Streets(ksnet, net);

		//loop over links and create layout (i.e. lights and programs) of target crossing (if it is expanded)
		for (Link link : net.getLinks().values()){
			// the node id of the matsim network gives the crossing id
			DgCrossing crossing = ksnet.getCrossings().get(this.idConverter.convertNodeId2CrossingId(link.getToNode().getId())); 
			// lights and programs are only necessary for expanded crossings
			if (!crossing.getType().equals(TtCrossingType.NOTEXPAND)){
				//prepare some objects/data
				Link backLink = this.getBackLink(link);
				Id<Link> backLinkId = (backLink == null) ?  null : backLink.getId();
				DgCrossingNode inLinkToNode = crossing.getNodes().get(this.idConverter.convertLinkId2ToCrossingNodeId(link.getId()));
				LanesToLinkAssignment20 l2l = lanes.getLanesToLinkAssignments().get(link.getId());
				//create crossing layout
				if (signalizedLinks.contains(link.getId())){
					log.debug("link: " + link.getId() + " is signalized...");
					SignalSystemData system = this.getSignalSystem4SignalizedLinkId(signalsData.getSignalSystemsData(), link.getId());
					this.createCrossing4SignalizedLink(crossing, link, inLinkToNode, backLinkId, l2l, system, signalsData);
				}
				else {
					log.debug("link: " + link.getId() + " not signalized...");
					this.createCrossing4NotSignalizedLink(crossing, link, inLinkToNode, backLinkId, l2l);
				}
			}
		}
		return ksnet;
	}

	
	/**
	 * creates crossings in ksNet for all nodes in the matsim network net.
	 * if a node lies within the signals bounding box (i.e. should be expanded)
	 * this method creates a corresponding crossing with the type "fixed" (if the node is signalized)
	 * or "equalRank" (else) respectively. this crossing has no crossing nodes so far.
	 * if a node lies outside the signals bounding box this method creates the complete crossing, 
	 * which gets the type "notExpand" and a single crossing node.
	 * 
	 * @param ksNet
	 * @param net
	 */
	private void convertNodes2Crossings(DgKSNetwork ksNet, Network net){
		for (Node node : net.getNodes().values()){
			DgCrossing crossing = new DgCrossing(this.idConverter.convertNodeId2CrossingId(node.getId()));
			
			// create crossing type
			Coordinate nodeCoordinate = MGC.coord2Coordinate(node.getCoord());
			if (this.signalsBoundingBox == null || // there is no signals bounding box to stop expansion -> all nodes will be expanded
					this.signalsBoundingBox.contains(nodeCoordinate)){ // node is within the signals bounding box
				
				// create crossing type "fixed" if node is signalized, "equalRank" else
				for (Link link : node.getInLinks().values()){
					if (signalizedLinks.contains(link.getId())){ // node is signalized
						crossing.setType(TtCrossingType.FIXED);
//						// create default program for signalized crossing
//						DgProgram program = new DgProgram(Id.create(M2KS2010NetworkConverter.DEFAULT_PROGRAM_ID, DgProgram.class));
//						program.setCycle(this.cycle);
//						crossing.addProgram(program);
					} 
				}
				if (crossing.getType() == null){ // node isn't signalized, but within the signals bounding box
					crossing.setType(TtCrossingType.EQUALRANK);
				}
			}
			else{ // node is outside the signals bounding box
				crossing.setType(TtCrossingType.NOTEXPAND);
				// create and add the single crossing node of the not expanded crossing
				DgCrossingNode crossingNode = new DgCrossingNode(this.idConverter.convertNodeId2NotExpandedCrossingNodeId(node.getId()));
				crossingNode.setCoordinate(node.getCoord());
				crossing.addNode(crossingNode);
			}
			
			ksNet.addCrossing(crossing);			
		}
	}
	
	/**
	 * creates streets in ksnet for all links in the matsim network net.
	 * if a from or to node of a link lies within the signals bounding box (i.e. should be expanded) 
	 * this method creates a new crossing node for this street in the expanded network ksnet.
	 * if a node lies outside the signals bounding box the single crossing node for the not expanded crossing 
	 * already exists (see convertNodes2Crossings(...)) and is used by this method to create the street.
	 * 
	 * @param ksnet
	 * @param net
	 */
	private void convertLinks2Streets(DgKSNetwork ksnet, Network net){
		
		for (Link link : net.getLinks().values()){
			Node mFromNode = link.getFromNode();
			Node mToNode = link.getToNode();
			Tuple<Coord, Coord> startEnd = this.scaleLinkCoordinates(link.getLength(), mFromNode.getCoord(), mToNode.getCoord());

			// get from node
			DgCrossing fromNodeCrossing = ksnet.getCrossings().get(this.idConverter.convertNodeId2CrossingId(mFromNode.getId()));
			DgCrossingNode fromNode;
			// create from node for expanded crossings
			if (!fromNodeCrossing.getType().equals(TtCrossingType.NOTEXPAND)){
				fromNode = new DgCrossingNode(this.idConverter.convertLinkId2FromCrossingNodeId(link.getId()));
				fromNode.setCoordinate(startEnd.getFirst());
				fromNodeCrossing.addNode(fromNode);
			}
			else{ // node for not expanded crossing already exists
				fromNode = fromNodeCrossing.getNodes().get(this.idConverter.convertNodeId2NotExpandedCrossingNodeId(mFromNode.getId()));
			}
			
			// get to node
			DgCrossing toNodeCrossing = ksnet.getCrossings().get(this.idConverter.convertNodeId2CrossingId(mToNode.getId()));
			DgCrossingNode toNode;
			// create to node for expanded crossings
			if (!toNodeCrossing.getType().equals(TtCrossingType.NOTEXPAND)){
				toNode = new DgCrossingNode(this.idConverter.convertLinkId2ToCrossingNodeId(link.getId()));
				toNode.setCoordinate(startEnd.getSecond());
				toNodeCrossing.addNode(toNode);
			}
			else{ // node for not expanded crossing already exists
				toNode = toNodeCrossing.getNodes().get(this.idConverter.convertNodeId2NotExpandedCrossingNodeId(mToNode.getId()));
			}
			
			DgStreet street = new DgStreet(this.idConverter.convertLinkId2StreetId(link.getId()), fromNode, toNode);
			double fsd = link.getLength() / link.getFreespeed();
			long fs = Math.round(fsd);
			if (fs != 0){
				street.setCost(fs);
			}
			else {
				log.warn("Street id " + street.getId() + " has a freespeed tt of " + fsd + " that is rounded to " + fs + " replacing by 1");
				street.setCost(0);
			}
			double capacity = link.getCapacity() / net.getCapacityPeriod() * this.timeInterval;
			street.setCapacity(capacity);
			ksnet.addStreet(street);
		}
	}
	
	
	/**
	 * scales the link start and end coordinates based on a node offset to create extended crossing nodes, 
	 * i.e. the link will be shortened at the beginning and the end.
	 * the scaled start coordinate gives the coordinate for the extended crossing node corresponding to the from crossing of the link.
	 * the scaled end coordinate gives this information for the to crossing of the link.
	 * 
	 * @param linkLength currently not used. we use the euclidean distance between start and end coordinate as link length.
	 * @param linkStartCoord the start coordinate of the link
	 * @param linkEndCoord the end coordinate of the link
	 * @return a tuple of the scaled start and end coordinates, so the coordinates for the extended crossing nodes 
	 */
	private Tuple<Coord,Coord> scaleLinkCoordinates(double linkLength, Coord linkStartCoord, Coord linkEndCoord){
		double nodeOffsetMeter = 20.0;
		Point2D.Double linkStart = new Point2D.Double(linkStartCoord.getX(), linkStartCoord.getY());
		Point2D.Double linkEnd =  new Point2D.Double(linkEndCoord.getX(), linkEndCoord.getY());
		
		//calculate length and normal
		Point2D.Double deltaLink = new Point2D.Double(linkEnd.x - linkStart.x, linkEnd.y - linkStart.y);
		double euclideanLinkLength = this.calculateEuclideanLinkLength(deltaLink);
//		//calculate the correction factor if real link length is different than euclidean distance
//		double linkLengthCorrectionFactor = euclideanLinkLength / linkLength;
//		Point2D.Double deltaLinkNorm = new Point2D.Double(deltaLink.x / euclideanLinkLength, deltaLink.y / euclideanLinkLength);
//		Point2D.Double normalizedOrthogonal = new Point2D.Double(deltaLinkNorm.y, - deltaLinkNorm.x);
		
		//first calculate the scale of the link based on the node offset, i.e. the link will be shortened at the beginning and the end 
		double linkScale = 1.0;
		if ((euclideanLinkLength * 0.2) > (2.0 * nodeOffsetMeter)){ // 2* nodeoffset is less than 20%
			linkScale = (euclideanLinkLength - (2.0 * nodeOffsetMeter)) / euclideanLinkLength;
		}
		else { // use 80 % as euclidean length (because nodeoffset is to big)
			linkScale = euclideanLinkLength * 0.8 / euclideanLinkLength;
		}
		
		//scale link
		Tuple<Double, Double> scaledLink = VectorUtils.scaleVector(linkStart, linkEnd, linkScale);
		Point2D.Double scaledLinkEnd = scaledLink.getSecond();
		Point2D.Double scaledLinkStart = scaledLink.getFirst();
		Coord start = new CoordImpl(scaledLinkStart.x, scaledLinkStart.y); 
		Coord end = new CoordImpl(scaledLinkEnd.x, scaledLinkEnd.y);
		return new Tuple<Coord, Coord>(start, end);
	}

	private double calculateEuclideanLinkLength(Point2D.Double deltaLink) {
		return Math.sqrt(Math.pow(deltaLink.x, 2) + Math.pow(deltaLink.y, 2));
	}
	
	private Tuple<SignalPlanData, SignalGroupSettingsData> getPlanAndSignalGroupSettings4Signal(Id<SignalSystem> signalSystemId, Id<Signal> signalId, SignalsData signalsData){
		SignalSystemControllerData controllData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(signalSystemId);
		SignalPlanData signalPlan = controllData.getSignalPlanData().values().iterator().next();
		SignalGroupData signalGroup = DgSignalsUtils.getSignalGroup4SignalId(signalSystemId, signalId, signalsData.getSignalGroupsData());
		return new Tuple<SignalPlanData, SignalGroupSettingsData>(signalPlan, signalPlan.getSignalGroupSettingsDataByGroupId().get(signalGroup.getId()));
	}

	
	/**
	 * creates a light (i.e. a street representing the connection of crossing nodes of one crossing)
	 * between the inLink crossing node and the outLink crossing node. 
	 * lights are only used for extended crossings. so the outLink gives the outLink crossing node.
	 * 
	 * @param fromLinkId the matsim id of the inLink
	 * @param fromLaneId 
	 * @param outLinkId the matsim id of the outLink
	 * @param backLinkId the back link id of the fromLink
	 * @param inLinkToNode the corresponding target crossing node of the fromLink
	 * @param crossing the target crossing of the fromLink
	 * @return the id of the created light
	 */
	private Id<DgGreen> createLights(Id<Link> fromLinkId, Id<Lane> fromLaneId, Id<Link> outLinkId, Id<Link> backLinkId, DgCrossingNode inLinkToNode, DgCrossing crossing){
		if (backLinkId != null && backLinkId.equals(outLinkId)){
			return null; //do nothing if it is the backlink
		}
		Id<DgGreen> lightId = this.idConverter.convertFromLinkIdToLinkId2LightId(fromLinkId, fromLaneId, outLinkId);
		log.debug("    light id: " + lightId);
		Id<Link> convertedOutLinkId = Id.create(this.idConverter.convertLinkId2FromCrossingNodeId(outLinkId), Link.class);
		log.debug("    outLinkId : " + outLinkId + " converted id: " + convertedOutLinkId);
		DgCrossingNode outLinkFromNode = crossing.getNodes().get(convertedOutLinkId);
		if (outLinkFromNode == null){
			log.error("Crossing " + crossing.getId() + " has no node with id " + convertedOutLinkId);
			throw new IllegalStateException("outLinkFromNode not found.");
//			return null;
		}
		DgStreet street = new DgStreet(Id.create(lightId, DgStreet.class), inLinkToNode, outLinkFromNode);
		street.setCost(0);
		crossing.addLight(street);
		return lightId;
	}
	
	
	/**
	 * creates the crossing layout (lights and programs) for the target crossing of signalized links.
	 * Maps a signalized MATSim Link's turning moves and signalization to lights and greens, i.e. 1 allowed turning move  => 1 light + 1 green
	 * Turning moves are given by:
	 *   a) the outLinks of the toNode of the Link, if no lanes are given and there are no turning move restrictions set for the Signal
	 *   b) the turning move restrictions of multiple signals attached to the link 
	 *   d) the turing move restrictions of the signal, if it is attached to a lane
	 *   c) the toLinks of the lanes attached to the link, if there are no turning move restrictions for the signal
	 * If there are several signals without turning move restrictions on a link or a lane nothing can be created because this is an inconsistent state of 
	 * the input data:  thus the programs/plans for the signal might be ambiguous, an exception is thrown.
	 * 
	 * @param crossing the target crossing of the link
	 * @param link
	 * @param inLinkToNode the corresponding target crossing node of the link
	 * @param backLinkId
	 * @param l2l
	 * @param system
	 * @param signalsData
	 */
	private void createCrossing4SignalizedLink(DgCrossing crossing, Link link, DgCrossingNode inLinkToNode, Id<Link> backLinkId, 
			LanesToLinkAssignment20 l2l, SignalSystemData system, SignalsData signalsData) {
//		//remove default program
//		if ( crossing.getPrograms().containsKey(M2KS2010NetworkConverter.DEFAULT_PROGRAM_ID)) {
//			crossing.getPrograms().remove(M2KS2010NetworkConverter.DEFAULT_PROGRAM_ID);
//		}
		//create program if not existing...
		DgProgram program = null;
		if (! crossing.getPrograms().containsKey(system.getId())){
			program = new DgProgram(Id.create(system.getId(), DgProgram.class));
			program.setCycle(this.cycle);
			crossing.addProgram(program);
		}
		else {
			program = crossing.getPrograms().get(system.getId());
		}
		
		List<SignalData> signals4Link = this.getSignals4LinkId(system, link.getId());
		//first get the outlinks that are controlled by the signal
		for (SignalData signal : signals4Link){
			log.debug("    signal: " + signal.getId() + " system: " + system.getId());
			Id<DgGreen> lightId = null;
			if (l2l == null) {
				Set<Id<Link>> outLinkIds = new HashSet<>();
				if (signals4Link.size() > 1 && (signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty())){
					throw new IllegalStateException("more than one signal on one link but no lanes and no turning move restrictions is not allowed");
				}
				else if (signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty()){
					outLinkIds.addAll(this.getTurningMoves4LinkWoLanes(link));
				}
				else { // we have turning move restrictions
					outLinkIds = signal.getTurningMoveRestrictions();
				}
				//create lights and green settings
				for (Id<Link> outLinkId : outLinkIds){
					log.debug("    outLinkId: " + outLinkId);
					lightId = this.createLights(link.getId(), null, outLinkId, backLinkId, inLinkToNode, crossing);
					log.debug("    created Light " + lightId);
					if (lightId != null){
						Tuple<SignalPlanData, SignalGroupSettingsData> planGroupSettings = this.getPlanAndSignalGroupSettings4Signal(system.getId(), signal.getId(), signalsData);
						SignalPlanData signalPlan = planGroupSettings.getFirst();
						SignalGroupSettingsData groupSettings = planGroupSettings.getSecond();
						this.createAndAddGreen4Settings(lightId, program, groupSettings, signalPlan);
					}
				}
			}
			else { //link with lanes
				for (Id<Lane> laneId : signal.getLaneIds()){
					LaneData20 lane = l2l.getLanes().get(laneId);
					if (signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty()){ //no turning move restrictions for signal -> outlinks come from lane
						for (Id<Link> outLinkId : lane.getToLinkIds()){
							log.debug("    outLinkId: " + outLinkId);
							lightId = this.createLights(link.getId(), laneId, outLinkId, backLinkId, inLinkToNode, crossing);
							log.debug("    created Light " + lightId);
							if (lightId != null){
								Tuple<SignalPlanData, SignalGroupSettingsData> planGroupSettings = this.getPlanAndSignalGroupSettings4Signal(system.getId(), signal.getId(), signalsData);
								SignalPlanData signalPlan = planGroupSettings.getFirst();
								SignalGroupSettingsData groupSettings = planGroupSettings.getSecond();
								this.createAndAddGreen4Settings(lightId, program, groupSettings, signalPlan);
							}
						}
					}
					else { //turning move restrictions on signal -> outlinks taken from signal
						for (Id<Link> outLinkId : signal.getTurningMoveRestrictions()){
							log.debug("    outLinkId: " + outLinkId);
							lightId = this.createLights(link.getId(), laneId, outLinkId, backLinkId, inLinkToNode, crossing);
							if (lightId != null){
								Tuple<SignalPlanData, SignalGroupSettingsData> planGroupSettings = this.getPlanAndSignalGroupSettings4Signal(system.getId(), signal.getId(), signalsData);
								SignalPlanData signalPlan = planGroupSettings.getFirst();
								SignalGroupSettingsData groupSettings = planGroupSettings.getSecond();
								this.createAndAddGreen4Settings(lightId, program, groupSettings, signalPlan);
							}
						}
					}
				}
			} 
		} 
	}
	
	/**
	 * creates the crossing layout (lights without programs) for the target crossing of non signalized links.
	 * 
	 * @param crossing the target crossing of the link
	 * @param link
	 * @param inLinkToNode the corresponding target crossing node of the link
	 * @param backLinkId
	 * @param l2l
	 */
	private void createCrossing4NotSignalizedLink(DgCrossing crossing, Link link,
			DgCrossingNode inLinkToNode, Id<Link> backLinkId, LanesToLinkAssignment20 l2l) {
//		DgProgram program = null;
//		if (crossing.getPrograms().containsKey(this.DEFAULT_PROGRAM_ID)){
//			program = crossing.getPrograms().get(this.DEFAULT_PROGRAM_ID);
//		}
//		else {
//			log.error("Link: " + link.getId() + " fromNode: " + link.getFromNode().getId() + " toNode: " + link.getToNode().getId());
//			throw new IllegalStateException("Default program must exist at not signalized crossing: " + crossing.getId());
//		}
		if (l2l == null){ // create lights for link without lanes
			List<Id<Link>> toLinks = this.getTurningMoves4LinkWoLanes(link);
			for (Id<Link> outLinkId : toLinks){
				Id<DgGreen> lightId = this.createLights(link.getId(), null, outLinkId, backLinkId, inLinkToNode, crossing);
//				if (lightId != null){
//					this.createAndAddAllTimeGreen(lightId, program);
//				}
			}
		}
		else {
			for (LaneData20 lane : l2l.getLanes().values()){
				// check for outlanes (create only lights for lanes without outlanes, i.e. "last lanes" of a link)
				if (lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty()){
					for (Id<Link> outLinkId : lane.getToLinkIds()){
						Id<DgGreen> lightId = this.createLights(link.getId(), lane.getId(), outLinkId, backLinkId, inLinkToNode, crossing);
//						if (lightId != null){
//							this.createAndAddAllTimeGreen(lightId, program);
//						}
					}
				}
			}
		}
	}

	//TODO check this again which offset is needed for green
	private void createAndAddGreen4Settings(Id<DgGreen> lightId, DgProgram program,
		SignalGroupSettingsData groupSettings, SignalPlanData signalPlan) {
		DgGreen green = new DgGreen(lightId);
		green.setOffset(groupSettings.getOnset());
		green.setLength(this.calculateGreenTimeSeconds(groupSettings, signalPlan.getCycleTime()));
		log.debug("    green time " + green.getLength() + " offset: " + green.getOffset());
		program.addGreen(green);
	}
	
	
	private int calculateGreenTimeSeconds(SignalGroupSettingsData settings, Integer cycle){
		if (settings.getOnset() <= settings.getDropping()) {
			return settings.getDropping() - settings.getOnset();
		}
		else {
			return  settings.getDropping() + (cycle - settings.getOnset()); 
		}
	}

	
	@Deprecated
	private void createAndAddAllTimeGreen(Id<DgGreen> lightId, DgProgram program){
		DgGreen green = new DgGreen(lightId);
		green.setLength(this.cycle);
		green.setOffset(0);
		program.addGreen(green);
	}
	
	
	private SignalSystemData getSignalSystem4SignalizedLinkId(SignalSystemsData signalSystems, Id<Link> linkId){
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()){
			for (SignalData signal : system.getSignalData().values()){
				if (signal.getLinkId().equals(linkId)){
					return system;
				}
			}
		}
		return null;
	}
	
	private List<SignalData> getSignals4LinkId(SignalSystemData system, Id<Link> linkId){
		List<SignalData> signals4Link = new ArrayList<SignalData>();
		for (SignalData signal : system.getSignalData().values()){
			if (signal.getLinkId().equals(linkId)){
				signals4Link.add(signal);
			}
		}
		return signals4Link;
	}

	
	private Link getBackLink(Link link){
		for (Link outLink : link.getToNode().getOutLinks().values()){
			if (link.getFromNode().equals(outLink.getToNode())){
				return outLink;
			}
		}
		return null;
	}
	
	
	private List<Id<Link>> getTurningMoves4LinkWoLanes(Link link){
		List<Id<Link>> outLinks = new ArrayList<>();
		for (Link outLink : link.getToNode().getOutLinks().values()){
			if (!link.getFromNode().equals(outLink.getToNode())){
				outLinks.add(outLink.getId());
			}
		}
		return outLinks;
	}
	
	private Set<Id<Link>> getSignalizedLinkIds(SignalSystemsData signals){
		Map<Id<SignalSystem>, Set<Id<Link>>> signalizedLinksPerSystem = DgSignalsUtils.calculateSignalizedLinksPerSystem(signals);
		Set<Id<Link>> signalizedLinks = new HashSet<>();
		for (Set<Id<Link>> signalizedLinksOfSystem : signalizedLinksPerSystem.values()){
			signalizedLinks.addAll(signalizedLinksOfSystem);
		}
		return signalizedLinks;
	}

	
}
