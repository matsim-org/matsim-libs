/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.signals.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.xml.sax.Attributes;
/**
 * Osm reader that extends the basic OSMNetworkReader and in addition reads in signals and lanes information.
 * This tool is based on the master thesis of Nils Schirrmacher from 2017 at VSP 
 * and intensively debugged, simplified and cleaned up by Soehnke Braun and Theresa Ziemke (Thunig)
 * 
 * @author tthunig, nschirrmacher, sbraun
 */
public class SignalsAndLanesOsmNetworkReader extends OsmNetworkReader {

	private final static Logger LOG = LogManager.getLogger(SignalsAndLanesOsmNetworkReader.class);

	private final static String TAG_RESTRICTION = "restriction";
	private final static String TAG_TURNLANES = "turn:lanes";
	private final static String TAG_TURNLANESFORW = "turn:lanes:forward";
	private final static String TAG_TURNLANESBACK = "turn:lanes:backward";

	private final static int DEFAULT_LANE_OFFSET = 35;
	private final static int INTERGREENTIME = 5;
	private final static int MIN_GREENTIME = 10;
	private final static double SIGNAL_MERGE_DISTANCE = 30; //changed from 40m sb 25.03.2020
    private final double MERGE_DISTANCE_JUNCTIONS = 30;

    private final static double SIGNAL_LANES_CAPACITY = 2000.0;
	private final static double THROUGHLINK_ANGLE_TOLERANCE = 0.1666667;
	private final static double DEGREE_TOLERANCE_ROUNDABOUTS = 1.;
	private final static int PEDESTRIAN_CROSSING_TIME = 20;
	private final static int CYCLE_TIME = 90;
	private final int minimalTimeForPair = 2 * INTERGREENTIME + 2 * MIN_GREENTIME;

    private final static String ORIG_ID = "origid";
	private final static String TYPE = "type";
	private final static String TO_LINKS_ANGLES = "toLinksAngles";

	private final String OSM_TURN_INFO = "osmTurnInfo";


	private Set<Id<Link>> linksNotMatchingTagsANDnoLanes = new HashSet<>();


	private final Map<Id<Link>, Stack<Stack<Integer>>> laneStacks = new HashMap<>();
	private final Map<Id<Link>, Map<Id<Link>, Double>> allToLinksAngles = new HashMap<>();
	private final Map<Id<Lane>, List<Id<Lane>>> nonCritLanes = new HashMap<>();
	private final Map<Id<Lane>, List<Id<Lane>>> critLanes = new HashMap<>();
	private Map<Id<Link>, Coord> linkToOrigToNodeCoord = new HashMap<>();
	private Map<Id<Link>, Coord> linkToOrigFromNodeCoord = new HashMap<>();
	private Set<Id<Link>> loopLinks = new HashSet<>();

	// Node stuff
	private Set<Long> signalizedOsmNodes = new HashSet<>();
	private Set<Long> crossingOsmNodes = new HashSet<>();
	private Map<Long, OsmNode> oldToMergedJunctionNodeMap = new HashMap<>();
	private Map<Long, Set<OsmRelation>> osmNodeRestrictions = new HashMap<>();
	private Set<Long> nodesNotToMerge = new HashSet<>();


	private boolean mergeOnewaySignalSystems = false;
	private boolean allowUTurnAtLeftLaneOnly = false;
	private boolean makePedestrianSignals = false;
	private boolean acceptFourPlusCrossings = true;
	private boolean saveTurnLanes = false; // add turn info as attribute in lane file


	private final SignalSystemsData systems;
	private final SignalGroupsData groups;
	private final SignalControlData control;
	private final Lanes lanes;

	private BoundingBox bbox = null;

	public SignalsAndLanesOsmNetworkReader(Network network, CoordinateTransformation transformation,
			final SignalsData signalsData, final Lanes lanes) {
		this(network, transformation, true, signalsData, lanes);
	}

	public SignalsAndLanesOsmNetworkReader(Network network, CoordinateTransformation transformation,
			boolean useHighwayDefaults, final SignalsData signalsData, final Lanes lanes) {
		super(network, transformation, useHighwayDefaults);
		
		super.addWayTags(Arrays.asList(TAG_TURNLANES, TAG_TURNLANESFORW, TAG_TURNLANESBACK, TAG_RESTRICTION));
		super.setParser(new SignalLanesOsmXmlParser(this.nodes, this.ways, this.transform));
		this.systems = signalsData.getSignalSystemsData();
		this.groups = signalsData.getSignalGroupsData();
		this.control = signalsData.getSignalControlData();
		this.lanes = lanes;		
	}

	private void stats() {
		LOG.info("MATSim: # links with lanes created: " + this.lanes.getLanesToLinkAssignments().size());
		LOG.info("MATSim: # signals created: " + this.systems.getSignalSystemData().size());
	}
    /*
    Set Bounding Box in which signals and lanes are going to be created
    If Bounding Box == null than signals/lanes are created for the whole network which might take a while...
     */
	public void setBoundingBox(double south, double west, double north, double east) {
	    if (north > south && east > west) {
            Coord nw = this.transform.transform(new Coord(west, north));
            Coord se = this.transform.transform(new Coord(east, south));
            this.bbox = new BoundingBox(se.getY(), nw.getX(), nw.getY(), se.getX());
        } else {
	    	if (!(north > south)){
				throw new RuntimeException("Double value of north coordinate should be larger than south value but was: N:"+north+" <= S:"+south);
			} else {
				throw new RuntimeException("Double value of east coordinate should be larger than west value but was: E:"+east+" <= W:"+west);
			}
        }
	}

    public void setMergeOnewaySignalSystems(boolean mergeOnewaySignalSystems){
        this.mergeOnewaySignalSystems = mergeOnewaySignalSystems;
    }

    public void setAllowUTurnAtLeftLaneOnly(boolean allowUTurnAtLeftLaneOnly){
        this.allowUTurnAtLeftLaneOnly = allowUTurnAtLeftLaneOnly;
    }
    public void setMakePedestrianSignals(boolean makePedestrianSignals){
        this.makePedestrianSignals = makePedestrianSignals;
    }
    public void setAcceptFourPlusCrossings(boolean acceptFourPlusCrossings){
        this.acceptFourPlusCrossings = acceptFourPlusCrossings;
    }

	/**
	 * Extends the super class method: Signal data (which is often located at
	 * upstream nodes) is pushed into intersection nodes, such that signalized 
	 * intersections can more easily be simplified during network simplification
	 */
	@Override
	protected void preprocessOsmData() {
		super.preprocessOsmData();
		// The logic to merge small roundabouts is depreciated as the original reader is doing something similar anyway.
//		simplifiyRoundaboutSignals();
		pushSignalsIntoNearbyJunctions();
		pushingSingnalsIntoEndpoints();
		pushSignalsOverShortWays();
		removeSignalsAtDeadEnds();

		//BoundingBox validation check - check if all if the BoundingBox is set within the range of available Nodes:
		if (this.bbox !=null){
			double maxX = Double.NaN;	//only relevant for the first node
			double minX = 0.;
			double maxY = 0.;
			double minY = 0.;

			// find the range of parsed OSM nodes
			for (OsmNode node : nodes.values()){
				if (Double.isNaN(maxX)){
					maxX = node.coord.getX();
					minX = node.coord.getX();
					maxY = node.coord.getY();
					minY = node.coord.getY();
				} else {
					if (node.coord.getX()> maxX) maxX = node.coord.getX();
					if (node.coord.getX()< minX) minX = node.coord.getX();
					if (node.coord.getY()> maxY) maxY = node.coord.getY();
					if (node.coord.getY()< minY) minY = node.coord.getY();
				}
			}
			// Check if the BoundBox is within that range
			if (!(minX <= this.bbox.west) || !(maxX >= this.bbox.east) || !(minY <= this.bbox.south) || !(maxY >= this.bbox.north)) {
				throw new RuntimeException("The bounding box is not within the range of parsed coordinates. Please verify the box correspond to OSM data.");
			}
			LOG.info("BoundingBox was checked and is valid...");
		} else {
			LOG.warn("No bounding box was set. Convert the whole network...");
		}


	}

	/**
	 * Extends the super class method: Intersections with more than two ways (i.e.
	 * more than one intersection node) are simplified into one intersection node.
	 */
	@Override
	protected void simplifyOsmData() {
		super.simplifyOsmData();
		
		LOG.info("Simplify four node and two node junctions to one node junctions...");
		List<OsmNode> addingNodes = new ArrayList<>();
		List<OsmNode> checkedNodes = new ArrayList<>();
		List<OsmWay> checkedWays = new ArrayList<>();

		findingFourNodeJunctions(addingNodes, checkedNodes);
		LOG.info("Found all Four-Node-Junctions");

		findingMoreNodeJunctions(addingNodes, checkedNodes);
		LOG.info("Found all More-Node-Junctions");

		findingTwoNodeJunctions(addingNodes, checkedNodes);
		LOG.info("Found all Two-Node-Junctions");

        if (this.mergeOnewaySignalSystems) {
			LOG.info("Start merging One-Way signal systems...");
			mergeOnewaySignalSystems(addingNodes, checkedNodes);
			LOG.info("Done merging One-Way signal systems...");
		}

		for (OsmNode node : addingNodes) {
			super.nodes.put(node.id, node);
		}

		addingNodes.clear();
		checkedNodes.clear();
		LOG.info("...done simplifying multiple node junctions.");

	}
	
	/**
	 * Extends the super class method: Only unsignalized nodes can be removed.
	 */
	@Override
	protected boolean canNodeBeRemoved(OsmNode node) {
		return super.canNodeBeRemoved(node) && !signalizedOsmNodes.contains(node.id);
	}

	/**
	 * Extends the super class method: Creates signals and lanes based on OSM data.
	 */
	@Override
	protected void createMatsimData() {
		super.createMatsimData();

		for (Id<Link> linkId : loopLinks){
			network.removeLink(linkId);
		}

		// lanes were already created (via setOrModifyLinkAttributes()) but without toLinks. add toLinks now:
		for (Link link : network.getLinks().values()) {
			if (link.getToNode().getOutLinks().size() >= 1) {
				if (link.getNumberOfLanes() > 1) {
					fillLanesAndCheckRestrictions(link);
				} else {
					Long toNodeId = Long.valueOf(link.getToNode().getId().toString());
					Set<OsmRelation> restrictions = osmNodeRestrictions.get(toNodeId);
					if (restrictions != null && !restrictions.isEmpty() && (this.bbox == null || this.bbox.contains(nodes.get(toNodeId).coord))) {
						// if there exists an Restriction in the ToNode, we want to
						// create a Lane to represent the restriction,
						// as the toLinks cannot be restricted otherwise
						List<LinkVector> outLinks = constructOrderedOutLinkVectors(link);
						createLanes(link, lanes, 1);
						removeRestrictedLinks(link, outLinks);
						LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(link.getId());
						Id<Lane> laneId = Id.create("Lane" + link.getId() + ".1", Lane.class);
						for (LinkVector lvec : outLinks) {
							Id<Link> toLink = lvec.getLink().getId();
							Lane lane = l2l.getLanes().get(laneId);
							lane.addToLinkId(toLink);
						}
					} 
				}
			}
				
			if (lanes.getLanesToLinkAssignments().containsKey(link.getId())
					&& lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().isEmpty() ) {
				// no lanes needed on this link -> delete the lane container for this link
				lanes.getLanesToLinkAssignments().remove(link.getId());
			}
		}


		LOG.info("Start pre-cleaning network before Creation of the Signal plans.");
		new NetworkCleaner().run(network);


		for (Link link : network.getLinks().values()) {
			if (lanes.getLanesToLinkAssignments().get(link.getId()) != null) {
				simplifyLanesAndAddOrigLane(link);
			}
			//Id<SignalSystem> systemId = Id.create("System" + node.id, SignalSystem.class);
			Id<SignalSystem> systemId = Id.create("System" + link.getToNode().getId(), SignalSystem.class);
			if (this.systems.getSignalSystemData().containsKey(systemId)){
				if (lanes.getLanesToLinkAssignments().containsKey(link.getId())) {
					for (Lane lane : lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().values()) {
						String end = lane.getId().toString().split("\\.")[1];
						if (!end.equals("ol")) {
							SignalData signal = this.systems.getFactory()
									.createSignalData(Id.create("Signal" + link.getId() + "." + end, Signal.class));
							signal.setLinkId(link.getId());
							signal.addLaneId(lane.getId());
							this.systems.getSignalSystemData().get(systemId).addSignalData(signal);
						}
					}
				} else {
					SignalData signal = this.systems.getFactory()
							.createSignalData(Id.create("Signal" + link.getId() + ".single", Signal.class));
					signal.setLinkId(link.getId());
					this.systems.getSignalSystemData().get(systemId).addSignalData(signal);
				}
			}
		}

        //THis counts signals with only Oneway ie. pedestrian signals
		int singSystemCounter = 0;
		for (Node node : network.getNodes().values()) {
			Id<SignalSystem> systemId = Id.create("System" + Long.valueOf(node.getId().toString()), SignalSystem.class);
			if (this.systems.getSignalSystemData().containsKey(systemId)) {
				SignalSystemData signalSystem = this.systems.getSignalSystemData().get(systemId);
				if (node.getInLinks().size() == 1) {
					if (this.makePedestrianSignals) {
						createPlansForOneWayJunction(signalSystem, node);
						LOG.info("single signal found @ " + node.getId());
						singSystemCounter++;
					} else {
						this.systems.getSignalSystemData().remove(systemId);
					}
				}

				if (node.getInLinks().size() == 2) {
					// check for pedestrian Signal in method
					createPlansforTwoWayJunction(node, signalSystem);
					if (this.systems.getSignalSystemData().containsKey(systemId))
						setInLinksCapacities(node);
				}

				if (node.getInLinks().size() == 3) {
					LinkVector thirdArm = null;
					List<LinkVector> inLinks = constructInLinkVectors(node);
					Tuple<LinkVector, LinkVector> pair = getInLinkPair(inLinks);
					for (int i = 0; i < inLinks.size(); i++) {
						if (!inLinks.get(i).equals(pair.getFirst()) && !inLinks.get(i).equals(pair.getSecond())) {
							thirdArm = inLinks.get(i);
							break;
						}
					}
					createPlansforThreeWayJunction(node, signalSystem, pair, thirdArm);
					setInLinksCapacities(node);
				}

				if (node.getInLinks().size() == 4) {
					List<LinkVector> inLinks = constructInLinkVectors(node);
					Tuple<LinkVector, LinkVector> firstPair = getInLinkPair(inLinks);
					LinkVector first = null;
					LinkVector second = null;
                    for (LinkVector inLink : inLinks) {
                        if (first == null) {
                            if (!inLink.equals(firstPair.getFirst())
                                    && !inLink.equals(firstPair.getSecond())) {
                                first = inLink;
                            }
                        } else {
                            if (!inLink.equals(firstPair.getFirst())
                                    && !inLink.equals(firstPair.getSecond())) {
                                second = inLink;
                            }
                        }

                    }
					Tuple<LinkVector, LinkVector> secondPair = new Tuple<LinkVector, LinkVector>(first, second);
					createPlansForFourWayJunction(node, signalSystem, firstPair, secondPair);
					setInLinksCapacities(node);
				}

				if (node.getInLinks().size() > 4) {
					if (this.acceptFourPlusCrossings) {
						createPlansForOneWayJunction(signalSystem, node);
						LOG.warn("Signal system with more than four in-links detected @ Node "
								+ node.getId().toString()+"\n\t\t\t -> Reader will create only ONE SignalGroup for this System");
					} else {
						throw new RuntimeException("Signal system with more than four in-links detected @ Node "
								+ node.getId().toString()+"\n\t\t\t -> Set acceptFourPlusCrossings=true to resolve");
					}
				}
			}

		}

		/*
		The following logic was added to remove bad/empty signal system as they were producing NullPointerExceptions
		 */
		Set<Id<SignalSystem>> badSignalSystemData= new HashSet<Id<SignalSystem>>();
		for (Id<SignalSystem> signalsystem : this.systems.getSignalSystemData().keySet()){
			if (this.systems.getSignalSystemData().get(signalsystem).getSignalData()==null) {
				badSignalSystemData.add(signalsystem);
			}
		}
//		LOG.warn("Found "+badSignalSystemData.size()+" incomplete or incorrect SignalSystemData -> remove them from the system...");
		for(Id<SignalSystem> badData :badSignalSystemData) {
			this.systems.getSignalSystemData().remove(badData);
		}


		// Save all signalised (MATSim) nodes and and From nodes of corresponding InLinks.
		// -> To be excluded from Network Simplifier
		for (Id<SignalSystem> systemId : this.systems.getSignalSystemData().keySet()){
			for (SignalData signaldata: this.systems.getSignalSystemData().get(systemId).getSignalData().values()){
				Node signalNode = network.getLinks().get(signaldata.getLinkId()).getToNode();


				for(Link outlink : signalNode.getOutLinks().values()){
					Long temp3 = new Long(outlink.getToNode().getId().toString());
					this.nodesNotToMerge.add(temp3);
				}
				Long temp1 = new Long(signalNode.getId().toString());
				Long temp2 = new Long(network.getLinks().get(signaldata.getLinkId()).getFromNode().getId().toString());
				this.nodesNotToMerge.add(temp1);
				this.nodesNotToMerge.add(temp2);

			}
		}

		stats();
	}

	//sbraun 30092020 Attempt to speed up this method:
    // 1. Try to parse over Long objects instead of OsmNodes in inner Loops
    // 2. Create Set with only OSMNodes in Boundingbox (to reduce size of each set)
	private void mergeOnewaySignalSystems(List<OsmNode> addingNodes, List<OsmNode> checkedNodes) {
	    //Find all nodes within BoundingBox
        Set<Long> nodesInBB = new HashSet<>();
        for (OsmNode node : this.nodes.values()) {
            if (this.bbox == null||this.bbox.contains(node.coord)) {
                nodesInBB.add(node.id);
            }
        }

		for (Long nodeID : nodesInBB) {
//				List<OsmNode> junctionNodes = new ArrayList<OsmNode>();
            List<Long> junctionNodes = new ArrayList<>();
            if (signalizedOsmNodes.contains(nodeID) && isNodeAtJunction(this.nodes.get(nodeID))
                    && !oldToMergedJunctionNodeMap.containsKey(nodeID) && hasNodeOneway(this.nodes.get(nodeID))) {
                junctionNodes.add(nodeID);
                for (Long otherNodeID : nodesInBB) {

                    if (signalizedOsmNodes.contains(otherNodeID) && isNodeAtJunction(this.nodes.get(otherNodeID))
                            && NetworkUtils.getEuclideanDistance(this.nodes.get(nodeID).coord.getX(), this.nodes.get(nodeID).coord.getY(),
                            this.nodes.get(otherNodeID).coord.getX(), this.nodes.get(otherNodeID).coord.getY()) < SIGNAL_MERGE_DISTANCE
                            && !oldToMergedJunctionNodeMap.containsKey(otherNodeID)
                            && hasNodeOneway(this.nodes.get(otherNodeID))) {
                        junctionNodes.add(otherNodeID);
                    }

                }
            }

            //TODO sbraun30092020 This is maybe redundant? Closeby nodes are already found by method above??
            if (junctionNodes.size() > 1) {
                double repXmin = 0;
                double repXmax = 0;
                double repYmin = 0;
                double repYmax = 0;
                double repX;
                double repY;
                for (Long jnID : junctionNodes) {
                    OsmNode tempNode = this.nodes.get(jnID) ;
                    if (repXmin == 0 || tempNode.coord.getX() < repXmin)
                        repXmin = tempNode.coord.getX();
                    if (repXmax == 0 || tempNode.coord.getX() > repXmax)
                        repXmax = tempNode.coord.getX();
                    if (repYmin == 0 || tempNode.coord.getY() < repYmin)
                        repYmin = tempNode.coord.getY();
                    if (repYmax == 0 || tempNode.coord.getY() > repYmax)
                        repYmax = tempNode.coord.getY();
                }
                repX = repXmin + (repXmax - repXmin) / 2;
                repY = repYmin + (repYmax - repYmin) / 2;

                BoundingBox box = new BoundingBox(repYmin, repXmin, repYmax, repXmax);
                for (Long betweenNodeID : nodesInBB) {
                    if (box.contains(this.nodes.get(betweenNodeID).coord))
                        junctionNodes.add(betweenNodeID);
                }
                OsmNode junctionNode = new OsmNode(this.id, new Coord(repX, repY));
                signalizedOsmNodes.add(junctionNode.id);
                junctionNode.used = true;
                for (Long jnID : junctionNodes) {
                    OsmNode tempNode = this.nodes.get(jnID);
                    oldToMergedJunctionNodeMap.put(jnID, junctionNode);
                    if (osmNodeRestrictions.containsKey(jnID)) {
                        osmNodeRestrictions.put(jnID, osmNodeRestrictions.get(jnID));
                    }
                    checkedNodes.add(tempNode);
                }
                addingNodes.add(junctionNode);
                id++;
            }

		}
	}

	private void setInLinksCapacities(Node node) {
		List<LinkVector> inLinks = constructInLinkVectors(node);
		for (LinkVector lvec : inLinks) {
			if (this.lanes.getLanesToLinkAssignments().containsKey(lvec.getLink().getId())) {
				Lane origLane = null;
				double olCapacity = 0;
				for (Lane lane : this.lanes.getLanesToLinkAssignments().get(lvec.getLink().getId()).getLanes()
						.values()) {
					if (!lane.getId().toString().endsWith("ol")) {
						lane.setCapacityVehiclesPerHour(SIGNAL_LANES_CAPACITY * lane.getNumberOfRepresentedLanes());
						olCapacity += lane.getCapacityVehiclesPerHour();
					} else {
						origLane = lane;
					}
				}
				origLane.setCapacityVehiclesPerHour(olCapacity);
			} else {
				lvec.getLink().setCapacity(SIGNAL_LANES_CAPACITY * lvec.getLink().getNumberOfLanes());
			}
		}
	}


	private void findCloseJunctionNodesWithSignals(OsmNode firstNode, OsmNode node, List<OsmNode> junctionNodes,
			List<OsmNode> checkedNodes, double distance, boolean getAll) {
		//Loop over all ways of given node
		for (OsmWay way : node.ways.values()) {
			String oneway = way.tags.get(TAG_ONEWAY);
			//if (oneway != null) { // && (oneway.equals("yes") || oneway.equals("true") || oneway.equals("1"))   //sbraun20200204 check why this was commented out??
			if ((oneway != null) && (oneway.equals("yes") || oneway.equals("true") || oneway.equals("1"))) { //
				//Again loop over all nodes of way
				for (int i = way.nodes.indexOf(node.id) + 1; i < way.nodes.size(); i++) {
					OsmNode otherNode = nodes.get(way.nodes.get(i));
					//Check if Node has a signal, is already checked and is in not in the list junctionNode
					if (otherNode.used && !checkedNodes.contains(otherNode) && !junctionNodes.contains(otherNode)) {
						// if smaller than 30m
                        if (NetworkUtils.getEuclideanDistance(node.coord.getX(), node.coord.getY(),
                                otherNode.coord.getX(), otherNode.coord.getY()) < distance) {
                        	if (otherNode.id == firstNode.id) {
								junctionNodes.add(otherNode);
							} else {

								junctionNodes.add(otherNode);
								findCloseJunctionNodesWithSignals(firstNode, otherNode, junctionNodes, checkedNodes,
										distance, getAll);
								//if firstNode is not a junction not forget about this again
								if (!junctionNodes.contains(firstNode)) {
									junctionNodes.remove(otherNode);
								}
							}
						}
                        //if >30m go to next node
						break;
					}
				}
			}
			if (junctionNodes.contains(firstNode) && !getAll)
				break;
		}
	}


	private void findingMoreNodeJunctions(List<OsmNode> addingNodes, List<OsmNode> checkedNodes) {
		for (OsmNode node : this.nodes.values()) {
			if (!checkedNodes.contains(node) && node.used && node.ways.size() > 1) {
				List<OsmNode> junctionNodes = new ArrayList<>();
				double distance = 40;
				findCloseJunctionNodesWithSignals(node, node, junctionNodes, checkedNodes, distance, true);

				if (junctionNodes.size() > 1) {
					double repXmin = 0;
					double repXmax = 0;
					double repYmin = 0;
					double repYmax = 0;
					double repX;
					double repY;
					boolean signalized = false;
					for (OsmNode tempNode : junctionNodes) {
						if (repXmin == 0 || tempNode.coord.getX() < repXmin)
							repXmin = tempNode.coord.getX();
						if (repXmax == 0 || tempNode.coord.getX() > repXmax)
							repXmax = tempNode.coord.getX();
						if (repYmin == 0 || tempNode.coord.getY() < repYmin)
							repYmin = tempNode.coord.getY();
						if (repYmax == 0 || tempNode.coord.getY() > repYmax)
							repYmax = tempNode.coord.getY();
						if (signalizedOsmNodes.contains(tempNode.id))
							signalized = true;
					}
					repX = repXmin + (repXmax - repXmin) / 2;
					repY = repYmin + (repYmax - repYmin) / 2;
					OsmNode junctionNode = new OsmNode(this.id, new Coord(repX, repY));
					if (signalized)
						signalizedOsmNodes.add(junctionNode.id);
					junctionNode.used = true;
					for (OsmNode tempNode : junctionNodes) {
						oldToMergedJunctionNodeMap.put(tempNode.id, junctionNode);
						if (osmNodeRestrictions.containsKey(tempNode.id)) {
							osmNodeRestrictions.put(junctionNode.id, osmNodeRestrictions.get(tempNode.id));						
						}
						checkedNodes.add(tempNode);
					}
					addingNodes.add(junctionNode);
					id++;
				}
			}
		}
	}

	private void findingTwoNodeJunctions(List<OsmNode> addingNodes, List<OsmNode> checkedNodes) {
		for (OsmNode node : this.nodes.values()) {
			if (!checkedNodes.contains(node) && node.used && isNodeAtJunction(node)) {
				boolean suit = false;
				OsmNode otherNode = null;
				boolean otherSuit = false;
				for (OsmWay way : node.ways.values()) {
					String oneway = way.tags.get(TAG_ONEWAY);
					if (oneway != null && !oneway.equals("no")) {
						suit = true;
					}
				}
				if (suit) {
					Set<OsmNode> tempNodes = new HashSet<>();
					for (OsmWay way : node.ways.values()) {
						String oneway = way.tags.get(TAG_ONEWAY);
						//sbraun 05032020 alles was kein Onewaytag hat wird übersprungen -> änder das -> evtl. die ganz Logik rausnehmen
						//-> Fixed Error in Junction System 1420523170 in Benchmark Network
						if (oneway == null || !oneway.equals("no"))
							continue;						//changed to continue
						for (int i = 0; i < way.nodes.size(); i++) {
							if (otherSuit)
								continue;					//changed to continue
							otherNode = nodes.get(way.nodes.get(i));

							boolean nodeSignalized = signalizedOsmNodes.contains(node.id);
							boolean otherNodeSignalized = signalizedOsmNodes.contains(otherNode.id);

							//Only checks one way out of node: There are sometimes Nodes in the mid of a junction; try to fix in else statement
                            if (NetworkUtils.getEuclideanDistance(node.coord.getX(), node.coord.getY(),
                                    otherNode.coord.getX(), otherNode.coord.getY()) < SIGNAL_MERGE_DISTANCE
									&& !checkedNodes.contains(otherNode) && isNodeAtJunction(otherNode)
									&& otherNode.used && !node.equals(otherNode)
									&& nodeSignalized == otherNodeSignalized) {


								for (OsmWay otherWay : otherNode.ways.values()) {
									if (!node.ways.containsKey(otherWay.id)) {
										String otherOneway = otherWay.tags.get(TAG_ONEWAY);
										if (otherOneway != null && !otherOneway.equals("no")) {
											otherSuit = true;
											break;
										}
									}
								}
								//sbraun20200218 I added this to check if this solves something
							} else if (!node.equals(otherNode)) {

								double distance = NetworkUtils.getEuclideanDistance(node.coord.getX(), node.coord.getY(),
										otherNode.coord.getX(), otherNode.coord.getY());


								OsmWay origWay = way;
								int counter = 0;
								Set<Long> passedWays = new HashSet<>();
								passedWays.add(origWay.id);
								//sbraun 05032020 changed from otherNode.ways.size()==2 and from counter < 3
								while (counter < 5 && (otherNode.ways.size()<=2 && distance < SIGNAL_MERGE_DISTANCE && !isNodeAtJunction(otherNode) )){
									//counter++;
									if (!checkedNodes.contains(otherNode)) {
										for (OsmWay tempWay : otherNode.ways.values()) {
											//if (!(origWay.id == tempWay.id)) {
											if (!passedWays.contains(tempWay.id)){
												tempNodes.add(otherNode);
												//sbraun05032020 try tempWay instead of Origway -> fix bug at junction 8
												OsmNode tempNode = nodes.get(tempWay.nodes.get(0));
												//check if there are better candiates on the same way if not take the last node
												for (Long potentialCandidate : tempWay.nodes){
													OsmNode candidate = nodes.get(potentialCandidate);
													if (isNodeAtJunction(candidate)){
														tempNode = candidate;
													}
													if (potentialCandidate ==otherNode.id) break;
												}

												//sbraun 05032020 wenn kleine Mittelwege keine Onewways sind ist die Reihenfolge manchmal anders rum
												/*if (tempNodes.contains(tempNode)){
													tempNode = nodes.get(origWay.nodes.get(origWay.nodes.size()-1));
												}*/

												distance += NetworkUtils.getEuclideanDistance(tempNode.coord.getX(), tempNode.coord.getY(),
														otherNode.coord.getX(), otherNode.coord.getY());
												//sbraun 07052020 add distance checker:
												if (distance > SIGNAL_MERGE_DISTANCE){
													break;
												}

												otherNodeSignalized = signalizedOsmNodes.contains(tempNode.id);
												otherNode = tempNode;
												passedWays.add(tempWay.id);
												origWay = tempWay;
												break;
											}
										}
										if(otherNodeSignalized==nodeSignalized && isNodeAtJunction(otherNode) && !node.equals(otherNode)){
											break;
										}
										counter++;
									}
								}

								String otherOneway = origWay.tags.get(TAG_ONEWAY);
								if (otherOneway == null || !otherOneway.equals("no")) {
									otherSuit = true;
									break;
								}


							}
						}
						if (suit && otherSuit)
							break;
					}
					if (suit && otherSuit && otherNode != null) {
						double repX = (node.coord.getX() + otherNode.coord.getX()) / 2;
						double repY = (node.coord.getY() + otherNode.coord.getY()) / 2;
						OsmNode junctionNode = new OsmNode(this.id, new Coord(repX, repY));
						if (signalizedOsmNodes.contains(node.id) || signalizedOsmNodes.contains(otherNode.id))
							signalizedOsmNodes.add(junctionNode.id);
						junctionNode.used = true;
						oldToMergedJunctionNodeMap.put(node.id, junctionNode);
						if (osmNodeRestrictions.containsKey(node.id)) {
							osmNodeRestrictions.put(junctionNode.id, osmNodeRestrictions.get(node.id));						
						}
						checkedNodes.add(node);
						oldToMergedJunctionNodeMap.put(otherNode.id, junctionNode);
						if (osmNodeRestrictions.containsKey(otherNode.id)) {
							osmNodeRestrictions.put(junctionNode.id, osmNodeRestrictions.get(otherNode.id));						
						}
						//sbraun20200218 I added this to check if this solves something

						//node.used = false;
						//otherNode.used = false;
						if (tempNodes.size()!=0) {
							for (OsmNode tempNode: tempNodes){
								oldToMergedJunctionNodeMap.put(tempNode.id, junctionNode);
								if (osmNodeRestrictions.containsKey(tempNode.id)) {
									osmNodeRestrictions.put(junctionNode.id, osmNodeRestrictions.get(tempNode.id));
								}
								checkedNodes.add(tempNode);
								//tempNode.used = false;
							}
						}


						checkedNodes.add(otherNode);
						addingNodes.add(junctionNode);
						id++;
					}
				}
			}
		}
	}


	private void findingFourNodeJunctions(List<OsmNode> addingNodes, List<OsmNode> checkedNodes) {
		for (OsmNode node : this.nodes.values()) {
			if (!checkedNodes.contains(node) && node.used && signalizedOsmNodes.contains(node.id)
					&& node.ways.size() > 1) {
				List<OsmNode> junctionNodes = new ArrayList<>();

				findCloseJunctionNodesWithSignals(node, node, junctionNodes, checkedNodes, MERGE_DISTANCE_JUNCTIONS, false);

				if (junctionNodes.size() == 4) {
					double repX = 0;
					double repY = 0;
					for (OsmNode tempNode : junctionNodes) {
						repX += tempNode.coord.getX();
						repY += tempNode.coord.getY();
					}
					repX /= junctionNodes.size();
					repY /= junctionNodes.size();
					OsmNode junctionNode = new OsmNode(this.id, new Coord(repX, repY));
					signalizedOsmNodes.add(junctionNode.id);
					junctionNode.used = true;
					for (OsmNode tempNode : junctionNodes) {
						oldToMergedJunctionNodeMap.put(tempNode.id, junctionNode);
						if (osmNodeRestrictions.containsKey(tempNode.id)) {
							osmNodeRestrictions.put(junctionNode.id, osmNodeRestrictions.get(tempNode.id));						
						}
						checkedNodes.add(tempNode);
					}
					addingNodes.add(junctionNode);
					id++;
				}
			}
		}
	}

//	private void pushingSignalsIntoRoundabouts() {
	private void removeSignalsAtDeadEnds() {
		for (OsmWay way : this.ways.values()) {
			/* I think, signals infront of roundabouts should be kept. theresa,jan'17*/
//			String oneway = way.tags.get(TAG_ONEWAY);
//			if (oneway != null && !oneway.equals("-1")) {
//				OsmNode signalNode = null;
//				for (int i = 0; i < way.nodes.size(); i++) {
//					signalNode = this.nodes.get(way.nodes.get(i));
//					if (signalizedOsmNodes.contains(signalNode.id) && !isNodeAtJunction(signalNode)
//							&& isInfrontOfRoundabout(signalNode, way, i))
//						signalizedOsmNodes.remove(signalNode.id);
//				}
//			}
			OsmNode node = this.nodes.get(way.nodes.get(0));
			if (node.endPoint && node.ways.size() == 1) {
				signalizedOsmNodes.remove(node.id);
			}
			node = this.nodes.get(way.nodes.get(way.nodes.size() - 1));
			if (node.endPoint && node.ways.size() == 1) {
				signalizedOsmNodes.remove(node.id);
			}
		}
	}

	private void pushSignalsOverShortWays() {
		//sbraun 19032020 added this for the situation that there is a signal between two short ways.
//		for (OsmNode node: this.nodes.values()){
//			if (signalizedOsmNodes.contains(node.id) && !makePedestrianSignals && node.ways.size()==2){
//				List<OsmWay> shortWays = new LinkedList();
//				for (OsmWay way : node.ways.values()){
//					if (way.nodes.size()==2) shortWays.add(way);
//				}
//				if (shortWays.size()==1){
//					OsmWay way = shortWays.get(0);
//					pushSignalOverShortWay(way, this.nodes.get(way.nodes.get(0)), this.nodes.get(way.nodes.get(1)));
//					pushSignalOverShortWay(way, this.nodes.get(way.nodes.get(1)), this.nodes.get(way.nodes.get(0)));
//				}
//				//in between two short ways puxh signal to the more important junction
//				if (shortWays.size()==2){
//
//				}
//			}
//		}
		//sbraun20200305 new logic -> irgendwie nicht schoen und code Wiederholung von #pushIntoNearbyJunction
		for (Long id : nodes.keySet()){
			if (!signalizedOsmNodes.contains(id)) continue;

			HashSet<Long> shortWays = new HashSet<>();

			for (OsmWay way : nodes.get(id).ways.values()){
				if(way.nodes.size()==2) {
					shortWays.add(way.id);
				}
			}

			if (shortWays.size()== 1){

				OsmWay way = ways.get(shortWays.iterator().next());
				pushSignalOverShortWay(way, this.nodes.get(way.nodes.get(0)), this.nodes.get(way.nodes.get(1)));
				pushSignalOverShortWay(way, this.nodes.get(way.nodes.get(1)), this.nodes.get(way.nodes.get(0)));
			}

			if (shortWays.size() > 1){
				OsmWay bestWay = null;
				int hierarchyBestWay = 999;
				int hierarchCounterBW = 0;

				for (long wayId : shortWays){
					OsmWay tempWay = ways.get(wayId);
					OsmNode tempNode = null;


					int hierarchy = 999;
					int hierarchCounter = 0;

					for (Long node : tempWay.nodes){
						if (!node.equals(id)){
							tempNode = nodes.get(node);
							break;
						}
					}

					for( OsmWay wayOfTempNode :tempNode.ways.values()){

						if (hierarchy > wayOfTempNode.hierarchy){
							hierarchy = wayOfTempNode.hierarchy;
							hierarchCounter = 1;
						}
						if (hierarchy == wayOfTempNode.hierarchy) hierarchCounter += 1;

					}



					if (hierarchy < hierarchyBestWay){
						bestWay = tempWay;
						hierarchyBestWay = hierarchy;
						hierarchCounterBW =  hierarchCounter;
					} else {
						if (hierarchy == hierarchyBestWay &&hierarchCounter>hierarchCounterBW){
							bestWay = tempWay;
							hierarchyBestWay = hierarchy;
							hierarchCounterBW =  hierarchCounter;
						}
					}

				}

				pushSignalOverShortWay(bestWay, this.nodes.get(bestWay.nodes.get(0)), this.nodes.get(bestWay.nodes.get(1)));
				pushSignalOverShortWay(bestWay, this.nodes.get(bestWay.nodes.get(1)), this.nodes.get(bestWay.nodes.get(0)));

			}


		}





		//sbraun 19032020: old
		for (OsmWay way : this.ways.values()) {
			if(way.nodes.size()==2) {
                pushSignalOverShortWay(way, this.nodes.get(way.nodes.get(0)), this.nodes.get(way.nodes.get(1)));
                pushSignalOverShortWay(way, this.nodes.get(way.nodes.get(1)), this.nodes.get(way.nodes.get(0)));
            }
		}
	}


	private void pushSignalOverShortWay(OsmWay shortWay, OsmNode fromNode, OsmNode toNode) {
		if (NetworkUtils.getEuclideanDistance(fromNode.coord.getX(), fromNode.coord.getY(),
                toNode.coord.getX(), toNode.coord.getY()) < SIGNAL_MERGE_DISTANCE) {
			if (fromNode.ways.size() == 2 && toNode.ways.size() > 2
					&& signalizedOsmNodes.contains(fromNode.id)) {
				signalizedOsmNodes.remove(fromNode.id);
				signalizedOsmNodes.add(toNode.id);
				LOG.info("signal pushed over short way @ Node " + toNode.id);
			}
		}
	}


	private void pushingSingnalsIntoEndpoints() {
		for (OsmWay way : this.ways.values()) {
			for (int i = 1; i < way.nodes.size() - 1; i++) {
				OsmNode signalNode = this.nodes.get(way.nodes.get(i));
				OsmNode endPoint;
				String oneWay = way.tags.get(TAG_ONEWAY);

				if (signalizedOsmNodes.contains(signalNode.id) && !isNodeAtJunction(signalNode)) {

					if ((oneWay != null && !oneWay.equals("-1") && !oneWay.equals("no")) || oneWay == null) {
						endPoint = this.nodes.get(way.nodes.get(way.nodes.size() - 1));
                        if (signalizedOsmNodes.contains(endPoint.id) && isNodeAtJunction(endPoint)
								&& NetworkUtils.getEuclideanDistance(signalNode.coord.getX(), signalNode.coord.getY(),
                                endPoint.coord.getX(), endPoint.coord.getY()) < SIGNAL_MERGE_DISTANCE)
							signalizedOsmNodes.remove(signalNode.id);
					}
					//sbraun20200128: Oneway Street reversed - need to keep that
					if ((oneWay != null && !oneWay.equals("yes") && !oneWay.equals("true") && !oneWay.equals("1")
							&& !oneWay.equals("no")) || oneWay == null) {
						endPoint = this.nodes.get(way.nodes.get(0));
                        if (signalizedOsmNodes.contains(endPoint.id) && isNodeAtJunction(endPoint)
								&& NetworkUtils.getEuclideanDistance(signalNode.coord.getX(), signalNode.coord.getY(),
                                endPoint.coord.getX(), endPoint.coord.getY()) < SIGNAL_MERGE_DISTANCE)
							signalizedOsmNodes.remove(signalNode.id);
					}
				}
			}
		}
	}

	// TODO this method already moves signals over crossings to endpoints of ways. do we still need pushingSingnalsIntoEndpoints? 
	// or, if we need it because it does additional stuff, can we remove the case for crossings/endpoints here in this method??
	private void pushSignalsIntoNearbyJunctions() {
		for (OsmWay way : this.ways.values()) {
			// go through all nodes, except the first and the last
			//sbraun 19032020
			for (int i = 1; i < way.nodes.size() - 1; i++) {
				OsmNode signalNode = this.nodes.get(way.nodes.get(i));
				OsmNode junctionNode = null;
				String oneway = way.tags.get(TAG_ONEWAY);
				if (signalizedOsmNodes.contains(signalNode.id) && !isNodeAtJunction(signalNode)) {
                    //sbraun 05032020 added oneway.equals("no"), doesnt really make sense but works...
					if ((oneway != null && !oneway.equals("-1"))|| oneway == null || oneway.equals("no")) {
						// positive oneway or no oneway
						//TODO 20200124 sbraun: loops over all nodes of this way to find the closest junction. The problem seems
						//that once he found that node he continue to loop over the nodes of the way, so once junction is found it continues to look

                        double distancePos = 0;
						double distanceNeg = 0;

						OsmNode tempNode = signalNode;
                        OsmNode nextNode = signalNode;
						OsmNode junctionNodePosDir = null;
						OsmNode	junctionNodeNegDir = null;

						//TODO this checks just one way -> put this all in a seperate method???
						//check positive direction
                        for (int j = i; j< way.nodes.size()-1; j++){
                            nextNode = this.nodes.get(way.nodes.get(j + 1));
                            distancePos += NetworkUtils.getEuclideanDistance(tempNode.coord.getX(), tempNode.coord.getY(),
                                    nextNode.coord.getX(), nextNode.coord.getY());

                            if (isNodeAtJunction(nextNode)){
								junctionNodePosDir = nextNode;
                                break;
                            }
                            if (distancePos >= SIGNAL_MERGE_DISTANCE) break;
                            tempNode = nextNode;
                        }
                        //negative direction
						if (i!=0) {
							for (int j = i; j > 0; j--) {
								nextNode = this.nodes.get(way.nodes.get(j - 1));
								distanceNeg += NetworkUtils.getEuclideanDistance(tempNode.coord.getX(), tempNode.coord.getY(),
										nextNode.coord.getX(), nextNode.coord.getY());

								if (isNodeAtJunction(nextNode)) {
									junctionNodeNegDir = nextNode;
									break;
								}
								if (distanceNeg >= SIGNAL_MERGE_DISTANCE) break;
								tempNode = nextNode;
							}
						}

						if (junctionNodePosDir!=null && junctionNodeNegDir!=null){
							//logic to decide which junction should be prioritised
							int topLayerPos = -1;
							int topLayerNeg = -1;
							int topLayerCounterPos = 0;
							int topLayerCounterNeg = 0;


							//Check the toplevel hierarchy (best is 1) and count how many ways are of that level
							for (OsmWay wayJunPos :junctionNodePosDir.ways.values()){
								if (wayJunPos.hierarchy< topLayerPos &&  wayJunPos.hierarchy!=-1){
									topLayerPos = wayJunPos.hierarchy;
									if (topLayerCounterPos != 0)  topLayerCounterPos = 0;
								}
								if (wayJunPos.hierarchy == topLayerPos &&  wayJunPos.hierarchy!=-1) topLayerCounterPos++;
							}
							for (OsmWay wayJunNeg :junctionNodeNegDir.ways.values()){
								if (wayJunNeg.hierarchy< topLayerNeg &&  wayJunNeg.hierarchy!=-1){
									topLayerNeg = wayJunNeg.hierarchy;
									if (topLayerCounterNeg != 0)  topLayerCounterNeg = 0;
								}
								if (wayJunNeg.hierarchy == topLayerNeg &&  wayJunNeg.hierarchy!=-1) topLayerCounterNeg++;
							}
							//sbraun 19032020
							//rather complicated but it compares firstly the hierarchies of the ways the junction node is on
							//if that is equal than it has a count of the number of ways in the same hierarchy
							//if that is equal it takes the one junction with more ways
							//sbraun 30.04.20 check closest Junction


							if(Math.abs(distanceNeg-distancePos)>SIGNAL_MERGE_DISTANCE*0.2){
								if (distancePos<distanceNeg){
									junctionNode = junctionNodePosDir;
								}else{
									junctionNode = junctionNodeNegDir;
								}

							} else {
								if (topLayerPos == -1 || topLayerNeg == -1) {
									if (topLayerPos == -1 && topLayerNeg == -1) {
										//TODO use number of lanes instead
										if (junctionNodePosDir.ways.size() >= junctionNodeNegDir.ways.size()) {
											junctionNode = junctionNodePosDir;
										} else junctionNode = junctionNodeNegDir;
									} else {
										if (topLayerPos == -1) {
											junctionNode = junctionNodeNegDir;
										} else junctionNode = junctionNodePosDir;
									}
								} else {
									if (topLayerPos == topLayerNeg) {
										if (topLayerCounterPos == topLayerCounterNeg) {
											//same hierarchy and same number of ways on that node with the same hierarchy
											if (junctionNodePosDir.ways.size() >= junctionNodeNegDir.ways.size()) {
												junctionNode = junctionNodePosDir;
											} else junctionNode = junctionNodeNegDir;
										} else {
											if (topLayerCounterPos > topLayerCounterNeg) {
												junctionNode = junctionNodePosDir;
											} else junctionNode = junctionNodeNegDir;
										}

									} else {
										if (topLayerPos < topLayerNeg) {
											junctionNode = junctionNodePosDir;
										} else junctionNode = junctionNodeNegDir;
									}
								}
							}
						} else{
							if (junctionNodePosDir != null) {
								junctionNode = junctionNodePosDir;
							} else junctionNode = junctionNodeNegDir;
						}

                        /*
						OsmNode nextNode = this.nodes.get(way.nodes.get(i + 1));
						if (nextNode.ways.size() > 1) {
							// either a junction or the end point of this way where a new way start
							junctionNode = nextNode;
						}*/

						//TODO sbraun20200130 This is only used once in Cottbus and it will only rest above junctionNode to Null again.
						if (i < way.nodes.size() - 2) {
							OsmNode secondNextNode = this.nodes.get(way.nodes.get(i + 2));
							if (crossingOsmNodes.contains(nextNode.id) && secondNextNode.ways.size() > 1) {
								// next node = pedestrian crossing. and the second next node is either a junction or the end of the way
								junctionNode = this.nodes.get(way.nodes.get(i + 2));
							}
						}
					}
                    if (junctionNode != null && NetworkUtils.getEuclideanDistance(signalNode.coord.getX(), signalNode.coord.getY(),
                            junctionNode.coord.getX(), junctionNode.coord.getY()) < SIGNAL_MERGE_DISTANCE) {
						signalizedOsmNodes.remove(signalNode.id);
						signalizedOsmNodes.add(junctionNode.id);

						//TODO sbraun20200128: This could have been checked further up, would be much clearer -> dont have code above double in this method
					} else if (((oneway != null) && oneway.equals("-1")) || (oneway == null) || oneway.equals("no")){
						// "else" = no nearby junction found in positive direction. try reverse direction:
						// "if" = reverse oneway or no oneway (check opposite direction)
						OsmNode prevNode = this.nodes.get(way.nodes.get(i - 1));
						if (prevNode.ways.size() > 1) {
							// either a junction or the start point of this way where another way ends
							junctionNode = prevNode;
						}

                        if (junctionNode != null && NetworkUtils.getEuclideanDistance(signalNode.coord.getX(), signalNode.coord.getY(),
                                junctionNode.coord.getX(), junctionNode.coord.getY()) < SIGNAL_MERGE_DISTANCE) {
							signalizedOsmNodes.remove(signalNode.id);
							signalizedOsmNodes.add(junctionNode.id);
						}
					}
				}
			}
		}
	}


	private void createPlansForFourWayJunction(Node node, SignalSystemData signalSystem,
			Tuple<LinkVector, LinkVector> firstPair, Tuple<LinkVector, LinkVector> secondPair) {
		int groupNumber = 1;
		int cycle = CYCLE_TIME;
		double lanesFirst = (firstPair.getFirst().getLink().getNumberOfLanes()
				+ firstPair.getSecond().getLink().getNumberOfLanes()) / 2;
		double lanesSecond = (secondPair.getFirst().getLink().getNumberOfLanes()
				+ secondPair.getSecond().getLink().getNumberOfLanes()) / 2;
		int changeTime = (int) ((lanesFirst) / (lanesFirst + lanesSecond) * cycle);
		if (changeTime < this.minimalTimeForPair)
			changeTime = this.minimalTimeForPair;
		if (changeTime > cycle - this.minimalTimeForPair)
			changeTime = cycle - this.minimalTimeForPair;

		List<Lane> criticalSignalLanesFirst = new ArrayList<Lane>();
		findTwoPhaseSignalLanes(firstPair, criticalSignalLanesFirst);

		List<Lane> criticalSignalLanesSecond = new ArrayList<Lane>();
		findTwoPhaseSignalLanes(secondPair, criticalSignalLanesSecond);

		SignalSystemControllerData controller = createController(signalSystem);
		SignalPlanData plan = createPlan(node, cycle);
		controller.addSignalPlanData(plan);

		if (!criticalSignalLanesFirst.isEmpty()) {
			createTwoPhase(groupNumber, signalSystem, criticalSignalLanesFirst, firstPair, plan, changeTime, cycle,
					node, true);
			groupNumber += 2;
		} else {
			createOnePhase(groupNumber, signalSystem, firstPair, plan, changeTime, cycle, node, true);
			groupNumber++;
		}

		if (!criticalSignalLanesSecond.isEmpty()) {
			createTwoPhase(groupNumber, signalSystem, criticalSignalLanesSecond, secondPair, plan, changeTime, cycle,
					node, false);
		} else {
			createOnePhase(groupNumber, signalSystem, secondPair, plan, changeTime, cycle, node, false);
		}
	}

	private void createPlansforThreeWayJunction(Node node, SignalSystemData signalSystem,
			Tuple<LinkVector, LinkVector> pair, LinkVector thirdArm) {
		int groupNumber = 1;
		int cycle = CYCLE_TIME;
		double lanesPair = (pair.getFirst().getLink().getNumberOfLanes()
				+ pair.getSecond().getLink().getNumberOfLanes()) / 2;
		int changeTime = (int) ((lanesPair) / (lanesPair + thirdArm.getLink().getNumberOfLanes()) * cycle);
		if (changeTime < this.minimalTimeForPair)
			changeTime = this.minimalTimeForPair;
		if (changeTime > cycle - this.minimalTimeForPair)
			changeTime = cycle - this.minimalTimeForPair;
		boolean firstIsCritical = false;
		List<Lane> criticalSignalLanes = new ArrayList<Lane>();
		if (pair.getFirst().getRotationToOtherInLink(thirdArm) > Math.PI)
			firstIsCritical = true;
		if (firstIsCritical && lanes.getLanesToLinkAssignments().containsKey(pair.getFirst().getLink().getId())) {
			for (Lane lane : lanes.getLanesToLinkAssignments().get(pair.getFirst().getLink().getId()).getLanes()
					.values()) {
				if (lane.getAlignment() == 2)
					criticalSignalLanes.add(lane);
			}
		} else if (lanes.getLanesToLinkAssignments().containsKey(pair.getSecond().getLink().getId())) {
			for (Lane lane : lanes.getLanesToLinkAssignments().get(pair.getSecond().getLink().getId()).getLanes()
					.values()) {
				if (lane.getAlignment() == 2)
					criticalSignalLanes.add(lane);
			}
		}

		SignalSystemControllerData controller = createController(signalSystem);
		SignalPlanData plan = createPlan(node, cycle);
		controller.addSignalPlanData(plan);
		if (!criticalSignalLanes.isEmpty()) {
			createTwoPhase(groupNumber, signalSystem, criticalSignalLanes, pair, plan, changeTime, cycle, node, true);
			groupNumber += 2;
		} else {
			createOnePhase(groupNumber, signalSystem, pair, plan, changeTime, cycle, node, true);
			groupNumber++;
		}
		Tuple<LinkVector, LinkVector> phantomPair = new Tuple<LinkVector, LinkVector>(thirdArm, null);
		createOnePhase(groupNumber, signalSystem, phantomPair, plan, changeTime, cycle, node, false);
	}

	private void createPlansforTwoWayJunction(Node node, SignalSystemData signalSystem) {
		List<LinkVector> inLinks = constructInLinkVectors(node);
		double inLinksAngle = inLinks.get(0).getRotationToOtherInLink(inLinks.get(1));
		int cycle = CYCLE_TIME;
		//sbraun
		if (inLinksAngle > (3. / 4. * Math.PI) && inLinksAngle < (5. / 4. * Math.PI)) {
			if (!this.makePedestrianSignals) {
				this.systems.getSignalSystemData().remove(signalSystem.getId());
				LOG.info("Remove Signal-"+signalSystem.getId().toString()+" as the InLinks angle indicates that this is is Pedestrian Crossing.");
				return;
			} else {
				SignalGroupData group = this.groups.getFactory().createSignalGroupData(signalSystem.getId(),
						Id.create("PedestrianSignal." + node.getId(), SignalGroup.class));
				for (SignalData signal : signalSystem.getSignalData().values()) {
					group.addSignalId(signal.getId());
				}
				SignalSystemControllerData controller = createController(signalSystem);
				SignalPlanData plan = createPlan(node, cycle);
				controller.addSignalPlanData(plan);
				SignalGroupSettingsData settings = createSetting(0, cycle - PEDESTRIAN_CROSSING_TIME + INTERGREENTIME,
						node, group.getId());
				plan.addSignalGroupSettings(settings);
				groups.addSignalGroupData(group);
			}
		} else {
			SignalGroupData groupOne = createSignalGroup(1, signalSystem, node);
			SignalSystemControllerData controller = createController(signalSystem);
			SignalPlanData plan = createPlan(node, cycle);
			controller.addSignalPlanData(plan);
			for (SignalData signal : signalSystem.getSignalData().values()) {
				if (signal.getLinkId().equals(inLinks.get(0).getLink().getId()))
					groupOne.addSignalId(signal.getId());
			}
			SignalGroupSettingsData settingsFirst = createSetting(0, 45 - INTERGREENTIME, node, groupOne.getId());
			plan.addSignalGroupSettings(settingsFirst);
			groups.addSignalGroupData(groupOne);

			SignalGroupData groupTwo = createSignalGroup(2, signalSystem, node);
			for (SignalData signal : signalSystem.getSignalData().values()) {
				if (signal.getLinkId().equals(inLinks.get(1).getLink().getId()))
					groupTwo.addSignalId(signal.getId());
			}

			controller.addSignalPlanData(plan);
			SignalGroupSettingsData settingsSecond = createSetting(45, 90 - INTERGREENTIME, node, groupTwo.getId());
			plan.addSignalGroupSettings(settingsSecond);
			groups.addSignalGroupData(groupTwo);
		}
	}

	private void createTwoPhase(int groupNumber, SignalSystemData signalSystem, List<Lane> criticalSignalLanes,
			Tuple<LinkVector, LinkVector> pair, SignalPlanData plan, int changeTime, int cycle, Node node,
			boolean first) {
		SignalGroupData groupOne = createSignalGroup(groupNumber, signalSystem, node);
		for (SignalData signal : signalSystem.getSignalData().values()) {
			if (signal.getLinkId().equals(pair.getFirst().getLink().getId())
					|| signal.getLinkId().equals(pair.getSecond().getLink().getId())) {
				boolean firstPhase = true;
				for (int i = 0; i < criticalSignalLanes.size(); i++) {
					if (signal.getLaneIds() != null && signal.getLaneIds().contains(criticalSignalLanes.get(i).getId()))
						firstPhase = false;
				}
				if (firstPhase)
					groupOne.addSignalId(signal.getId());
			}
		}
		fillConflictingLanesData(pair, criticalSignalLanes);
		SignalGroupSettingsData settingsFirst;
		if (first)
			settingsFirst = createSetting(0, changeTime - (2 * INTERGREENTIME + MIN_GREENTIME), node, groupOne.getId());
		else
			settingsFirst = createSetting(changeTime, cycle - (2 * INTERGREENTIME + MIN_GREENTIME), node,
					groupOne.getId());
		plan.addSignalGroupSettings(settingsFirst);
		groups.addSignalGroupData(groupOne);
		groupNumber++;

		SignalGroupData groupTwo = createSignalGroup(groupNumber, signalSystem, node);
		for (SignalData signal : signalSystem.getSignalData().values()) {
			if (signal.getLinkId().equals(pair.getFirst().getLink().getId())
					|| signal.getLinkId().equals(pair.getSecond().getLink().getId())) {
				for (int i = 0; i < criticalSignalLanes.size(); i++) {
					if (signal.getLaneIds() != null && signal.getLaneIds().contains(criticalSignalLanes.get(i).getId()))
						groupTwo.addSignalId(signal.getId());
				}
			}
		}
		SignalGroupSettingsData settingsSecond = null;
		if (first)
			settingsSecond = createSetting(changeTime - (INTERGREENTIME + MIN_GREENTIME), changeTime - INTERGREENTIME,
					node, groupTwo.getId());
		else
			settingsSecond = createSetting(cycle - (INTERGREENTIME + MIN_GREENTIME), cycle - INTERGREENTIME, node,
					groupTwo.getId());
		plan.addSignalGroupSettings(settingsSecond);
		groups.addSignalGroupData(groupTwo);
		groupNumber++;

	}

	private void fillConflictingLanesData(Tuple<LinkVector, LinkVector> pair, List<Lane> criticalSignalLanes) {
		Link firstLink = pair.getFirst().getLink();
		Link secondLink = null;
		if (pair.getSecond() != null)
			secondLink = pair.getSecond().getLink();
		setConflictingAndNonConflictingLanesToLanes(firstLink, secondLink, criticalSignalLanes);
		setConflictingAndNonConflictingLanesToLanes(secondLink, firstLink, criticalSignalLanes);
	}

	private void setConflictingAndNonConflictingLanesToLanes(Link firstLink, Link secondLink,
			List<Lane> criticalSignalLanes) {
		if (firstLink == null || secondLink == null)
			return;
		
		LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(firstLink.getId());
		LanesToLinkAssignment otherl2l = lanes.getLanesToLinkAssignments().get(secondLink.getId());
		List<Lane> firstLanes = new ArrayList<Lane>();
		List<Lane> secondLanes = new ArrayList<>();
		if (l2l != null) {
			for (Lane lane : l2l.getLanes().values()) {
				if (!lane.getId().toString().endsWith("ol")) {
                    firstLanes.add(lane);
                }
			}
		}
		if (otherl2l != null) {
			for (Lane lane : otherl2l.getLanes().values()) {
				if (!lane.getId().toString().endsWith("ol"))
					secondLanes.add(lane);
			}
		}

		for (Lane lane : firstLanes) {
			List<Id<Lane>> nonCritLanes = new ArrayList<Id<Lane>>();
			List<Id<Lane>> critLanes = new ArrayList<Id<Lane>>();
			for (Lane otherLane : firstLanes) {
				if (!otherLane.equals(lane))
					nonCritLanes.add(otherLane.getId());
			}
			for (Lane otherLane : secondLanes) {
				if (criticalSignalLanes != null && criticalSignalLanes.contains(otherLane)) 
					critLanes.add(otherLane.getId());
				else
					nonCritLanes.add(otherLane.getId());
			}
			this.nonCritLanes.put(lane.getId(), nonCritLanes);
			int i = 1;
			for (Id<Lane> laneId : nonCritLanes) {
				//lane.getAttributes().putAttribute(NON_CRIT_LANES + "_" + i, laneId.toString());
				i++;
			}
			if (!critLanes.isEmpty()) {
				i = 1;
				this.critLanes.put(lane.getId(), critLanes);
				for (Id<Lane> laneId : critLanes) {
					//lane.getAttributes().putAttribute(CRIT_LANES + "_" + i, laneId.toString());
					i++;
				}
			}
		}
	}

	private void createOnePhase(int groupNumber, SignalSystemData signalSystem, Tuple<LinkVector, LinkVector> pair,
			SignalPlanData plan, int changeTime, int cycle, Node node, boolean first) {
		SignalGroupData group = createSignalGroup(groupNumber, signalSystem, node);
		Id<Link> firstLinkId = pair.getFirst().getLink().getId();
		Id<Link> secondLinkId = null;
		if (pair.getSecond() != null)
			secondLinkId = pair.getSecond().getLink().getId();
		for (SignalData signal : signalSystem.getSignalData().values()) {
			if (signal.getLinkId().equals(firstLinkId) || signal.getLinkId().equals(secondLinkId)) {
				group.addSignalId(signal.getId());
			}
		}
		fillConflictingLanesData(pair, null);
		SignalGroupSettingsData settings = null;
		if (first)
			settings = createSetting(0, changeTime - INTERGREENTIME, node, group.getId());
		else
			settings = createSetting(changeTime, cycle - INTERGREENTIME, node, group.getId());
		plan.addSignalGroupSettings(settings);
		groups.addSignalGroupData(group);
		groupNumber++;

	}

	private Tuple<LinkVector, LinkVector> getInLinkPair(List<LinkVector> inLinks) {
		LinkVector first = inLinks.get(0);
		LinkVector second = inLinks.get(1);
		double diff = Math.abs(first.getRotationToOtherInLink(second) - Math.PI);
		double otherDiff;
		for (int i = 0; i < inLinks.size() - 1; i++) {
			for (int j = i + 1; j < inLinks.size(); j++) {
				otherDiff = Math.abs(inLinks.get(i).getRotationToOtherInLink(inLinks.get(j)) - Math.PI);
				if (otherDiff < diff) {
					first = inLinks.get(i);
					second = inLinks.get(j);
					diff = otherDiff;
				}
			}
		}
		Tuple<LinkVector, LinkVector> pair = new Tuple<LinkVector, LinkVector>(first, second);
		return pair;
	}

	private SignalGroupSettingsData createSetting(int onset, int dropping, Node node, Id<SignalGroup> id) {
		SignalGroupSettingsData settings = control.getFactory().createSignalGroupSettingsData(id);
		settings.setOnset(onset);
		settings.setDropping(dropping);
		return settings;
	}

	private SignalPlanData createPlan(Node node, int cycle) {
		SignalPlanData plan = this.control.getFactory().createSignalPlanData(Id.create(node.getId(), SignalPlan.class));
		plan.setStartTime(0.0);
		plan.setEndTime(0.0);
		plan.setCycleTime(cycle);
		plan.setOffset(0);
		return plan;
	}

	private SignalSystemControllerData createController(SignalSystemData signalSystem) {
		SignalSystemControllerData controller = this.control.getFactory()
				.createSignalSystemControllerData(signalSystem.getId());
		this.control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		return controller;
	}

	private SignalGroupData createSignalGroup(int groupNumber, SignalSystemData signalSystem, Node node) {
		SignalGroupData group = this.groups.getFactory().createSignalGroupData(signalSystem.getId(),
				Id.create("SignalGroup" + node.getId() + "." + groupNumber, SignalGroup.class));
		return group;
	}

	private void createPlansForOneWayJunction(SignalSystemData signalSystem, Node node) {
		int cycle = CYCLE_TIME;
		int changeTime = CYCLE_TIME - PEDESTRIAN_CROSSING_TIME;
		SignalGroupData group = createSignalGroup(1, signalSystem, node);

		for (SignalData signal : signalSystem.getSignalData().values())
			group.addSignalId(signal.getId());

		SignalSystemControllerData controller = createController(signalSystem);
		SignalPlanData plan = createPlan(node, cycle);
		controller.addSignalPlanData(plan);
		SignalGroupSettingsData settings = null;
		settings = createSetting(0, changeTime - INTERGREENTIME, node, group.getId());

		plan.addSignalGroupSettings(settings);
		groups.addSignalGroupData(group);

	}

	private void findTwoPhaseSignalLanes(Tuple<LinkVector, LinkVector> pair, List<Lane> criticalSignalLanes) {
		if (lanes.getLanesToLinkAssignments().containsKey(pair.getFirst().getLink().getId())) {
			for (Lane lane : lanes.getLanesToLinkAssignments().get(pair.getFirst().getLink().getId()).getLanes()
					.values()) {
				if (lane.getAlignment() == 2)
					criticalSignalLanes.add(lane);
			}
		}
		if (lanes.getLanesToLinkAssignments().containsKey(pair.getSecond().getLink().getId())) {
			for (Lane lane : lanes.getLanesToLinkAssignments().get(pair.getSecond().getLink().getId()).getLanes()
					.values()) {
				if (lane.getAlignment() == 2)
					criticalSignalLanes.add(lane);
			}
		}
	}

	// private List<OsmNode> findCloseJunctionNodesWithout(OsmNode node,
	// List<OsmNode> junctionNodes) {
	// for (OsmWay way : node.ways.values()) {
	// for (int i = 0; i < way.nodes.size(); i++) {
	// OsmNode otherNode = nodes.get(way.nodes.get(i));
	// if (otherNode.used && !otherNode.signalized) {
	// if (node.getDistance(otherNode) < 30) {
	// if(!junctionNodes.contains(otherNode)){
	// junctionNodes.add(otherNode);
	// junctionNodes = findCloseJunctionNodesWithSignals(otherNode, junctionNodes);
	// break;
	// }
	// }
	// }
	// }
	// }
	// return junctionNodes;
	// }

	// trying to create lanes while creating a Link - toLinks can only be set
	// after all Links are created
	// idea: creating empty lanes with links -> filling after all links are
	// created - useful?************
	// **************************************************************************************************
	private void createLanes(final Link l, final Lanes lanes, final double nofLanes) {
		OsmHighwayDefaults defaults = this.highwayDefaults.get(l.getAttributes().getAttribute(TYPE).toString());
		LanesFactory factory = lanes.getFactory();
		LanesToLinkAssignment lanesForLink = factory.createLanesToLinkAssignment(Id.create(l.getId(), Link.class));
		lanes.addLanesToLinkAssignment(lanesForLink);
		// Lane origLane = lanes.getFactory().createLane(Id.create("Lane" + id + ".ol",
		// Lane.class));
		// origLane.setStartsAtMeterFromLinkEnd(l.getLength());
		// origLane.setCapacityVehiclesPerHour(0);
		// lanesForLink.addLane(origLane);
		for (int i = 1; i <= nofLanes; i++) {
			Lane lane = lanes.getFactory().createLane(Id.create("Lane" + l.getId() + "." + i, Lane.class));
			if (l.getLength() > DEFAULT_LANE_OFFSET) {
				lane.setStartsAtMeterFromLinkEnd(DEFAULT_LANE_OFFSET);
			} else {
				lane.setStartsAtMeterFromLinkEnd(l.getLength() - 1);
			}
			lane.setCapacityVehiclesPerHour(defaults.laneCapacity);
			lanesForLink.addLane(lane);
		}
	}

	private void simplifyLanesAndAddOrigLane(Link link) {
		// create 'original' lane, i.e. first lane of the link
		Lane origLane = lanes.getFactory().createLane(Id.create("Lane" + link.getId() + ".ol", Lane.class));
		origLane.setStartsAtMeterFromLinkEnd(link.getLength());
		origLane.setNumberOfRepresentedLanes(link.getNumberOfLanes());
		// note: lane capacities are set later 
		// merge duplicated lanes (lanes with same to-links)
		Set<Id<Lane>> lanesToBeRemoved = new HashSet<>();
		for (int indexLane1 = 1; indexLane1 <= lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().size(); indexLane1++) {
			Lane lane1 = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().get(Id.create("Lane" + link.getId() + "." + indexLane1, Lane.class));
			// only consider lanes that are not already marked as to be removed
			if (!lanesToBeRemoved.contains(lane1.getId())) {
				origLane.addToLaneId(lane1.getId());
				// check for other lanes with same outgoing links
				mergeCheck: 
				for (int indexLane2 = indexLane1+1; indexLane2 <= lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().size(); indexLane2++) {
					Lane lane2 = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().get(Id.create("Lane" + link.getId() + "." + indexLane2, Lane.class));

					if (lane1.getToLinkIds().size() == lane2.getToLinkIds().size()) {
						for (int i = 0; i < lane1.getToLinkIds().size(); i++) {
							if (!lane1.getToLinkIds().get(i).equals(lane2.getToLinkIds().get(i))) {
								break mergeCheck; // the lanes have not the same to-links
							}
						}
						// lane2 has the same outgoing links as lane1. save it as to be merged
						lanesToBeRemoved.add(lane2.getId());
						lane1.setNumberOfRepresentedLanes(lane1.getNumberOfRepresentedLanes() + 1);
					}
				}
			}
		}
		lanes.getLanesToLinkAssignments().get(link.getId()).addLane(origLane);
		for (Id<Lane> laneToBeRemoved : lanesToBeRemoved) {
			lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().remove(laneToBeRemoved);
		}
	}

	/*
	 * Creates a Stack of Lanedirection informations for every Lane. These Stacks
	 * are stacked-up for all Lanes. Directions are saved as int
	 * placeholder-variables. The far right Lane is on top of the Stack.
	 * nschirrmacher on 170613
	 */
	private void createLaneStack(String turnLanes, Stack<Stack<Integer>> turnLaneStack, double nofLanes, Id<Link> id) {

		String[] allTheLanes = turnLanes.split("\\|");
		for (int i = 0; i < allTheLanes.length; i++) {
			String[] directionsPerLane = allTheLanes[i].split(";");
			Stack<Integer> tempLane = new Stack<Integer>();
			for (int j = 0; j < directionsPerLane.length; j++) {
				Integer tempDir = null;
				if (directionsPerLane[j].equals("left")) {
					tempDir = 1;
				} else if (directionsPerLane[j].equals("slight_left")) {
					tempDir = 2;
				} else if (directionsPerLane[j].equals("sharp_left")) {
					tempDir = 3;
				} else if (directionsPerLane[j].equals("merge_to_right")) {
					tempDir = 4;
				} else if (directionsPerLane[j].equals("reverse")) {
					tempDir = 5;
				} else if (directionsPerLane[j].equals("through")) {
					tempDir = 0;
				} else if (directionsPerLane[j].equals("right")) {
					tempDir = -1;
				} else if (directionsPerLane[j].equals("slight_right")) {
					tempDir = -2;
				} else if (directionsPerLane[j].equals("sharp_right")) {
					tempDir = -3;
				} else if (directionsPerLane[j].equals("merge_to_left")) {
					tempDir = -5;
				} else if (directionsPerLane[j].equals(null)) {
					tempDir = null;
					LOG.warn("Lane-Tag was Null " + directionsPerLane[j] + " -> at link "+id.toString());
				} else {
					//Add here tempDir = 99 -> decide later what happens than with this
					tempDir = 99;
					LOG.warn("Could not read Turnlanes: \"" + directionsPerLane[j] + "\" -> at link "+id.toString());
				}
				tempLane.push(tempDir);
			}
			turnLaneStack.push(tempLane);
		}
		// TODO rather give an error message than filling with null-lanes here:
		// fills up Stack with dummy Lanes if size of Stack does not match
		// number of Lanes
		Stack<Integer> tempLane = new Stack<Integer>();
		if(turnLaneStack.size() < nofLanes) LOG.warn("Number of Lanes does not match the number of tags at link"
				+ id.toString()+"\n Tags:"+turnLaneStack.size()+"\n Lanes: "+nofLanes);
		this.linksNotMatchingTagsANDnoLanes.add(id);
		while (turnLaneStack.size() < nofLanes) {
			tempLane.push(null);
			turnLaneStack.push(tempLane);
		}
	}

	private List<LinkVector> constructInLinkVectors(Node node) {
		List<Link> inLinks = new ArrayList<Link>();
		for (Link l : node.getInLinks().values()) {
			inLinks.add(l);
		}
		List<LinkVector> inLinkVectors = new ArrayList<LinkVector>();
		for (int i = 0; i < inLinks.size(); i++) {
			//sbraun20200225: added this so we are using old degrees for vectors
			if (linkToOrigToNodeCoord.containsKey(inLinks.get(i).getId())|| linkToOrigFromNodeCoord.containsKey(inLinks.get(i).getId())){
				Coord oldToNode = inLinks.get(i).getToNode().getCoord();
				Coord oldFromNode = inLinks.get(i).getFromNode().getCoord();
				if (linkToOrigToNodeCoord.containsKey(inLinks.get(i).getId())){
					oldToNode = linkToOrigToNodeCoord.get(inLinks.get(i).getId());
				}
				if (linkToOrigFromNodeCoord.containsKey(inLinks.get(i).getId())){
					oldFromNode = linkToOrigFromNodeCoord.get(inLinks.get(i).getId());
				}

				LinkVector inLink = new LinkVector(inLinks.get(i),oldFromNode,oldToNode);
				inLinkVectors.add(inLink);

			} else {
				LinkVector inLink = new LinkVector(inLinks.get(i));
				inLinkVectors.add(inLink);
			}

		}
		return inLinkVectors;
	}

	private List<LinkVector> constructOrderedOutLinkVectors(Link fromLink) {
		List<Link> toLinks = new ArrayList<Link>();
		for (Link l : fromLink.getToNode().getOutLinks().values()) {
			toLinks.add(l);
		}
		List<LinkVector> toLinkVectors = orderToLinks(fromLink, toLinks);
		Map<Id<Link>, Double> toLinksAngles = new HashMap<Id<Link>, Double>();
		for (LinkVector lvec : toLinkVectors) {
			toLinksAngles.put(lvec.getLink().getId(), lvec.getRotation());
		}
		// FIXME Can I put a Map to attributes?
		fromLink.getAttributes().putAttribute(TO_LINKS_ANGLES, toLinksAngles);
		this.allToLinksAngles.put(fromLink.getId(), toLinksAngles);
		return toLinkVectors;
	}

	/**
	 * Fills already created Lanes of a Link with available informations: toLinks,
	 * ... (more planned). nschirrmacher on 170613
	 */
	private void fillLanesAndCheckRestrictions(Link link) {
		// create a List of all toLinks
		List<LinkVector> linkVectors = constructOrderedOutLinkVectors(link);

		if (osmNodeRestrictions.containsKey(Long.valueOf(link.getToNode().getId().toString()))) {
			// remove restricted toLinks from List
			removeRestrictedLinks(link, linkVectors);
		}

		// if a LaneStack exists, fill Lanes with turn:lane informations,
		// otherwise fill by default
		Stack<Stack<Integer>> laneStack = laneStacks.get(link.getId());
		if (laneStack != null && !laneStack.isEmpty()) {
			if (laneStack.size() != (int)link.getNumberOfLanes()) {
				throw new RuntimeException("the turn:lanes tag has a different number of lanes than the lanes tag");
			}
			for (int i = (int) link.getNumberOfLanes(); i > 0; i--) {
				Lane lane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
						.get(Id.create("Lane" + link.getId() + "." + i, Lane.class));
				setToLinksForLaneWithTurnLanes(lane, laneStack.pop(), linkVectors, (laneStack.size() == 1), link.getId());
			}
		} else {
			setToLinksForLanesDefault(link, linkVectors);
		}
	}

	private void setToLinksForLanesDefault(Link link, List<LinkVector> toLinks) {
		try {
			int straightLink = 0;
			int reverseLink = 0;
			int straightestLink = 0;
			for (int i = 1; i < toLinks.size(); i++) {
				if (Math.abs(toLinks.get(i).getRotation() - Math.PI) < Math
						.abs(toLinks.get(straightLink).getRotation() - Math.PI))
					straightLink = i;
				if (Math.abs(toLinks.get(i).getRotation() - Math.PI) > Math
						.abs(toLinks.get(reverseLink).getRotation() - Math.PI))
					reverseLink = i;

			}
			if (toLinks.get(straightLink).getRotation() < (1. - THROUGHLINK_ANGLE_TOLERANCE) * Math.PI
					|| toLinks.get(straightLink).getRotation() > (1. + THROUGHLINK_ANGLE_TOLERANCE) * Math.PI) {
				straightestLink = straightLink;
				straightLink = -1;
			}
			if (toLinks.get(reverseLink).getRotation() > THROUGHLINK_ANGLE_TOLERANCE * Math.PI
					&& toLinks.get(reverseLink).getRotation() < (2. - THROUGHLINK_ANGLE_TOLERANCE) * Math.PI)
				reverseLink = -1;
			if (toLinks.size() == 1) {
				lanes.getLanesToLinkAssignments().remove(link.getId());
				return;
			}

			//sbraun08082020 the case above leads to wrong results -> find reverse link with node coordinates:
			if (toLinks.size()==2){
				for (int i = 1; i < toLinks.size(); i++){
					Link tempLink = toLinks.get(i).getLink();
					if (tempLink.getToNode().getId().equals(link.getFromNode().getId()) &&
							tempLink.getFromNode().getId().equals(link.getToNode().getId())){
						reverseLink = i;
						break;
					}
				}
			}


			if (toLinks.size() == 2 && reverseLink >= 0) {
				lanes.getLanesToLinkAssignments().remove(link.getId());
				return;
			}

			if (lanes.getLanesToLinkAssignments().containsKey(link.getId()) && toLinks.size() > 1) {
				{
					// add right turn
					Lane lane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
							.get(Id.create("Lane" + link.getId() + "." + ((int) link.getNumberOfLanes()), Lane.class));
					if (reverseLink != 0)
						lane.addToLinkId(toLinks.get(0).getLink().getId());
					else
						lane.addToLinkId(toLinks.get(1).getLink().getId());
					lane.setAlignment(-2);
					//lane.getAttributes().putAttribute(TO_LINK_REFERENCE, OUTER_LANE_TYPE);

					// add left turn
					lane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
							.get(Id.create("Lane" + link.getId() + "." + "1", Lane.class));
					if (reverseLink != -1)
						lane.addToLinkId(toLinks.get(reverseLink).getLink().getId());
					if (reverseLink == toLinks.size() - 1)
						lane.addToLinkId(toLinks.get(toLinks.size() - 2).getLink().getId());
					else
						lane.addToLinkId(toLinks.get(toLinks.size() - 1).getLink().getId());
					lane.setAlignment(2);
					//lane.getAttributes().putAttribute(TO_LINK_REFERENCE, OUTER_LANE_TYPE);
				}

				// check for all toLinks can be reached. If not, add to right Lane
				if (link.getNumberOfLanes() == 2) {
					Lane leftLane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
							.get(Id.create("Lane" + link.getId() + "." + "1", Lane.class));
					Lane rightLane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
							.get(Id.create("Lane" + link.getId() + "." + ((int) link.getNumberOfLanes()), Lane.class));
					for (LinkVector lvec : toLinks) {
						if (!leftLane.getToLinkIds().contains(lvec.getLink().getId())
								&& !rightLane.getToLinkIds().contains(lvec.getLink().getId()))
							rightLane.addToLinkId(lvec.getLink().getId());
					}
				}

				int midLink = -1;
				{
					// add straight turns to middle lanes
					for (int i = (int) link.getNumberOfLanes() - 1; i > 1; i--) {
						Lane lane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
								.get(Id.create("Lane" + link.getId() + "." + i, Lane.class));
						if (straightLink >= 0) {
							lane.addToLinkId(toLinks.get(straightLink).getLink().getId());
							midLink = straightLink;
						} else {
							lane.addToLinkId(toLinks.get(straightestLink).getLink().getId());
							midLink = straightestLink;
						}
						//lane.getAttributes().putAttribute(TO_LINK_REFERENCE, MIDDLE_LANE_TYPE);
					}
				}

				// check for all toLinks can be reached. If not, add to second farthest right
				// Lane
				if (link.getNumberOfLanes() > 2) {
					List<Id<Link>> coveredLinks = new ArrayList<Id<Link>>();
					Lane laneToPutTo = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
							.get(Id.create("Lane" + link.getId() + "." + ((int) link.getNumberOfLanes() - 1), Lane.class));
					for (Lane lane : lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().values())
						coveredLinks.addAll(lane.getToLinkIds());
					for (LinkVector lvec : toLinks) {
						if (!coveredLinks.contains(lvec.getLink().getId()))
							laneToPutTo.addToLinkId(lvec.getLink().getId());
					}
				}
			}
		} catch (Exception e) {
			LOG.warn("Could not set Links for Lane Defaults for Link "+link.getId().toString());
			e.printStackTrace();
		}
	}

	// Fills Lanes with turn:lane informations
	private void setToLinksForLaneWithTurnLanes(Lane lane, Stack<Integer> laneStack, List<LinkVector> toLinks,
			boolean singleLaneOfTheLink, Id<Link> linkId) {
		//lane.getAttributes().putAttribute(TO_LINK_REFERENCE, "OSM-Information");
		int alignmentAnte;
		LinkVector throughLink = toLinks.get(0);
		double minDiff = Math.PI;
		LinkVector reverseLink = toLinks.get(0);
		double maxDiff = 0;
		for (LinkVector lvec : toLinks) {
			double diff = Math.abs(lvec.dirTheta - Math.PI);
			if (diff < minDiff) {
				minDiff = diff;
				throughLink = lvec;
			}
			if (diff > maxDiff) {
				maxDiff = diff;
				reverseLink = lvec;
			}
		}
		if (reverseLink.getRotation() < (2. - THROUGHLINK_ANGLE_TOLERANCE) * Math.PI
				&& reverseLink.getRotation() > THROUGHLINK_ANGLE_TOLERANCE * Math.PI)
			reverseLink = null;
        int it = 1;
        boolean insertedUTurnOnLeftLane = false;

		while (!laneStack.isEmpty()) {

			Integer tempDir = laneStack.pop();
			List<LinkVector> tempLinks = new ArrayList<LinkVector>();
			// removeLinks.clear();
			// log.info("Trying to Fill " + lane.getId().toString() + " with
			// Direction: " + tempDir + " with #ofToLinks: " + toLinks.size() );
			if (tempDir == null) { // no direction for lane available
                // only add straight to-link
                LOG.warn("Did not not understand direction of lane " + lane.getId().toString() +
                        " - MiddleLane type is realistic. Therefore only add straight direction.");
                lane.addToLinkId(throughLink.getLink().getId());
                writeOutLinkInfo(lane, throughLink.getLink().getId(), "through");


				lane.setAlignment(0);
				//sbraun 24072020 change break to continue-
				// if there are unreadable tags in between than the the other lanes remain unconsidered
				//then remove dummy lane logic in laneStack
				break;
			}
			if (tempDir < 0 && tempDir > -5 || tempDir == 99) { // all right directions (right,
				// slight_right,sharp_right)
                //sbraun 08082020 added tempDir == 99
//				for (LinkVector lvec : tempLinks) {
                for (LinkVector lvec : toLinks) {
					if (lvec.dirTheta < (1. - THROUGHLINK_ANGLE_TOLERANCE) * Math.PI)
						tempLinks.add(lvec);
				}

                //add U turn information here
				if (tempLinks.size() > 0 && reverseLink!=null) {
					if (tempLinks.get(0).getLink().getId().equals(reverseLink.getLink().getId())) {
						if (!this.allowUTurnAtLeftLaneOnly && insertedUTurnOnLeftLane) {
							lane.addToLinkId(tempLinks.get(0).getLink().getId());
							writeOutLinkInfo(lane, tempLinks.get(0).getLink().getId(), "reverse");

							if (this.saveTurnLanes == true) {
								String toLinkId = tempLinks.get(0).getLink().getId().toString();
								if (lane.getAttributes().getAttribute(OSM_TURN_INFO) == null) {
									lane.getAttributes().putAttribute(OSM_TURN_INFO, toLinkId + ":reverse");
								} else {
									String newTurn = lane.getAttributes().getAttribute(OSM_TURN_INFO).toString();
									lane.getAttributes().putAttribute(OSM_TURN_INFO, newTurn + "|" + toLinkId + ":reverse");
								}
							}
						}
					}
				}

				if (tempLinks.size() == 1) { // if there is just one "right"
					// link, take it
					lane.addToLinkId(tempLinks.get(0).getLink().getId());
					writeOutLinkInfo(lane, tempLinks.get(0).getLink().getId(), "right");

//                    lane.getAttributes().putAttribute(OSM_TURN_INFO, "right");
				} else if (tempLinks.size() == 2) {
					if (tempDir == -1 || tempDir == 99) { // lane direction: "right" //TODO sbraun 31072020 add right direction for 99??
						for (LinkVector lvec : tempLinks) {
							if (reverseLink != null && lvec.getLink().getId().equals(reverseLink.getLink().getId())){
								continue;
							}
							lane.addToLinkId(lvec.getLink().getId());
							writeOutLinkInfo(lane, lvec.getLink().getId(), "right");

//                            lane.getAttributes().putAttribute(OSM_TURN_INFO, "right");
						}
					}
					if (tempDir == -2) { // lane direction: "slight_right"
						if (tempLinks.get(0).dirTheta < Math.PI / 2.) {
                            lane.addToLinkId(tempLinks.get(1).getLink().getId());
							writeOutLinkInfo(lane, tempLinks.get(1).getLink().getId(), "slight_right");
                        } else {
                            lane.addToLinkId(tempLinks.get(0).getLink().getId());
							writeOutLinkInfo(lane, tempLinks.get(0).getLink().getId(), "slight_right");
						}
					}
					if (tempDir == -3) { // lane direction: "sharp_right"
						lane.addToLinkId(tempLinks.get(0).getLink().getId());
						writeOutLinkInfo(lane, tempLinks.get(0).getLink().getId(), "sharp_right");
					}
				} else {
					lane.addToLinkId(toLinks.get(0).getLink().getId());
				}
				lane.setAlignment(-2);
			}
			if (tempDir > 0 && tempDir < 4) { // all "left" directions (left,
				// slight_left,sharp_left)
				alignmentAnte = lane.getAlignment();
				if (alignmentAnte == 0 && it == 1)
					alignmentAnte = -10;
				for (LinkVector lvec : toLinks) {
					if (lvec.dirTheta > (1. + THROUGHLINK_ANGLE_TOLERANCE) * Math.PI)
						tempLinks.add(lvec);
				}


				if (tempLinks.size() == 1) { // if there is just one "left"
					// link, take it
					lane.addToLinkId(tempLinks.get(0).getLink().getId());
					writeOutLinkInfo(lane, tempLinks.get(0).getLink().getId(), "left");

				} else if (tempLinks.size() == 2) {
					//add Reverse Turn here and flag "insertedUTurnOnLeftLane" it (TODO maybe useful later other wise redundant)
                    // set to true to push U turn to left lane
					//sbraun 11.06.2020
					if (this.allowUTurnAtLeftLaneOnly && reverseLink != null){
						lane.addToLinkId(reverseLink.getLink().getId());
						insertedUTurnOnLeftLane = true;
						writeOutLinkInfo(lane, reverseLink.getLink().getId(), "reverse");
					}

					if (tempDir == 1) { // lane direction: "left"
						for (LinkVector lvec : tempLinks) {
                            // add link as to-link, if it is not the reverse link OR the link has only one lane OR u-turn is allowed at any lane
                            if (!lvec.equals(reverseLink) || singleLaneOfTheLink || !this.allowUTurnAtLeftLaneOnly) {
                                lane.addToLinkId(lvec.getLink().getId());
								writeOutLinkInfo(lane, lvec.getLink().getId(), "left");
                            }
                        }
					}
					if (tempDir == 2) { // lane direction: "slight_left"
						if (tempLinks.get(1).dirTheta > 3. * Math.PI / 2. || !tempLinks.get(1).equals(reverseLink)) {
                            lane.addToLinkId(tempLinks.get(0).getLink().getId());
							writeOutLinkInfo(lane, tempLinks.get(0).getLink().getId(), "slight_left");
                        }else{
							lane.addToLinkId(tempLinks.get(1).getLink().getId());
							writeOutLinkInfo(lane, tempLinks.get(1).getLink().getId(), "slight_left");
						}
					}
					if (tempDir == 3) { // lane direction: "sharp_left"
                        lane.addToLinkId(tempLinks.get(1).getLink().getId());
						writeOutLinkInfo(lane, tempLinks.get(1).getLink().getId(), "sharp_left");
                    }
				} else if (tempLinks.size() > 2) {
					for (LinkVector lvec : tempLinks)
						if (!lvec.equals(reverseLink) || singleLaneOfTheLink || !this.allowUTurnAtLeftLaneOnly) {
                            lane.addToLinkId(lvec.getLink().getId());
							writeOutLinkInfo(lane, lvec.getLink().getId(), "left");
                        }
				} else {
					lane.addToLinkId(toLinks.get(toLinks.size() - 1).getLink().getId());
					writeOutLinkInfo(lane, toLinks.get(toLinks.size() - 1).getLink().getId(), "left");
				}
				if (alignmentAnte == 0)
					lane.setAlignment(1);
				else
					lane.setAlignment(2);
			}
			if (tempDir == 0 || tempDir == 4 || tempDir == -5 || tempDir == 99) { // lane directions that have to lead to a forward link
																	// (through, merge_to_left,merge_to_right)
																	//sbraun24072020: added 99 -> a direction which has been not understood previously
				alignmentAnte = lane.getAlignment(); // look for the most "forward" link (closest to 180° or pi) and
														// take it
				//sbraun 08082020added special case rounabouts where through should actually be left

				boolean unclearThrough = false;
				if (toLinks.size()==2) {
					if (Math.abs(toLinks.get(0).dirTheta-Math.PI) < DEGREE_TOLERANCE_ROUNDABOUTS &&
							Math.abs(toLinks.get(1).dirTheta-Math.PI) < DEGREE_TOLERANCE_ROUNDABOUTS &&
							!(toLinks.get(0).getLink().equals(reverseLink)||toLinks.get(1).getLink().equals(reverseLink))){
						unclearThrough = true;
					}
				}
				if (unclearThrough) {
					LOG.warn("Detected unclear through direction at Lane " + lane.getId() +
							" this might be a large Roundabout. Therefore add both ToLinks as 'through'.");
				}
				if (unclearThrough){
					lane.addToLinkId(toLinks.get(0).getLink().getId());
					lane.addToLinkId(toLinks.get(1).getLink().getId());
				} else {
					lane.addToLinkId(throughLink.getLink().getId());
				}

				if (this.saveTurnLanes) {
					String toLinkId;
					if (unclearThrough) {
						toLinkId = toLinks.get(0).getLink().getId().toString()+","+toLinks.get(1).getLink().getId().toString();
					}else {
						toLinkId = throughLink.getLink().getId().toString();
					}

					if (lane.getAttributes().getAttribute(OSM_TURN_INFO) == null) {
						lane.getAttributes().putAttribute(OSM_TURN_INFO, toLinkId + ":through");
					} else {
						String newTurn = lane.getAttributes().getAttribute(OSM_TURN_INFO).toString();
						lane.getAttributes().putAttribute(OSM_TURN_INFO, newTurn + "|"+ toLinkId + ":through");
					}
				}

				if (alignmentAnte == -2)
					lane.setAlignment(-1);
			}
			//sbraun08082020 added this to ensure that some special links are connected e.g. Baenschstr./Berlin
			if (tempDir == 5 || network.getLinks().get(linkId).getNumberOfLanes()==1 ) { // lane direction: "reverse"
				// look for the most "backward" link (furthest from 180° or pi)
				// and take it
				alignmentAnte = lane.getAlignment();
				System.out.print(alignmentAnte);
				System.out.print(lane.getToLinkIds());
				if (alignmentAnte == 0 && lane.getToLinkIds()!=null) {// || lane.getToLinkIds().isEmpty())) //sbraun26052020
					if (lane.getToLinkIds().isEmpty())
					alignmentAnte = -10;
				}
				if (reverseLink!=null) {
                    lane.addToLinkId(reverseLink.getLink().getId());
					writeOutLinkInfo(lane, reverseLink.getLink().getId(), "reverse");
                }
				if (alignmentAnte == 0)
					lane.setAlignment(1);
				else
					lane.setAlignment(2);

			}
			if (lane.getToLinkIds()==null || lane.getToLinkIds().isEmpty()) {
				LOG.warn("No toLink could be found for " + lane.getId());
				if (toLinks.get(0).getLink()!=null) {
					lane.addToLinkId(toLinks.get(0).getLink().getId());
				}
			}
			it++;
		}
	}

	/*
	 * This class gets a fromLink and a List of toLinks. It returns a sorted List of
	 * LinkVectors. The LinkVectors are sorted from very right to very left. This is
	 * useful to check against the turnlane-informations later. nschirrmacher on
	 * 170613
	 */

	private List<LinkVector> orderToLinks(Link link, List<Link> toLinks) {
		List<LinkVector> toLinkList = new ArrayList<LinkVector>();
		LinkVector fromLink;
		//sbraun20200225: added this so we are using old degrees for vectors
		if (linkToOrigToNodeCoord.containsKey(link.getId())|| linkToOrigFromNodeCoord.containsKey(link.getId())) {
			Coord oldToNode = link.getToNode().getCoord();
			Coord oldFromNode = link.getFromNode().getCoord();
			if (linkToOrigToNodeCoord.containsKey(link.getId())) {
				oldToNode = linkToOrigToNodeCoord.get(link.getId());
			}
			if (linkToOrigFromNodeCoord.containsKey(link.getId())) {
				oldFromNode = linkToOrigFromNodeCoord.get(link.getId());
			}
			fromLink = new LinkVector(link,oldFromNode,oldToNode);
		} else {
			fromLink = new LinkVector(link);
		}


		//sbraun20200225: added this so we are using old degrees for vectors
		for (Link value : toLinks) {
			LinkVector toLink;// = new LinkVector(toLinks.get(i));
			//sbraun20200225: added this so we are using old degrees for vectors
			if (linkToOrigToNodeCoord.containsKey(value.getId()) || linkToOrigFromNodeCoord.containsKey(value.getId())) {
				Coord oldToNode = value.getToNode().getCoord();
				Coord oldFromNode = value.getFromNode().getCoord();
				if (linkToOrigToNodeCoord.containsKey(value.getId())) {
					oldToNode = linkToOrigToNodeCoord.get(value.getId());
				}
				if (linkToOrigFromNodeCoord.containsKey(value.getId())) {
					oldFromNode = linkToOrigFromNodeCoord.get(value.getId());
				}

				toLink = new LinkVector(value, oldFromNode, oldToNode);
				toLink.calculateRotation(fromLink);
				toLinkList.add(toLink);

			} else {
				toLink = new LinkVector(value);
				toLink.calculateRotation(fromLink);

				toLinkList.add(toLink);

			}
		}
		Collections.sort(toLinkList);
		return toLinkList;
	}

	private void removeRestrictedLinks(Link fromLink, List<LinkVector> toLinks) {
		//07082020 sbraun I added a return if only one Link is in the toLinks structure
		// -> that way the network is still connected
		if (toLinks.size()== 1){
			LOG.warn("The Node between ToLink "+toLinks.get(0).getLink().getId().toString()+" and FromLink "+fromLink.getId().toString()+
					"is restricted. Since there is only one ToLink this one will not be removed to ensure that the network remains connected");
			return;
		}

		OsmNode toNode = nodes.get(Long.valueOf(fromLink.getToNode().getId().toString()));
		for (OsmRelation restriction : osmNodeRestrictions.get(toNode.id)) {
			if (Long.valueOf(fromLink.getAttributes().getAttribute(ORIG_ID).toString()) == restriction.fromRestricted.id) {
				if (restriction.restrictionValue == false) {
					LinkVector lvec2remove = null;
					for (LinkVector linkVector : toLinks) {
						//sbraun08082020 added this to allow Uturns
						if (linkVector.getLink().getToNode().getId().equals(fromLink.getFromNode().getId()) &&
								linkVector.getLink().getFromNode().getId().equals(fromLink.getToNode().getId())&&
								toLinks.size()==2){
							LOG.warn("Don't delete reverse Link from Link "+fromLink.getId().toString()+" to prevent Dead-Ends");
							continue;
						}
						if (Long.valueOf(linkVector.getLink().getAttributes().getAttribute(ORIG_ID)
								.toString()) == restriction.toRestricted.id) {
							lvec2remove = linkVector;
							break;
						}
					}
					if (lvec2remove!=null) toLinks.remove(lvec2remove);
				} else {
					for (LinkVector linkVector : toLinks) {
						if (Long.valueOf(linkVector.getLink().getAttributes().getAttribute(ORIG_ID)
								.toString()) == restriction.toRestricted.id) {
							LinkVector onlyLink = linkVector;
							toLinks.clear();
							toLinks.add(onlyLink);
							return;
						}
					}
				}
			}
		}
	}

	@Override
	protected void setOrModifyNodeAttributes(Node n, OsmNode node) {
		// create empty signal system for the node
		if (signalizedOsmNodes.contains(node.id) && (bbox == null || bbox.contains(node.coord))) {
			Id<SignalSystem> systemId = Id.create("System" + node.id, SignalSystem.class);
			if (!this.systems.getSignalSystemData().containsKey(systemId)) {
				SignalSystemData system = this.systems.getFactory().createSignalSystemData(systemId);
				this.systems.getSignalSystemData().put(systemId, system);
			}
		}
	}

	@Override
	protected void setOrModifyLinkAttributes(Link l, OsmWay way, boolean forwardDirection) {
		// modify to/from nodes if they have been simplified (earlier in simplifyOsmData)
		long toNodeOsmId = Long.valueOf(l.getToNode().getId().toString());
		long fromNodeOsmId = Long.valueOf(l.getFromNode().getId().toString());
		if (oldToMergedJunctionNodeMap.containsKey(toNodeOsmId)) {
			//remember old toNode for LinkVector calculation
			linkToOrigToNodeCoord.put(l.getId(), l.getToNode().getCoord());
			//change toNode
			long simplifiedOsmNodeId = oldToMergedJunctionNodeMap.get(toNodeOsmId).id;
			l.setToNode(network.getNodes().get(Id.createNodeId(simplifiedOsmNodeId)));
			l.setLength(NetworkUtils.getEuclideanDistance(l.getFromNode().getCoord(), l.getToNode().getCoord()));
		}
		if (oldToMergedJunctionNodeMap.containsKey(fromNodeOsmId)) {
			//remember old fromNode for LinkVector calculation
			linkToOrigFromNodeCoord.put(l.getId(), l.getFromNode().getCoord());
			//change fromNode
			long simplifiedOsmNodeId = oldToMergedJunctionNodeMap.get(fromNodeOsmId).id;
			l.setFromNode(network.getNodes().get(Id.createNodeId(simplifiedOsmNodeId)));
			l.setLength(NetworkUtils.getEuclideanDistance(l.getFromNode().getCoord(), l.getToNode().getCoord()));
		}
		if (l.getToNode().equals(l.getFromNode())){
			loopLinks.add(l.getId());
		}
		// convert lane directions
		String turnLanesOsm;
		if (forwardDirection) {
			turnLanesOsm = way.tags.get(TAG_TURNLANESFORW);
			if (turnLanesOsm == null) {
				// use general turn lanes tag only if turn lanes forward not existent
				turnLanesOsm = way.tags.get(TAG_TURNLANES);
			}
		} else { // i.e. backward direction
			turnLanesOsm = way.tags.get(TAG_TURNLANESBACK);
		}
		Stack<Stack<Integer>> turnLanesOfThisLink = new Stack<Stack<Integer>>();
		if (turnLanesOsm != null) {
			createLaneStack(turnLanesOsm, turnLanesOfThisLink, l.getNumberOfLanes(), l.getId());
			if (l.getNumberOfLanes() < turnLanesOfThisLink.size()) {
				// TODO this prioritizes turn:lanes over #lanes. consistent? adapt capacity too?
				if(turnLanesOfThisLink.size()==0.5) LOG.warn(turnLanesOfThisLink.size());
                l.setNumberOfLanes(turnLanesOfThisLink.size());
			}
		}

		// create Lanes only if more than one Lane detected
		Node toNode = this.network.getNodes().get(l.getToNode().getId());
		if (l.getNumberOfLanes() > 1 && (bbox == null || bbox.contains(toNode.getCoord()))) {
			createLanes(l, lanes, l.getNumberOfLanes());
			if (turnLanesOfThisLink != null) {
				this.laneStacks.put(l.getId(), turnLanesOfThisLink);
			}
		}
	}


	public boolean isNodeAtJunction(OsmNode node) {
		if (node.endPoint && node.ways.size() > 2)
			return true;
		if (!node.endPoint && node.ways.size() > 1)
			return true;
		if (node.endPoint && node.ways.size() == 2) {
			for (OsmNetworkReader.OsmWay way : node.ways.values()) {
				for (int i = 0; i < way.nodes.size(); i++) {
					if (node.id == (way.nodes.get(i))) {
						if (i != 0 && i != way.nodes.size() - 1)
							// the node is an endpoint of one way and no endpoint of the other way -> three arm intersection
							return true;
					}
				}
			}
		}
		return false;
	}

    private boolean hasNodeOneway(OsmNode node) {
		boolean hasOneway = false;
		for (OsmWay way : node.ways.values()) {
			String oneway = way.tags.get(TAG_ONEWAY);
			if (oneway != null && !oneway.equals("no"))
				hasOneway = true;
		}
		return hasOneway;
	}


	private void writeOutLinkInfo(Lane lane, Id<Link> linkId, String dir){
		if (this.saveTurnLanes) {
			if (lane.getAttributes().getAttribute(OSM_TURN_INFO) == null) {
				lane.getAttributes().putAttribute(OSM_TURN_INFO, linkId.toString() + ":" + dir);
			} else {
				String newTurn = lane.getAttributes().getAttribute(OSM_TURN_INFO).toString();
				lane.getAttributes().putAttribute(OSM_TURN_INFO, newTurn + "|" + linkId.toString() + ":" + dir);
			}
		}
	}


	public Set<Long> getNodesNotToMerge() {
		return nodesNotToMerge;
	}


	private final class SignalLanesOsmXmlParser extends OsmXmlParser {

		private OsmRelation currentRelation = null;
		
		public SignalLanesOsmXmlParser(final Map<Long, OsmNode> nodes, final Map<Long, OsmWay> ways,
				final CoordinateTransformation transform) {
			super(nodes, ways, transform);
		}
		
		@Override
		public void enableOptimization(int step) {
			throw new UnsupportedOperationException("slow but low memory is not supported for the signals and lanes osm reader");
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			super.startTag(name, atts, context);
			
			if ("relation".equals(name)) {
				this.currentRelation = new OsmRelation(Long.parseLong(atts.getValue("id")));
			} else if ("tag".equals(name)) {
				if (this.currentNode != null) {
					String key = atts.getValue("k");
					String value = atts.getValue("v");
					if ("highway".equals(key) && "traffic_signals".equals(value)) {
						signalizedOsmNodes.add(currentNode.id);
					}
					if ("highway".equals(key) && "crossing".equals(value)) {
						crossingOsmNodes.add(currentNode.id);
					}
				}
				if (this.currentRelation != null) {
					String key = atts.getValue("k");
					String value = atts.getValue("v");
					if ("restriction".equals(key)) {
						if ("no".equals(value.substring(0, 2))) {
							this.currentRelation.restrictionValue = false;
						} else if ("only".equals(value.substring(0, 4))) {
							this.currentRelation.restrictionValue = true;
						}
					}
				}
			} else if ("member".equals(name)) {
				if (this.currentRelation != null) {
					String type = atts.getValue("type");
					String role = atts.getValue("role");
					if ("node".equals(type)) {
						this.currentRelation.resNode = this.nodes.get(Long.parseLong(atts.getValue("ref")));
					} else if ("way".equals(type)) {
						if ("from".equals(role)) {
							this.currentRelation.fromRestricted = this.ways.get(Long.parseLong(atts.getValue("ref")));
						} else if ("to".equals(role)) {
							this.currentRelation.toRestricted = this.ways.get(Long.parseLong(atts.getValue("ref")));

						}
					}
				}
			}
		}

		@Override
		public void endTag(final String name, final String content, final Stack<String> context) {
			super.endTag(name, content, context);
			
			if ("relation".equals(name)) {
				if (this.currentRelation.fromRestricted != null) {
					if (this.currentRelation.resNode != null && this.currentRelation.toRestricted != null) {
						if (!osmNodeRestrictions.containsKey(this.currentRelation.resNode.id)) {
							osmNodeRestrictions.put(this.currentRelation.resNode.id, new HashSet<>());
						}
						osmNodeRestrictions.get(this.currentRelation.resNode.id).add(this.currentRelation);
					}
				} else {
					this.currentRelation = null;
				}
			}
		}

	}
	private static final class OsmRelation {
		public final long id;
		public OsmNode resNode;
		public OsmWay fromRestricted;
		public OsmWay toRestricted;
		public boolean restrictionValue;

		public OsmRelation(final long id) {
			this.id = id;
		}
	}


	private static final class LinkVector implements Comparable<LinkVector> {
		private Link link;
		private double x;
		private double y;
		private double theta;
		private double dirTheta;

		public LinkVector(Link link) {
			this.link = link;
			this.x = this.link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
			this.y = this.link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();
			this.calculateTheta();
		}

		//08042020 sbraun second constructor since we changed coordinates
		public LinkVector(Link link, Coord oldFromNode, Coord oldToNode){
			this.link = link;
			this.x = oldToNode.getX() - oldFromNode.getX();
			this.y = oldToNode.getY() - oldFromNode.getY();
			this.calculateTheta();
		}

		private void calculateTheta() {
			if (this.y >= 0.) {
				this.theta = Math.atan2(this.y, this.x);
			} else {
				this.theta = 2. * Math.PI + Math.atan2(this.y, this.x);
			}
		}

		public void calculateRotation(LinkVector linkVector) {
			if (this.theta <= Math.PI)
				this.dirTheta = this.theta - linkVector.getAlpha() + Math.PI;
			else
				this.dirTheta = this.theta - linkVector.getAlpha() - Math.PI;
			if (this.dirTheta < 0) {
				this.dirTheta += 2. * Math.PI;
			}

		}

		public double getAlpha() {
			return this.theta;
		}

		public double getRotation() {
			return this.dirTheta;
		}

		public double getRotationToOtherInLink(LinkVector linkVector) {
			double rotation = linkVector.getAlpha() - this.theta;
			if (rotation < 0) {
				rotation += 2. * Math.PI;
			}
			return rotation;
		}

		public Link getLink() {
			return this.link;
		}

		@Override
		public int compareTo(LinkVector lv) {
			double otherDirAlpha = lv.getRotation();
			if (this.dirTheta == otherDirAlpha)
				return 0;
			if (this.dirTheta > otherDirAlpha)
				return 1;
			else
				return -1;
		}

	}


	private static final class BoundingBox {
		private double south;
		private double west;
		private double north;
		private double east;

		public BoundingBox(double south, double west, double north, double east) {
			this.south = south;
			this.west = west;
			this.north = north;
			this.east = east;
		}

		public boolean contains(Coord coord) {
			if ((coord.getX() < this.east && coord.getX() > this.west)
					&& (coord.getY() < this.north && coord.getY() > this.south))
				return true;
			else
				return false;
		}
	}

}
