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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.consistency.LanesAndSignalsCleaner;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesFactory;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.matsim.lanes.data.LanesWriter;
import org.xml.sax.Attributes;

import com.vividsolutions.jts.util.Assert;

/**
 * @author tthunig, nschirrmacher
 */

public class SignalsAndLanesOsmNetworkReader extends OsmNetworkReader {

	private final static Logger LOG = Logger.getLogger(SignalsAndLanesOsmNetworkReader.class);

	private final static String TAG_RESTRICTION = "restriction";
	private final static String TAG_TURNLANES = "turn:lanes";
	private final static String TAG_TURNLANESFORW = "turn:lanes:forward";
	private final static String TAG_TURNLANESBACK = "turn:lanes:backward";

	private final static int DEFAULT_LANE_OFFSET = 35;
	private final static int INTERGREENTIME = 5;
	private final static int MIN_GREENTIME = 10;
	private final static double SIGNAL_MERGE_DISTANCE = 40.0;
	private final static double SIGNAL_LANES_CAPACITY = 2000.0;
	private final static double THROUGHLINK_ANGLE_TOLERANCE = 0.1666667;
	private final static int PEDESTRIAN_CROSSING_TIME = 20;
	private final static int CYCLE_TIME = 90;
	private final int minimalTimeForPair = 2 * INTERGREENTIME + 2 * MIN_GREENTIME;

	private final static String ORIG_ID = "origid";
	private final static String TYPE = "type";
	private final static String TO_LINKS_ANGLES = "toLinksAngles";
	private final static String IS_ORIG_LANE = "isOrigLane";
	private final static String TO_LINK_REFERENCE = "toLinkReference";
	private final static String NON_CRIT_LANES = "non_critical_lane";
	private final static String CRIT_LANES = "critical_lane";

	// specify turn restrictions of lanes without turn:lanes information on OSM
	private final MiddleLaneRestriction MIDDLE_LANE_TYPE = MiddleLaneRestriction.REALISTIC;
	private final OuterLaneRestriction OUTER_LANE_TYPE = OuterLaneRestriction.RESTRICTIVE;
	public enum MiddleLaneRestriction {
		REGULATION_BASED, // all turns are allowed from middle lanes, except u-turns
		REALISTIC // only straight traffic allowed from middle lanes
	}
	public enum OuterLaneRestriction {
		VERY_RESTRICTIVE, // only left and u-turns at the left most lane, only right turns at the right most lane
		RESTRICTIVE, // only left and u-turns at the left most lane, right and straight turns at the right most lane
		NON_RESTRICTIVE // left, u- and straight turns at the left most lane, right and straight turns at the right most lane
	}

	private final Map<Id<Link>, Stack<Stack<Integer>>> laneStacks = new HashMap<>();
	private final Map<Long, OsmNode> roundaboutNodes = new HashMap<>();
	private final Map<Id<Link>, Map<Id<Link>, Double>> allToLinksAngles = new HashMap<>();
	private final Map<Id<Lane>, List<Id<Lane>>> nonCritLanes = new HashMap<>();
	private final Map<Id<Lane>, List<Id<Lane>>> critLanes = new HashMap<>();
	private final Map<Long, Double> turnRadii = new HashMap<>();
	
	// Node stuff
	Set<Long> signalizedOsmNodes = new HashSet<>();
	Set<Long> crossingOsmNodes = new HashSet<>();
	Map<Long, OsmNode> oldToSimplifiedJunctionNodeMap = new HashMap<>();
	Map<Long, Set<OsmRelation>> osmNodeRestrictions = new HashMap<>();
	

	private boolean minimizeSmallRoundabouts = true;
	private boolean mergeOnewaySignalSystems = true;
	private boolean useRadiusReduction = true;
	private boolean allowUTurnAtLeftLaneOnly = true;
	private boolean makePedestrianSignals = false;
	private boolean acceptFourPlusCrossings = false;

	private final SignalSystemsData systems;
	private final SignalGroupsData groups;
	private final SignalControlData control;
	private final Lanes lanes;

	private BoundingBox bbox = null;

	public static void main(String[] args) {
//		String inputOSM = "../../../shared-svn/studies/countries/de/cottbus-osmSignalsLanes/input/osm/brandenburg.osm";
//		String outputDir = "../../../shared-svn/studies/countries/de/cottbus-osmSignalsLanes/input/matsim/";
//		String inputOSM = "../../../shared-svn/studies/tthunig/osmData/brandenburg-latest.osm";
//		String outputDir = "../../../shared-svn/studies/tthunig/osmData/signalsAndLanesReader/brandenburg/";
//		String inputOSM = "../../../shared-svn/studies/tthunig/osmData/berlin-latest.osm";
//		String outputDir = "../../../shared-svn/studies/tthunig/osmData/signalsAndLanesReader/berlin/";
		String inputOSM = "../../../shared-svn/studies/tthunig/osmData/interpreter.osm";
		String outputDir = "../../../shared-svn/studies/tthunig/osmData/signalsAndLanesReader/cottbusCity/";
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				TransformationFactory.WGS84_UTM33N);

		// create a config
		Config config = ConfigUtils.createConfig();
		SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalSystemsConfigGroup.setUseSignalSystems(true);
		config.qsim().setUseLanes(true);

		// create a scenario
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		// pick network, lanes and signals data from the scenario
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		Lanes lanes = scenario.getLanes();
		Network network = scenario.getNetwork();

		SignalsAndLanesOsmNetworkReader reader = new SignalsAndLanesOsmNetworkReader(network, ct, signalsData, lanes);
		reader.setAssumptions(false, // minimize small roundabouts
				false, // merge oneway Signal Systems
				false, // use radius reduction
				true, // allow U-turn at left lane only
				true, // make pedestrian signals
				false); // accept 4+ crossings
		reader.setBoundingBox(51.7464, 14.3087, 51.7761, 14.3639); // setting Bounding Box for signals and lanes
																	// (south,west,north,east)
		reader.parse(inputOSM);
		reader.stats();

		/*
		 * Clean the Network. Cleaning means removing disconnected components, so that
		 * afterwards there is a route from every link to every other link. This may not
		 * be the case in the initial network converted from OpenStreetMap.
		 */

		new NetworkCleaner().run(network);
		new LanesAndSignalsCleaner().run(scenario);

		/*
		 * Write the files out: network, lanes, signalSystems, signalGroups,
		 * signalControl
		 */

		new NetworkWriter(network).write(outputDir + "network.xml");
		new LanesWriter(lanes).write(outputDir + "lanes.xml");
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(outputDir + "signalSystems.xml");
		signalsWriter.setSignalGroupsOutputFilename(outputDir + "signalGroups.xml");
		signalsWriter.setSignalControlOutputFilename(outputDir + "signalControl.xml");
		signalsWriter.writeSignalsData(scenario);
	}

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

	public void setBoundingBox(double south, double west, double north, double east) {
		Coord nw = this.transform.transform(new Coord(west, north));
		Coord se = this.transform.transform(new Coord(east, south));
		this.bbox = new BoundingBox(se.getY(), nw.getX(), nw.getY(), se.getX());
	}

	// TODO single setters for all
	public void setAssumptions(boolean minimizeSmallRoundabouts, boolean mergeOnewaySignalSystems,
			boolean useRadiusReduction, boolean allowUTurnAtLeftLaneOnly, boolean makePedestrianSignals,
			boolean acceptFourPlusCrossings) {
		this.minimizeSmallRoundabouts = minimizeSmallRoundabouts;
		this.mergeOnewaySignalSystems = mergeOnewaySignalSystems;
		this.useRadiusReduction = mergeOnewaySignalSystems;
		this.allowUTurnAtLeftLaneOnly = allowUTurnAtLeftLaneOnly;
		this.makePedestrianSignals = makePedestrianSignals;
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
		
//		simplifiyRoundaboutSignals();
		pushSignalsIntoNearbyJunctions();
		pushingSingnalsIntoEndpoints();
		pushSignalsOverShortWays();
		removeSignalsAtDeadEnds();
		// TODO check and clean this methods
	}

	/**
	 * Extends the super class method: Intersections with more than two ways (i.e.
	 * more than one intersection node) are simplified into one intersection node.
	 */
	@Override
	protected void simplifyOsmData() {
		super.simplifyOsmData();
		
		// TODO move this into the general osm network reader (with a flag)
		// (afterwards, make nodes private again)

		// Trying to simplify four-node- and two-node-junctions to one-node-junctions
		List<OsmNode> addingNodes = new ArrayList<>();
		List<OsmNode> checkedNodes = new ArrayList<>();
		List<OsmWay> checkedWays = new ArrayList<>();
		if (this.minimizeSmallRoundabouts)
			findingSmallRoundabouts(addingNodes, checkedNodes, checkedWays);

		findingFourNodeJunctions(addingNodes, checkedNodes);

		findingMoreNodeJunctions(addingNodes, checkedNodes);

		findingTwoNodeJunctions(addingNodes, checkedNodes);

		if (this.mergeOnewaySignalSystems)
			mergeOnewaySignalSystems(addingNodes, checkedNodes);

		for (OsmNode node : addingNodes) {
			super.nodes.put(node.id, node);
		}
		addingNodes.clear();
		checkedNodes.clear();
		// TODO check and clean this methods
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
		
		// TODO check and clean this method!

		// lanes were already created but without toLinks. add toLinks now:
		for (Link link : network.getLinks().values()) {
			if (link.getToNode().getOutLinks().size() > 1) {
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

		for (Link link : network.getLinks().values()) {
			if (lanes.getLanesToLinkAssignments().get(link.getId()) != null) {
				simplifyLanesAndAddOrigLane(link);
			}
			Id<SignalSystem> systemId = Id.create("System" + link.getToNode().getId(), SignalSystem.class);
			if (this.systems.getSignalSystemData().containsKey(systemId)
					&& lanes.getLanesToLinkAssignments().containsKey(link.getId())) {
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
			}
			if (this.systems.getSignalSystemData().containsKey(systemId)
					&& !lanes.getLanesToLinkAssignments().containsKey(link.getId())) {
				SignalData signal = this.systems.getFactory()
						.createSignalData(Id.create("Signal" + link.getId() + ".single", Signal.class));
				signal.setLinkId(link.getId());
				this.systems.getSignalSystemData().get(systemId).addSignalData(signal);
			}
		}
		int badCounter = 0;
		for (Node node : network.getNodes().values()) {

			Id<SignalSystem> systemId = Id.create("System" + Long.valueOf(node.getId().toString()), SignalSystem.class);
			if (this.systems.getSignalSystemData().containsKey(systemId)) {
				SignalSystemData signalSystem = this.systems.getSignalSystemData().get(systemId);
				if (node.getInLinks().size() == 1) {
					if (this.makePedestrianSignals) {
						createPlansForOneWayJunction(signalSystem, node);
						LOG.info("single signal found @ " + node.getId());
						badCounter++;
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
					for (int i = 0; i < inLinks.size(); i++) {
						if (first == null) {
							if (!inLinks.get(i).equals(firstPair.getFirst())
									&& !inLinks.get(i).equals(firstPair.getSecond())) {
								first = inLinks.get(i);
							}
						} else {
							if (!inLinks.get(i).equals(firstPair.getFirst())
									&& !inLinks.get(i).equals(firstPair.getSecond())) {
								second = inLinks.get(i);
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
								+ node.getId().toString());
					} else {
						throw new RuntimeException("Signal system with more than four in-links detected @ Node "
								+ node.getId().toString());
					}
				}
			}

		}
		LOG.info(badCounter);
	}

	private void mergeOnewaySignalSystems(List<OsmNode> addingNodes, List<OsmNode> checkedNodes) {
		for (OsmNode node : this.nodes.values()) {
			List<OsmNode> junctionNodes = new ArrayList<OsmNode>();
			if (signalizedOsmNodes.contains(node.id) && isNodeAtJunction(node) 
					&& !oldToSimplifiedJunctionNodeMap.containsKey(node.id) && hasNodeOneway(node)) {
				junctionNodes.add(node);
				for (OsmNode otherNode : this.nodes.values()) {
					if (signalizedOsmNodes.contains(otherNode.id) && isNodeAtJunction(otherNode)
							&& calcNode2NodeDistance(node, otherNode) < SIGNAL_MERGE_DISTANCE 
							&& !oldToSimplifiedJunctionNodeMap.containsKey(otherNode.id)
							&& hasNodeOneway(otherNode)) {
						junctionNodes.add(otherNode);
					}
				}
			}
			if (junctionNodes.size() > 1) {
				double repXmin = 0;
				double repXmax = 0;
				double repYmin = 0;
				double repYmax = 0;
				double repX;
				double repY;
				for (OsmNode tempNode : junctionNodes) {
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
				for (OsmNode betweenNode : this.nodes.values()) {
					if (box.contains(betweenNode.coord))
						junctionNodes.add(betweenNode);
				}
				OsmNode junctionNode = new OsmNode(this.id, new Coord(repX, repY));
				signalizedOsmNodes.add(junctionNode.id);
				junctionNode.used = true;
				for (OsmNode tempNode : junctionNodes) {
					// TODO tempNode.used = false; node in way ersetzen -> repJunNode nicht noetig?!
					oldToSimplifiedJunctionNodeMap.put(tempNode.id, junctionNode);
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

	private void setInLinksCapacities(Node node) {
		List<LinkVector> inLinks = constructInLinkVectors(node);
		for (LinkVector lvec : inLinks) {
			if (this.lanes.getLanesToLinkAssignments().containsKey(lvec.getLink().getId())) {
				Lane origLane = null;
				double olCapacity = 0;
				for (Lane lane : this.lanes.getLanesToLinkAssignments().get(lvec.getLink().getId()).getLanes()
						.values()) {
					if (lane.getAttributes().getAttribute(IS_ORIG_LANE).equals(false)) {
						lane.setCapacityVehiclesPerHour(SIGNAL_LANES_CAPACITY * lane.getNumberOfRepresentedLanes());
						if (this.useRadiusReduction) {
							Long key = Long.valueOf(lvec.getLink().getToNode().getId().toString());
							if (lane.getAlignment() == 2 && this.turnRadii.containsKey(key)) {
								double radius = this.turnRadii.get(key);
								double reductionFactor = getRadiusCapacityReductionFactor(radius);
								lane.setCapacityVehiclesPerHour(lane.getCapacityVehiclesPerHour() * reductionFactor);
							} else if (lane.getAlignment() == 2 || lane.getAlignment() == -2) {
								double reductionFactor = getRadiusCapacityReductionFactor(0);
								lane.setCapacityVehiclesPerHour(lane.getCapacityVehiclesPerHour() * reductionFactor);
							}
						}
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
		for (OsmWay way : node.ways.values()) {
			String oneway = way.tags.get(TAG_ONEWAY);
			if (oneway != null) { // && (oneway.equals("yes") || oneway.equals("true") || oneway.equals("1"))
				for (int i = way.nodes.indexOf(node.id) + 1; i < way.nodes.size(); i++) {
					OsmNode otherNode = nodes.get(way.nodes.get(i));
					if (otherNode.used && !checkedNodes.contains(otherNode) && !junctionNodes.contains(otherNode)) {
						if (calcNode2NodeDistance(node, otherNode) < distance) {
							if (otherNode.id == firstNode.id) {
								junctionNodes.add(otherNode);
							} else {

								junctionNodes.add(otherNode);
								findCloseJunctionNodesWithSignals(firstNode, otherNode, junctionNodes, checkedNodes,
										distance, getAll);
								if (!junctionNodes.contains(firstNode)) {
									junctionNodes.remove(otherNode);
								}
							}
						}
						break;
					}
				}
			}
			if (junctionNodes.contains(firstNode) && !getAll)
				break;
		}
	}

	private void findingSmallRoundabouts(List<OsmNode> addingNodes, List<OsmNode> checkedNodes,
			List<OsmWay> checkedWays) {
		for (OsmWay way : this.ways.values()) {
			String roundabout = way.tags.get(TAG_JUNCTION);
			if (roundabout != null && roundabout.equals("roundabout") && !checkedWays.contains(way)) {
				List<OsmNode> roundaboutNodes = new ArrayList<>();
				double radius = 20;
				if (this.nodes.get(way.nodes.get(0)).equals(this.nodes.get(way.nodes.get(way.nodes.size() - 1)))) {
					checkedWays.add(way);
					for (Long nodeId : way.nodes) {
						roundaboutNodes.add(this.nodes.get(nodeId));
					}
				}

				if (roundaboutNodes.size() > 1) {
					double repXmin = 0;
					double repXmax = 0;
					double repYmin = 0;
					double repYmax = 0;
					double repX;
					double repY;
					OsmNode lastNode = roundaboutNodes.get(roundaboutNodes.size() - 1);
					double circumference = 0;
					for (OsmNode tempNode : roundaboutNodes) {
						if (repXmin == 0 || tempNode.coord.getX() < repXmin)
							repXmin = tempNode.coord.getX();
						if (repXmax == 0 || tempNode.coord.getX() > repXmax)
							repXmax = tempNode.coord.getX();
						if (repYmin == 0 || tempNode.coord.getY() < repYmin)
							repYmin = tempNode.coord.getY();
						if (repYmax == 0 || tempNode.coord.getY() > repYmax)
							repYmax = tempNode.coord.getY();
						circumference += calcNode2NodeDistance(tempNode,lastNode);
						lastNode = tempNode;
					}
					repX = repXmin + (repXmax - repXmin) / 2;
					repY = repYmin + (repYmax - repYmin) / 2;
					if ((circumference / (2 * Math.PI)) < radius) {
						OsmNode roundaboutNode = new OsmNode(this.id, new Coord(repX, repY));
						roundaboutNode.used = true;
						for (OsmNode tempNode : roundaboutNodes) {
							oldToSimplifiedJunctionNodeMap.put(tempNode.id, roundaboutNode);
							if (osmNodeRestrictions.containsKey(tempNode.id)) {
								osmNodeRestrictions.put(roundaboutNode.id, osmNodeRestrictions.get(tempNode.id));						
							}
							checkedNodes.add(tempNode);
							tempNode.used = true;
						}
						addingNodes.add(roundaboutNode);
						id++;
					}
				}
			}
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
					double leftTurnRadius = 0;
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
					leftTurnRadius = ((repXmax - repXmin) + (repYmax - repYmin)) / 2;
					OsmNode junctionNode = new OsmNode(this.id, new Coord(repX, repY));
					if (signalized)
						signalizedOsmNodes.add(junctionNode.id);
					junctionNode.used = true;
					for (OsmNode tempNode : junctionNodes) {
						oldToSimplifiedJunctionNodeMap.put(tempNode.id, junctionNode);
						if (osmNodeRestrictions.containsKey(tempNode.id)) {
							osmNodeRestrictions.put(junctionNode.id, osmNodeRestrictions.get(tempNode.id));						
						}
						checkedNodes.add(tempNode);
					}
					addingNodes.add(junctionNode);
					this.turnRadii.put(junctionNode.id, leftTurnRadius);
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
					for (OsmWay way : node.ways.values()) {
						String oneway = way.tags.get(TAG_ONEWAY);
						if (oneway != null && !oneway.equals("no"))
							break;
						for (int i = 0; i < way.nodes.size(); i++) {
							if (otherSuit == true)
								break;
							otherNode = nodes.get(way.nodes.get(i));

							boolean nodeSignalized = signalizedOsmNodes.contains(node.id);
							boolean otherNodeSignalized = signalizedOsmNodes.contains(otherNode.id);
							if (calcNode2NodeDistance(node, otherNode) < SIGNAL_MERGE_DISTANCE
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
							}
						}
						if (suit == true && otherSuit == true)
							break;
					}
					if (suit == true && otherSuit == true && otherNode != null) {
						double repX = (node.coord.getX() + otherNode.coord.getX()) / 2;
						double repY = (node.coord.getY() + otherNode.coord.getY()) / 2;
						OsmNode junctionNode = new OsmNode(this.id, new Coord(repX, repY));
						if (signalizedOsmNodes.contains(node.id) || signalizedOsmNodes.contains(otherNode.id))
							signalizedOsmNodes.add(junctionNode.id);
						junctionNode.used = true;
						oldToSimplifiedJunctionNodeMap.put(node.id, junctionNode);
						if (osmNodeRestrictions.containsKey(node.id)) {
							osmNodeRestrictions.put(junctionNode.id, osmNodeRestrictions.get(node.id));						
						}
						checkedNodes.add(node);
						oldToSimplifiedJunctionNodeMap.put(otherNode.id, junctionNode);
						if (osmNodeRestrictions.containsKey(otherNode.id)) {
							osmNodeRestrictions.put(junctionNode.id, osmNodeRestrictions.get(otherNode.id));						
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
				double distance = 30;
				findCloseJunctionNodesWithSignals(node, node, junctionNodes, checkedNodes, distance, false);

				if (junctionNodes.size() == 4) {
					double repX = 0;
					double repY = 0;
					double leftTurnRadius = 0;
					OsmNode lastNode = junctionNodes.get(junctionNodes.size() - 1);
					for (OsmNode tempNode : junctionNodes) {
						repX += tempNode.coord.getX();
						repY += tempNode.coord.getY();
						leftTurnRadius += calcNode2NodeDistance(tempNode, lastNode);
						lastNode = tempNode;
					}
					leftTurnRadius /= junctionNodes.size();
					repX /= junctionNodes.size();
					repY /= junctionNodes.size();
					OsmNode junctionNode = new OsmNode(this.id, new Coord(repX, repY));
					signalizedOsmNodes.add(junctionNode.id);
					junctionNode.used = true;
					for (OsmNode tempNode : junctionNodes) {
						oldToSimplifiedJunctionNodeMap.put(tempNode.id, junctionNode);
						if (osmNodeRestrictions.containsKey(tempNode.id)) {
							osmNodeRestrictions.put(junctionNode.id, osmNodeRestrictions.get(tempNode.id));						
						}
						checkedNodes.add(tempNode);
					}
					addingNodes.add(junctionNode);
					this.turnRadii.put(junctionNode.id, leftTurnRadius);
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
		for (OsmWay way : this.ways.values()) {
			String oneway = way.tags.get(TAG_ONEWAY);
			if (oneway != null && !oneway.equals("-1") && !oneway.equals("no")) {
				// oneway in positive direction
				pushSignalOverShortWay(way, this.nodes.get(way.nodes.get(0)), this.nodes.get(way.nodes.get(1)));
			} else if (oneway != null && oneway.equals("-1")) {
				// oneway in negative direction
				pushSignalOverShortWay(way, this.nodes.get(way.nodes.get(1)), this.nodes.get(way.nodes.get(0)));
			}
			// TODO what happens for ways that are no oneway??
		}
	}

	private void pushSignalOverShortWay(OsmWay shortWay, OsmNode fromNode, OsmNode toNode) {
		if (shortWay.nodes.size() == 2 && calcNode2NodeDistance(fromNode, toNode) < SIGNAL_MERGE_DISTANCE) {
			if (fromNode.ways.size() == 2 && toNode.ways.size() > 2
					&& signalizedOsmNodes.contains(fromNode.id) && !signalizedOsmNodes.contains(toNode.id)) {
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
				OsmNode endPoint = null;
				String oneway = way.tags.get(TAG_ONEWAY);

				if (signalizedOsmNodes.contains(signalNode.id) && !isNodeAtJunction(signalNode)) {
					if ((oneway != null && !oneway.equals("-1") && !oneway.equals("no")) || oneway == null) {
						endPoint = this.nodes.get(way.nodes.get(way.nodes.size() - 1));
						if (signalizedOsmNodes.contains(endPoint.id) && isNodeAtJunction(endPoint)
								&& calcNode2NodeDistance(signalNode, endPoint) < SIGNAL_MERGE_DISTANCE)
							signalizedOsmNodes.remove(signalNode.id);
					}
					if ((oneway != null && !oneway.equals("yes") && !oneway.equals("true") && !oneway.equals("1")
							&& !oneway.equals("no")) || oneway == null) {
						endPoint = this.nodes.get(way.nodes.get(0));
						if (signalizedOsmNodes.contains(endPoint.id) && isNodeAtJunction(endPoint)
								&& calcNode2NodeDistance(signalNode, endPoint) < SIGNAL_MERGE_DISTANCE)
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
			for (int i = 1; i < way.nodes.size() - 1; i++) {
				OsmNode signalNode = this.nodes.get(way.nodes.get(i));
				OsmNode junctionNode = null;
				String oneway = way.tags.get(TAG_ONEWAY);

				if (signalizedOsmNodes.contains(signalNode.id) && !isNodeAtJunction(signalNode)) {
					if ((oneway != null && !oneway.equals("-1")) || oneway == null) {
						// positive oneway or no oneway
						OsmNode nextNode = this.nodes.get(way.nodes.get(i + 1));
						if (nextNode.ways.size() > 1) {
							// either a junction or the end point of this way where a new way starts
							junctionNode = nextNode;
						}
						if (i < way.nodes.size() - 2) {
							OsmNode secondNextNode = this.nodes.get(way.nodes.get(i + 2));
							if (crossingOsmNodes.contains(nextNode.id) && secondNextNode.ways.size() > 1) {
								// next node = pedestrian crossing. and the second next node is either a junction or the end of the way
								junctionNode = this.nodes.get(way.nodes.get(i + 2));
							}
						}
					}
					if (junctionNode != null && calcNode2NodeDistance(signalNode, junctionNode) < SIGNAL_MERGE_DISTANCE) {
						signalizedOsmNodes.remove(signalNode.id);
						signalizedOsmNodes.add(junctionNode.id);
					} else if ((oneway != null && oneway.equals("-1")) || oneway == null) {
						// "else" = no nearby junction found in positive direction. try reverse direction:
						// "if" = reverse oneway or no oneway (check opposite direction)
						OsmNode prevNode = this.nodes.get(way.nodes.get(i - 1));
						if (prevNode.ways.size() > 1) {
							// either a junction or the start point of this way where another way ends
							junctionNode = prevNode;
						}
						if (i > 1) {
							OsmNode secondPrevNode = this.nodes.get(way.nodes.get(i - 2));
							if (crossingOsmNodes.contains(prevNode.id) && secondPrevNode.ways.size() > 1) {
								// previous node = pedestrian crossing. and the second previous node is either a junction or the start of the way
								junctionNode = secondPrevNode;
							}
						}
						if (junctionNode != null && calcNode2NodeDistance(signalNode, junctionNode) < SIGNAL_MERGE_DISTANCE) {
							signalizedOsmNodes.remove(signalNode.id);
							signalizedOsmNodes.add(junctionNode.id);
						}
					}
				}
			}
		}
	}

//	private void simplifiyRoundaboutSignals() {
//		for (OsmWay way : this.ways.values()) {
//			String junction = way.tags.get(TAG_JUNCTION);
//			if (junction != null && junction.equals("roundabout")) {
//				for (int i = 1; i < way.nodes.size() - 1; i++) {
//					OsmNode junctionNode = this.nodes.get(way.nodes.get(i));
//					OsmNode otherNode = null;
//					if (signalizedOsmNodes.contains(junctionNode.id))
//						otherNode = findRoundaboutSignalNode(junctionNode, way, i);
//					if (otherNode != null) {
//						signalizedOsmNodes.remove(junctionNode.id);
//						signalizedOsmNodes.add(otherNode.id);
//						LOG.info("signal push around roundabout");
//						roundaboutNodes.put(otherNode.id, otherNode);
//					}
//				}
//			}
//		}
//	}

//	// TODO was macht diese methode?? signalNode und index wird gar nicht verwendet
//	private boolean isInfrontOfRoundabout(OsmNode signalNode, OsmWay way, int index) {
//		OsmNode endPoint = this.nodes.get(way.nodes.get(way.nodes.size() - 1));
//		if (endPoint.ways.size() == 2) { // TODO ist hier > 1 gemeint?
//			for (OsmWay tempWay : endPoint.ways.values()) {
//				if (!tempWay.equals(way))
//					// hier wechseln wir auf einen (den?) anderen way des endPoints -- warum?
//					way = tempWay;
//				// TODO das break soll doch bestimmt ins if...
//				break;
//			}
//			// endPoint vom neuen way
//			endPoint = this.nodes.get(way.nodes.get(way.nodes.size() - 1));
//			if (endPoint.ways.size() == 2) // TODO ist hier > 1 gemeint?
//				// wenn der auch an intersection endet, sind wir aus irgendeinem grund fertig
//				return false;
//			else {
//				if (roundaboutNodes.containsKey(endPoint.id)) {
//					LOG.info("Roundabout found @ " + endPoint.id);
//					return true;
//				}
//			}
//		} else {
//			if (roundaboutNodes.containsKey(endPoint.id)) {
//				LOG.info("Roundabout found @ " + endPoint.id);
//				return true;
//			}
//		}
//		return false;
//	}

	// TODO keine ahnung, warum das hier noetig ist. probiere es deshalb erstmal ohne
//	private OsmNode findRoundaboutSignalNode(OsmNode junctionNode, OsmWay way, int index) {
//		OsmNode otherNode = null;
//		for (int i = index + 1; i < way.nodes.size(); i++) {
//			otherNode = this.nodes.get(way.nodes.get(i));
//			if ((otherNode.ways.size() > 1 && !otherNode.endPoint) || (otherNode.ways.size() > 2 && otherNode.endPoint))
//				return otherNode;
//		}
//		// hier wird ein anderer way des endpoints gewaehlt
//		for (OsmWay tempWay : otherNode.ways.values()) {
//			if (!tempWay.equals(way))
//				way = tempWay;
//			// TODO das break soll doch bestimmt ins if...?!
//			break;
//		}
//		// alle nodes des anderen ways (wenn er auch roundabout ist) werden geprueft...
//		// TODO wenn er nicht roundabout ist, wird nicht der naechste way gewaehlt sondern einfach aufgehoert
//		String junction = way.tags.get(TAG_JUNCTION);
//		if (junction != null && junction.equals("roundabout")) {
//			for (int i = 0; i < way.nodes.size(); i++) {
//				otherNode = this.nodes.get(way.nodes.get(i));
//				if ((otherNode.ways.size() > 1 && !otherNode.endPoint)
//						|| (otherNode.ways.size() > 2 && otherNode.endPoint))
//					return otherNode;
//			}
//		}
//		return null;
//	}

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
		if (inLinksAngle > 3 / 4 * Math.PI && inLinksAngle < 5 / 4 * Math.PI) {
			if (!this.makePedestrianSignals) {
				this.systems.getSignalSystemData().remove(signalSystem.getId());
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
		SignalGroupSettingsData settingsFirst = null;
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
		if (firstLink == null)
			return;
		LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(firstLink.getId());
		LanesToLinkAssignment otherl2l = null;
		if (secondLink != null)
			otherl2l = lanes.getLanesToLinkAssignments().get(secondLink.getId());
		List<Lane> firstLanes = new ArrayList<Lane>();
		List<Lane> secondLanes = new ArrayList<Lane>();
		if (l2l != null) {
			for (Lane lane : l2l.getLanes().values()) {
				if (!((boolean) lane.getAttributes().getAttribute(IS_ORIG_LANE)))
					firstLanes.add(lane);
			}
		}

		if (otherl2l != null) {
			for (Lane lane : otherl2l.getLanes().values()) {
				if (!((boolean) lane.getAttributes().getAttribute(IS_ORIG_LANE)))
					secondLanes.add(lane);
			}
		}

		if (l2l != null) {
			for (Lane lane : l2l.getLanes().values()) {
				if (!((boolean) lane.getAttributes().getAttribute(IS_ORIG_LANE))) {
					List<Id<Lane>> nonCritLanes = new ArrayList<Id<Lane>>();
					List<Id<Lane>> critLanes = new ArrayList<Id<Lane>>();
					for (Lane otherLane : firstLanes) {
						if (!otherLane.equals(lane))
							nonCritLanes.add(otherLane.getId());
					}
					if (otherl2l != null) {
						for (Lane otherLane : secondLanes) {
							if (criticalSignalLanes != null && criticalSignalLanes.contains(otherLane))
								critLanes.add(otherLane.getId());
							else
								nonCritLanes.add(otherLane.getId());
						}
					}
					this.nonCritLanes.put(lane.getId(), nonCritLanes);
					int i = 1;
					for (Id<Lane> laneId : nonCritLanes) {
						lane.getAttributes().putAttribute(NON_CRIT_LANES + "_" + i, laneId.toString());
						i++;
					}
					if (!critLanes.isEmpty()) {
						i = 1;
						this.critLanes.put(lane.getId(), critLanes);
						for (Id<Lane> laneId : critLanes) {
							lane.getAttributes().putAttribute(CRIT_LANES + "_" + i, laneId.toString());
							i++;
						}
					}
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
		Lane origLane = lanes.getFactory().createLane(Id.create("Lane" + link.getId() + ".ol", Lane.class));
		lanes.getLanesToLinkAssignments().get(link.getId()).addLane(origLane);
		origLane.setCapacityVehiclesPerHour(0);
		origLane.setStartsAtMeterFromLinkEnd(link.getLength());
		origLane.setNumberOfRepresentedLanes(link.getNumberOfLanes());

		Lane rightLane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
				.get(Id.create("Lane" + link.getId() + "." + ((int) link.getNumberOfLanes()), Lane.class));
		rightLane.getAttributes().putAttribute(IS_ORIG_LANE, false);
		origLane.addToLaneId(rightLane.getId());
		origLane.setCapacityVehiclesPerHour(
				origLane.getCapacityVehiclesPerHour() + rightLane.getCapacityVehiclesPerHour());
		origLane.getAttributes().putAttribute(IS_ORIG_LANE, true);
		for (int i = (int) link.getNumberOfLanes() - 1; i > 0; i--) {
			Lane leftLane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
					.get(Id.create("Lane" + link.getId() + "." + i, Lane.class));
			origLane.addToLaneId(leftLane.getId());
			origLane.setCapacityVehiclesPerHour(
					origLane.getCapacityVehiclesPerHour() + leftLane.getCapacityVehiclesPerHour());
			if (rightLane.getToLinkIds().equals(leftLane.getToLinkIds())) {
				leftLane.setNumberOfRepresentedLanes(
						leftLane.getNumberOfRepresentedLanes() + rightLane.getNumberOfRepresentedLanes());
				leftLane.setCapacityVehiclesPerHour(
						leftLane.getCapacityVehiclesPerHour() + rightLane.getCapacityVehiclesPerHour());
				// log.info("Put together Lane " +
				// leftLane.getId().toString() + " and Lane " +
				// rightLane.getId().toString());
				LanesToLinkAssignment linkLanes = lanes.getLanesToLinkAssignments().get(link.getId());
				origLane.getToLaneIds().remove(rightLane.getId());
				linkLanes.getLanes().remove(rightLane.getId());
			}
			rightLane = leftLane;
			rightLane.getAttributes().putAttribute(IS_ORIG_LANE, false);
		}
	}

	/*
	 * Creates a Stack of Lanedirection informations for every Lane. These Stacks
	 * are stacked-up for all Lanes. Directions are saved as int
	 * placeholder-variables. The far right Lane is on top of the Stack.
	 * nschirrmacher on 170613
	 */

	private void createLaneStack(String turnLanes, Stack<Stack<Integer>> turnLaneStack, double nofLanes) {

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
				} else if (directionsPerLane[j].equals("none") || directionsPerLane[j].equals(null)) {
					tempDir = null;
				} else {
					tempDir = null;
					LOG.warn("Could not read Turnlanes! " + directionsPerLane[j]);
				}
				tempLane.push(tempDir);
			}
			turnLaneStack.push(tempLane);
		}
		// TODO rather give an error message than filling with null-lanes here:
		// fills up Stack with dummy Lanes if size of Stack does not match
		// number of Lanes
		Stack<Integer> tempLane = new Stack<Integer>();
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
			LinkVector inLink = new LinkVector(inLinks.get(i));
			inLinkVectors.add(inLink);
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

	/*
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
			Assert.equals(laneStack.size(), (int)link.getNumberOfLanes());
			for (int i = (int) link.getNumberOfLanes(); i > 0; i--) {
				Lane lane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
						.get(Id.create("Lane" + link.getId() + "." + i, Lane.class));
				setToLinksForLaneWithTurnLanes(lane, laneStack.pop(), linkVectors, (laneStack.size() == 1));
			}
		} else {
			setToLinksForLanesDefault(link, linkVectors);
		}
	}

	// Source: HBS 2001
	private double getRadiusCapacityReductionFactor(double radius) {
		if (radius <= 10)
			return 0.85;
		if (radius <= 15)
			return 0.9;
		return 1;
	}

	private void setToLinksForLanesDefault(Link link, List<LinkVector> toLinks) {
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
		if (toLinks.get(straightLink).getRotation() < (1 - THROUGHLINK_ANGLE_TOLERANCE) * Math.PI
				|| toLinks.get(straightLink).getRotation() > (1 + THROUGHLINK_ANGLE_TOLERANCE) * Math.PI) {
			straightestLink = straightLink;
			straightLink = -1;
		}
		if (toLinks.get(reverseLink).getRotation() > THROUGHLINK_ANGLE_TOLERANCE * Math.PI
				&& toLinks.get(reverseLink).getRotation() < (2 - THROUGHLINK_ANGLE_TOLERANCE) * Math.PI)
			reverseLink = -1;
		if (toLinks.size() == 1) {
			lanes.getLanesToLinkAssignments().remove(link.getId());
			return;
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
				lane.getAttributes().putAttribute(TO_LINK_REFERENCE, OUTER_LANE_TYPE);
				
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
				lane.getAttributes().putAttribute(TO_LINK_REFERENCE, OUTER_LANE_TYPE);
			}

			if (!OUTER_LANE_TYPE.equals(OuterLaneRestriction.RESTRICTIVE)) {
				// add straight turn to right lane
				Lane lane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
						.get(Id.create("Lane" + link.getId() + "." + ((int) link.getNumberOfLanes()), Lane.class));
				if (reverseLink != 0)
					lane.addToLinkId(toLinks.get(1).getLink().getId());
				else if (straightLink != 1)
					lane.addToLinkId(toLinks.get(2).getLink().getId());
				lane.setAlignment(-1);
				if (OUTER_LANE_TYPE.equals(OuterLaneRestriction.NON_RESTRICTIVE)) {
					// add straight turn to left lane
					lane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
							.get(Id.create("Lane" + link.getId() + "." + "1", Lane.class));
					if (straightLink < toLinks.size() - 2) {
						if (reverseLink == toLinks.size() - 1)
							lane.addToLinkId(toLinks.get(toLinks.size() - 3).getLink().getId());
						else
							lane.addToLinkId(toLinks.get(toLinks.size() - 2).getLink().getId());
					}
					lane.setAlignment(1);
				}
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
					lane.getAttributes().putAttribute(TO_LINK_REFERENCE, MIDDLE_LANE_TYPE);
				}
			}

			if (MIDDLE_LANE_TYPE.equals(MiddleLaneRestriction.REGULATION_BASED)) {
				// add all other out-links except u-turn to middle lanes
				for (int i = (int) link.getNumberOfLanes() - 1; i > 1; i--) {
					Lane lane = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes()
							.get(Id.create("Lane" + link.getId() + "." + i, Lane.class));
					if (midLink > 0 && (midLink - 1 != reverseLink || !this.allowUTurnAtLeftLaneOnly))
						lane.addToLinkId(toLinks.get(midLink - 1).getLink().getId());
					if (midLink < toLinks.size() - 1 && (midLink + 1 != reverseLink || !this.allowUTurnAtLeftLaneOnly))
						lane.addToLinkId(toLinks.get(midLink + 1).getLink().getId());
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
	}

	// Fills Lanes with turn:lane informations
	private void setToLinksForLaneWithTurnLanes(Lane lane, Stack<Integer> laneStack, List<LinkVector> toLinks,
			boolean singleLaneOfTheLink) {
		lane.getAttributes().putAttribute(TO_LINK_REFERENCE, "OSM-Information");
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
		if (reverseLink.getRotation() < (2 - THROUGHLINK_ANGLE_TOLERANCE) * Math.PI
				&& reverseLink.getRotation() > THROUGHLINK_ANGLE_TOLERANCE * Math.PI)
			reverseLink = null;
		int it = 1;
		while (!laneStack.isEmpty()) {

			Integer tempDir = laneStack.pop();
			List<LinkVector> tempLinks = new ArrayList<LinkVector>();
			// removeLinks.clear();
			// log.info("Trying to Fill " + lane.getId().toString() + " with
			// Direction: " + tempDir + " with #ofToLinks: " + toLinks.size() );
			if (tempDir == null) { // no direction for lane available
				if (MIDDLE_LANE_TYPE.equals(MiddleLaneRestriction.REALISTIC))
					// only add straight to-link
					lane.addToLinkId(throughLink.getLink().getId());
				else {
					for (LinkVector lvec : toLinks) {
						// add all to-links except u-turn
						if (!lvec.equals(reverseLink) || !this.allowUTurnAtLeftLaneOnly)
							lane.addToLinkId(lvec.getLink().getId());
					}
				}
				lane.setAlignment(0);
				lane.getAttributes().putAttribute(TO_LINK_REFERENCE, MIDDLE_LANE_TYPE);
				break;
			}
			if (tempDir < 0 && tempDir > -5) { // all right directions (right,
				// slight_right,sharp_right)
				for (LinkVector lvec : tempLinks) {
					if (lvec.dirTheta < (1 - THROUGHLINK_ANGLE_TOLERANCE) * Math.PI)
						tempLinks.add(lvec);
				}
				if (tempLinks.size() == 1) { // if there is just one "right"
					// link, take it
					lane.addToLinkId(tempLinks.get(0).getLink().getId());
				} else if (tempLinks.size() == 2) {
					if (tempDir == -1) { // lane direction: "right"
						for (LinkVector lvec : tempLinks)
							lane.addToLinkId(lvec.getLink().getId());
					}
					if (tempDir == -2) { // lane direction: "slight_right"
						if (tempLinks.get(0).dirTheta < Math.PI / 2)
							lane.addToLinkId(tempLinks.get(1).getLink().getId());
						else
							lane.addToLinkId(tempLinks.get(0).getLink().getId());
					}
					if (tempDir == -3) // lane direction: "sharp_right"
						lane.addToLinkId(tempLinks.get(0).getLink().getId());
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
					if (lvec.dirTheta > (1 + THROUGHLINK_ANGLE_TOLERANCE) * Math.PI)
						tempLinks.add(lvec);
				}
				if (tempLinks.size() == 1) { // if there is just one "left"
					// link, take it
					lane.addToLinkId(tempLinks.get(0).getLink().getId());
				} else if (tempLinks.size() == 2) {
					if (tempDir == 1) { // lane direction: "left"
						for (LinkVector lvec : tempLinks)
							// TODO does this make sense? it means:
							// add link as to-link, if it is not the reverse link OR the link has only one lane OR u-turn is allowed at any lane
							if (!lvec.equals(reverseLink) || singleLaneOfTheLink || !this.allowUTurnAtLeftLaneOnly)
								lane.addToLinkId(lvec.getLink().getId());
					}
					if (tempDir == 2) { // lane direction: "slight_left"
						if (tempLinks.get(1).dirTheta > 3 * Math.PI / 2 || !tempLinks.get(1).equals(reverseLink))
							lane.addToLinkId(tempLinks.get(0).getLink().getId());
						else
							lane.addToLinkId(tempLinks.get(1).getLink().getId());
					}
					if (tempDir == 3) // lane direction: "sharp_left"
						lane.addToLinkId(tempLinks.get(1).getLink().getId());
				} else if (tempLinks.size() > 2) {
					for (LinkVector lvec : tempLinks)
						if (!lvec.equals(reverseLink) || singleLaneOfTheLink || !this.allowUTurnAtLeftLaneOnly)
							lane.addToLinkId(lvec.getLink().getId());
				} else {
					lane.addToLinkId(toLinks.get(toLinks.size() - 1).getLink().getId());
				}
				if (alignmentAnte == 0)
					lane.setAlignment(1);
				else
					lane.setAlignment(2);
			}
			if (tempDir == 0 || tempDir == 4 || tempDir == -5) { // lane directions that have to lead to a forward link
																	// (through, merge_to_left,merge_to_right)
				alignmentAnte = lane.getAlignment(); // look for the most "forward" link (closest to 180° or pi) and
														// take it

				lane.addToLinkId(throughLink.getLink().getId());
				if (alignmentAnte == -2)
					lane.setAlignment(-1);
			}
			if (tempDir == 5) { // lane direction: "reverse"
				// look for the most "backward" link (furthest from 180° or pi)
				// and take it
				alignmentAnte = lane.getAlignment();
				if (alignmentAnte == 0 && lane.getToLinkIds().isEmpty())
					alignmentAnte = -10;
				lane.addToLinkId(reverseLink.getLink().getId());
				if (alignmentAnte == 0)
					lane.setAlignment(1);
				else
					lane.setAlignment(2);

			}
			if (lane.getToLinkIds().isEmpty()) {
				LOG.warn("No toLink could be found for " + lane.getId());
				lane.addToLinkId(toLinks.get(0).getLink().getId());
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
		LinkVector fromLink = new LinkVector(link);
		for (int i = 0; i < toLinks.size(); i++) {
			LinkVector toLink = new LinkVector(toLinks.get(i));
			toLink.calculateRotation(fromLink);
			toLinkList.add(toLink);
		}
		Collections.sort(toLinkList);
		return toLinkList;
	}

	private void removeRestrictedLinks(Link fromLink, List<LinkVector> toLinks) {
		OsmNode toNode = nodes.get(Long.valueOf(fromLink.getToNode().getId().toString()));
		for (OsmRelation restriction : osmNodeRestrictions.get(toNode.id)) {
			if (Long.valueOf(fromLink.getAttributes().getAttribute(ORIG_ID).toString()) == restriction.fromRestricted.id) {
				if (restriction.restrictionValue == false) {
					LinkVector lvec2remove = null;
					for (LinkVector linkVector : toLinks) {
						if (Long.valueOf(linkVector.getLink().getAttributes().getAttribute(ORIG_ID)
								.toString()) == restriction.toRestricted.id) {
							lvec2remove = linkVector;
							break;
						}
					}
					toLinks.remove(lvec2remove);
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
			createLaneStack(turnLanesOsm, turnLanesOfThisLink, l.getNumberOfLanes());
			if (l.getNumberOfLanes() < turnLanesOfThisLink.size()) {
				// TODO dies stellt die info der turn lanes über die info der #lanes.
				// konsistent? adapt capacity too?
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

	private double calcNode2NodeDistance(OsmNode node1, OsmNode node2) {
		double x = node1.coord.getX() - node2.coord.getX();
		double y = node1.coord.getY() - node2.coord.getY();
		double distance = Math.sqrt(x * x + y * y);
		return distance;
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
					// TODO just save tags here (as for ways) and evaluate in endTags??
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
			
			// TODO check this!
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

		private void calculateTheta() {
			if (this.y >= 0) {
				this.theta = Math.atan2(this.y, this.x);
			} else {
				this.theta = 2 * Math.PI + Math.atan2(this.y, this.x);
			}
		}

		public void calculateRotation(LinkVector linkVector) {
			if (this.theta <= Math.PI)
				this.dirTheta = this.theta - linkVector.getAlpha() + Math.PI;
			else
				this.dirTheta = this.theta - linkVector.getAlpha() - Math.PI;
			if (this.dirTheta < 0) {
				this.dirTheta += 2 * Math.PI;
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
				rotation += 2 * Math.PI;
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

	// TODO consider moving this out in some utils directory
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
