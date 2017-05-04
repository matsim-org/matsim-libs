/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesFactory;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Envelope;

/**
 * This class converts a scenario without signals into a scenario with signals at all intersections.
 * The user may choose between (1) a simpler approach without lanes, where only one signal is created for every link
 * and (2) a more detailed approach with lanes, where a signal is created for every possible turning relation.
 * 
 * @author tthunig
 */
public class SignalizeScenario {
	
	private static final Logger LOG = Logger.getLogger(SignalizeScenario.class);
	
	private Scenario scenario;
	private Envelope bbEnv;
	private String signalControlIdentifier = DefaultPlanbasedSignalSystemController.IDENTIFIER;
	private boolean overwriteSignals = true;
	
	private SignalsData signalsData;
	private List<Id<Node>> signalizedNodes = new LinkedList<>();
	
	/**
	 * constructor.
	 * 
	 * @param scenario contains a network, but no signal or lane information
	 */
	public SignalizeScenario(Scenario scenario) {
		this.scenario = scenario;
	}
	
	public void setSignalControlIdentifier(String signalControlIdentifier){
		this.signalControlIdentifier = signalControlIdentifier;
	}
	
	public void setBoundingBox(String bbShapeFile){
		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(bbShapeFile);
		SimpleFeature f = shapeReader.getFeatureSet().iterator().next();
		BoundingBox bb = f.getBounds();
		bbEnv = new Envelope(bb.getMinX(), bb.getMaxX(), bb.getMinY(), bb.getMaxY());
	}
	
	public void setOverwriteSignals(boolean overwriteSignals){
		this.overwriteSignals = overwriteSignals;
	}
	
	/**
	 * Creates a signal system at every intersection which contains a signal on every link. 
	 * No lanes are used, i.e. from every signal one can reach all outgoing links.
	 * The signal information is saved in the given Scenario.
	 */
	public void createSignalsForAllLinks(){
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		if (!signalsConfigGroup.isUseSignalSystems()){
			signalsConfigGroup.setUseSignalSystems(true);
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
		} 
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		initSignalizedNodes();
	
		createSystemAtEveryNode(false);
		createGroupForEverySignal();
		createControlForEveryGroup();
	}

	/**
	 * On every link a lane is created for each outgoing link.
	 * Afterwards a signal system is created at every intersection which contains 
	 * a signal on every lane, i.e. for every turning relation.
	 * The lane and signal information is saved in the given Scenario.
	 */
	public void createSignalsAndLanesForAllTurnings(){
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), 
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		if (!signalsConfigGroup.isUseSignalSystems()){
			signalsConfigGroup.setUseSignalSystems(true);
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
		} 
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		scenario.getConfig().qsim().setUseLanes(true);
		
		initSignalizedNodes();
		
		createLanesForEveryInLinkOutLinkPair();
		createSystemAtEveryNode(true);
		createGroupForEverySignal();
		createControlForEveryGroup();
	}

	/**
	 * create a list with all signalized nodes.
	 * is necessary when overwriteSignals is set to false.
	 */
	private void initSignalizedNodes() {
		if (!overwriteSignals){
			for (SignalSystemData signalSystem : signalsData.getSignalSystemsData().getSignalSystemData().values()){
				for (SignalData signal : signalSystem.getSignalData().values()){
					// take node of the first signal of the system, i.e. assume all systems signal belong to the same node
					signalizedNodes.add(scenario.getNetwork().getLinks().get(signal.getLinkId()).getToNode().getId());
					break;
				}
			}
		}
	}

	/**
	 * for every link with more than one outgoing link, this method creates an original lane and outgoing lanes for all turning possibilities.
	 * if a bounding box envelope is used, lanes are only created for links that lead to nodes inside the envelope.
	 */
	private void createLanesForEveryInLinkOutLinkPair() {
		Lanes lanes = scenario.getLanes();
		LanesFactory fac = lanes.getFactory();
		if (!lanes.getLanesToLinkAssignments().isEmpty() && this.overwriteSignals){
			LOG.warn("This class will overwrite existing lane information");
		}
		
		for (Link inLink : scenario.getNetwork().getLinks().values()) {
			// to node lies inside the bounding box envelope
			if (bbEnv != null && bbEnv.contains(MGC.coord2Coordinate(inLink.getToNode().getCoord()))) {
				// Create lanes for every link with more than one outgoing link
				if ((inLink.getToNode().getOutLinks() != null) && (inLink.getToNode().getOutLinks().size() > 1)){	
					// except the link already contains lane information and overwriteSignals is set to false
					// or if there already exists a signal at the links to node
					if (!overwriteSignals && lanes.getLanesToLinkAssignments().containsKey(inLink.getId())
							|| signalizedNodes.contains(inLink.getToNode().getId())) {
						continue;
					} // else, i.e. the link either does not contain lane information or overwriteSignals is set to true:
					LanesToLinkAssignment linkLanes = fac.createLanesToLinkAssignment(inLink.getId());
					lanes.addLanesToLinkAssignment(linkLanes);
					List<Id<Lane>> linkLaneIds = new ArrayList<>();
					for (Link outLink : inLink.getToNode().getOutLinks().values()) {
						// TODO decide for suitable capacity, lane length and alignment. use dg utils or lane v1 to v2 converter logik?
						double laneCap = inLink.getCapacity();
						double laneStart_mFromLinkEnd = inLink.getLength() / 2;
						int alignment = 0;
						Id<Lane> laneId = Id.create(inLink.getId() + "." + outLink.getId(), Lane.class);
						LanesUtils.createAndAddLane(linkLanes, fac, laneId, laneCap, laneStart_mFromLinkEnd, alignment, 1, Collections.singletonList(outLink.getId()), null);
						linkLaneIds.add(laneId);
					}
					// create original lane
					LanesUtils.createAndAddLane(linkLanes, fac, Id.create(inLink.getId() + ".ol", Lane.class), inLink.getCapacity(), inLink.getLength(), 0, 1, null, linkLaneIds);
				}
			}
		}
	}

	/**
	 * create a signal system at every node inside the bounding box envelope.
	 * creates signals at all links or lanes (if useLanes is true)
	 * @param useLanes
	 */
	private void createSystemAtEveryNode(boolean useLanes) {
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory fac = signalSystems.getFactory();
		if (!signalSystems.getSignalSystemData().isEmpty() && this.overwriteSignals){
			LOG.warn("This class will overwrite existing signal data");
		}
		
		for (Node node : scenario.getNetwork().getNodes().values()){
			// node lies inside the bounding box envelope
			if (bbEnv != null && bbEnv.contains(MGC.coord2Coordinate(node.getCoord()))) {

				// Create system for every node with outgoing and ingoing links
				if ((node.getOutLinks() != null) && (!node.getOutLinks().isEmpty()) && (node.getInLinks() != null) && (!node.getInLinks().isEmpty())) {
					Id<SignalSystem> signalSystemId = Id.create("signalSystem" + node.getId(), SignalSystem.class);
					
					// except the node already contains system information and overwriteSignals is set to false
					if (!overwriteSignals && signalizedNodes.contains(node.getId())) {
						continue;
					} // else, i.e. the node either does not contain system information or overwriteSignals is set to true:
					
					// if (node.getOutLinks().size() == 1 && node.getInLinks().size() == 1
					// && node.getOutLinks().get(0).getToNode().equals(node.getInLinks().get(0).getFromNode())){
					// // do not create signals at dead end streets
					// continue;
					// }

					// create signal system
					SignalSystemData signalSystem = fac.createSignalSystemData(signalSystemId);
					signalSystems.addSignalSystemData(signalSystem);

					// go through all ingoing links
					for (Id<Link> linkId : node.getInLinks().keySet()) {
						if (useLanes && scenario.getLanes().getLanesToLinkAssignments().containsKey(linkId)) {
							// go through all ingoing lanes
							for (Lane lane : scenario.getLanes().getLanesToLinkAssignments().get(linkId).getLanes().values()) {
								// do not create signals for original lanes
								if (!lane.getId().toString().endsWith(".ol")) {
									SignalData signal = fac.createSignalData(Id.create("signal" + linkId + "." + lane.getId(), Signal.class));
									signalSystem.addSignalData(signal);
									signal.setLinkId(linkId);
									signal.addLaneId(lane.getId());
									for (Id<Link> toLinkId : lane.getToLinkIds()) {
										signal.addTurningMoveRestriction(toLinkId);
									}
								}
							}
						} else { // i.e. no lanes
							SignalData signal = fac.createSignalData(Id.create("signal" + linkId, Signal.class));
							signalSystem.addSignalData(signal);
							signal.setLinkId(linkId);
						}
					}
				}
			}
		}
	}

	private void createGroupForEverySignal() {
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		for (SignalSystemData system : signalsData.getSignalSystemsData().getSignalSystemData().values()) {
			// create single element groups for the systems signal if no groups exist yet or overwriteSignals is set to true anyways
			if (overwriteSignals || !signalGroups.getSignalGroupDataBySignalSystemId().containsKey(system.getId())){
				SignalUtils.createAndAddSignalGroups4Signals(signalGroups, system);
			}
		}
	}

	/**
	 * Create an all day green signal control for every signal group.
	 */
	private void createControlForEveryGroup() {
		int CYCLE_TIME = 90;
		
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory fac = signalControl.getFactory();
		
		// create a signal control for all signal systems
		for (Id<SignalSystem> signalSystemId : signalSystems.getSignalSystemData().keySet()) {
			// except the system already contains control data and overwriteSignals is set to false
			if (!overwriteSignals && signalControl.getSignalSystemControllerDataBySystemId().containsKey(signalSystemId)) {
				continue;
			} // else, i.e. there is either no control data for this system or overwriteSignals is set to true:
			
			SignalSystemControllerData signalSystemControl = fac.createSignalSystemControllerData(signalSystemId);
			// create a default plan for the signal system (with defined cycle time and offset 0)
			SignalPlanData signalPlan = SignalUtils.createSignalPlan(fac, CYCLE_TIME, 0);
			signalSystemControl.addSignalPlanData(signalPlan);
			signalSystemControl.setControllerIdentifier(signalControlIdentifier);
			// add the systems control to the control container
			signalControl.addSignalSystemControllerData(signalSystemControl);

			// specify signal group settings for all signal groups of this signal system
			for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystemId).values()) {
				/* TODO decide which kind of signal control to create
				 * devide 90 sec into equal time slots for all directions?! 
				 * problem: corresponding directions cannot be found easily
				 * currently used: all day green: */
				int onset = 0;
				int dropping = CYCLE_TIME-1;
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroup.getId(), onset, dropping));
			}
		}
	}
	
}
