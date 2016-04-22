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
package scenarios.illustrative.braess.createInput;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactoryImpl;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;

import playground.dgrether.signalsystems.sylvia.data.DgSylviaPreprocessData;
import scenarios.illustrative.braess.createInput.TtCreateBraessNetworkAndLanes.LaneType;

/**
 * Class to create signals (signal systems, signal groups and signal control)
 * for the Braess scenario.
 * 
 * @author tthunig
 * 
 */
public final class TtCreateBraessSignals {

	private static final Logger log = Logger
			.getLogger(TtCreateBraessSignals.class);
	
	public enum SignalControlType{
		NONE, ALL_GREEN, ONE_SECOND_Z, ONE_SECOND_SO, GREEN_WAVE_Z, GREEN_WAVE_SO, SIGNAL4_ONE_SECOND_Z, SIGNAL4_ONE_SECOND_SO, SIGNAL4_SYLVIA_V2Z, SIGNAL4_SYLVIA_Z2V
	}
	
	private static final int CYCLE_TIME = 60;
	private static final int INTERGREEN_TIME = 0;

	private Scenario scenario;
	
	private boolean middleLinkExists = true;
	private LaneType laneType;
	private SignalControlType signalType;
	
	// travel time for the middle link
	private int linkTTMid;
	// travel time for the middle route links
	private int linkTTSmall; // [s]
	// travel time for the two remaining outer route links
	private int linkTTBig; // [s]

	public TtCreateBraessSignals(Scenario scenario) {
		this.scenario = scenario;

		checkNetworkProperties();
	}

	/**
	 * Checks several properties of the network.
	 */
	private void checkNetworkProperties() {

		// check whether the network contains the middle link
		if (!this.scenario.getNetwork().getLinks().containsKey(Id.createLinkId("3_4")))
			this.middleLinkExists = false;
		
		// calculate link travel times (necessary for green wave settings)
		Link currentLink;
		// get link travel time of the middle link
		currentLink = scenario.getNetwork().getLinks().get(Id.createLinkId("3_4"));
		linkTTMid = (int) Math.ceil(currentLink.getLength() / currentLink.getFreespeed());
		// get link travel time of the middle route links besides the middle link (add inflow link travel time if inflow link exists)
		if (scenario.getNetwork().getLinks().containsKey(Id.createLinkId("2_3"))){
			currentLink = scenario.getNetwork().getLinks().get(Id.createLinkId("2_3"));
			linkTTSmall = (int) Math.ceil(currentLink.getLength() / currentLink.getFreespeed());
		} else { // inflow link exists
			currentLink = scenario.getNetwork().getLinks().get(Id.createLinkId("2_23"));
			linkTTSmall = (int) Math.ceil(currentLink.getLength() / currentLink.getFreespeed());
			currentLink = scenario.getNetwork().getLinks().get(Id.createLinkId("23_3"));
			linkTTSmall += (int) Math.ceil(currentLink.getLength() / currentLink.getFreespeed());
		}		
		// get link travel time of the two remaining outer route links
		currentLink = scenario.getNetwork().getLinks().get(Id.createLinkId("3_5"));
		linkTTBig = (int) Math.ceil(currentLink.getLength() / currentLink.getFreespeed());
	}

	public void createSignals() {
		
		if (signalType.equals(SignalControlType.NONE)){
			log.error("This method should not be called if the signal type NONE is used.");
		}
		if (!this.middleLinkExists && !this.signalType.equals(SignalControlType.ALL_GREEN)){
			log.error("No provided signal control besides ALL_GREEN makes sense in a scenario without the middle link.");
		}
		
		createSignalSystems();
		createSignalGroups();
		createSignalControl();
	}

	/**
	 * Creates signal systems depending on the network situation.
	 * 
	 * If realistic lanes are used they already give the turning move restrictions such that no
	 * further turning move restrictions are necessary for the signal definitions. If only trivial
	 * or no lanes are used this method adds the turning move restrictions to the signals.
	 */
	private void createSignalSystems() {

		if (signalType.equals(SignalControlType.SIGNAL4_ONE_SECOND_SO) || 
				signalType.equals(SignalControlType.SIGNAL4_ONE_SECOND_Z) || 
				signalType.equals(SignalControlType.SIGNAL4_SYLVIA_V2Z) ||
				signalType.equals(SignalControlType.SIGNAL4_SYLVIA_Z2V)) {
			// create only one signal system at node 4
			createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(4)));
		} else {
			// create signal systems at nodes 2, 3, 4 and 5
			for (Node node : this.scenario.getNetwork().getNodes().values()) {
				switch (node.getId().toString()) {
				case "2":
				case "3":
				case "4":
				case "5":
					createSignalSystemAtNode(node);
					break;
				default:
					break;
				}
			}
		}
	}

	private void createSignalSystemAtNode(Node node) {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory fac = new SignalSystemsDataFactoryImpl();
		
		// create signal system
		SignalSystemData signalSystem = fac.createSignalSystemData(Id.create("signalSystem" + node.getId(), SignalSystem.class));
		signalSystems.addSignalSystemData(signalSystem);
		
		// create a signal for every inLink outLink pair
		for (Id<Link> inLinkId : node.getInLinks().keySet()){
			int outLinkCounter = 0;
			for (Id<Link> outLinkId : node.getOutLinks().keySet()){
				outLinkCounter++;
				SignalData signal = fac.createSignalData(Id.create("signal" + inLinkId + "." + outLinkCounter, Signal.class));
				signalSystem.addSignalData(signal);
				signal.setLinkId(inLinkId);
				
				// add turning move restrictions and lanes if necessary
				switch (this.laneType) {
				case TRIVIAL:
					LanesToLinkAssignment20 linkLanes = this.scenario.getLanes().getLanesToLinkAssignments().get(inLinkId);
					// the link only contains one lane (the trivial lane)
					signal.addLaneId(linkLanes.getLanes().firstKey());
				case NONE:
					// turning move restrictions are necessary for TRIVIAL and NONE
					signal.addTurningMoveRestriction(outLinkId);
					break;
				case REALISTIC:
					// find and add the correct lane if it exists
					linkLanes = this.scenario.getLanes().getLanesToLinkAssignments().get(inLinkId);
					if (linkLanes != null) {
						for (Lane lane : linkLanes.getLanes().values()) {
							if (lane.getToLinkIds() != null && !lane.getToLinkIds().isEmpty() && lane.getToLinkIds().contains(outLinkId))
								// correct lane found
								signal.addLaneId(lane.getId());
						}
					}
					break;
				}
			}
		}
	}

	private void createSignalGroups() {

		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();

		// create signal groups for each signal system
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()) {
			SignalUtils.createAndAddSignalGroups4Signals(signalGroups, system);
		}
	}

	private void createSignalControl() {

		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory fac = new SignalControlDataFactoryImpl();
		
		// create a temporary, empty signal control object needed in case sylvia is used
		SignalControlData tmpSignalControl = new SignalControlDataImpl();		
		
		// create a signal control for all signal systems
		for (SignalSystemData signalSystem : signalSystems.getSignalSystemData().values()) {

			SignalSystemControllerData signalSystemControl = fac.createSignalSystemControllerData(signalSystem.getId());

			// create a default plan for the signal system (with defined cycle time and offset 0)
			SignalPlanData signalPlan = SignalUtils.createSignalPlan(fac, CYCLE_TIME, 0);
			
			signalSystemControl.addSignalPlanData(signalPlan);
			signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			// add the signalSystemControl to the final or temporary, respectively, signalControl 
			if (this.signalType.equals(SignalControlType.SIGNAL4_SYLVIA_Z2V) || this.signalType.equals(SignalControlType.SIGNAL4_SYLVIA_V2Z)){
				tmpSignalControl.addSignalSystemControllerData(signalSystemControl);
			} else {
				signalControl.addSignalSystemControllerData(signalSystemControl);
			}
			
			// specify signal group settings for all signal groups of this signal system
			for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
				
				switch (this.signalType){
				case GREEN_WAVE_Z:
					// create signal control such that the middle route is preferred
					createGreenWaveZSignalControl(fac, signalPlan, signalGroup.getId());
					break;
				case GREEN_WAVE_SO:
					// create signal control such that the outer routes are preferred
					createGreenWaveSOSignalControl(fac, signalPlan, signalGroup.getId());
					break;
				case ALL_GREEN:
				case ONE_SECOND_Z:
				case ONE_SECOND_SO:
					// create all day green onset and dropping
					// and change it afterwards in case of ONE_SECOND_* control
					signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(
							fac, signalGroup.getId(), 0, CYCLE_TIME));
					break;
				case SIGNAL4_ONE_SECOND_SO:
					createSignal4SettingFirstZ(fac, signalPlan, signalGroup.getId(), 59);
					break;
				case SIGNAL4_ONE_SECOND_Z:
					createSignal4SettingFirstZ(fac, signalPlan, signalGroup.getId(), 1);
					break;
				case SIGNAL4_SYLVIA_V2Z:
					// create a basis signal plan
					createSignal4SettingFirstV(fac, signalPlan, signalGroup.getId(), 55);
					break;
				case SIGNAL4_SYLVIA_Z2V:
					// create a basis signal plan
					createSignal4SettingFirstZ(fac, signalPlan, signalGroup.getId(), 55);
					break;
				default:
					break;
				}
			}
		}
		
		// overall adoptions
		switch (this.signalType){
		
		// change the overall signal control to ONE_SECOND_Z or ONE_SECOND_SO respectively if necessary
		case ONE_SECOND_Z:
			// change all day green signal control such that
			// the middle route gets only green for one second a cycle
			changeAllGreenSignalControlTo1Z();
			break;
		case ONE_SECOND_SO:
			// change all day green signal control such that
			// the outer routes get only green for one second a cycle
			changeAllGreenSignalControlTo1SO();
			break;
		
		// convert basis fixed time plan to sylvia plan
		case SIGNAL4_SYLVIA_V2Z:
		case SIGNAL4_SYLVIA_Z2V:
			// create the final sylvia signal control with information of the temporary signal control
			DgSylviaPreprocessData.convertSignalControlData(tmpSignalControl, signalControl);
			break;
		
		default:
			break;
		}
	}

	private void createGreenWaveZSignalControl(SignalControlDataFactory fac, SignalPlanData signalPlan, Id<SignalGroup> signalGroupId) {
		int onset = 0;
		int dropping = 0;
		int signalSystemOffset = 0;
		// set onset and dropping for each signal group and offset for each signal system
		switch (signalGroupId.toString()){
		case "signal1_2.1": // signal for turning left (upper or middle route) at node 2
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
			signalSystemOffset = 0;
			break;
		case "signal1_2.2": // signal for turning right (lower route) at node 2
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = 0;
			break;
		case "signal23_3.1":
		case "signal2_3.1": // signals for turning right (middle route) at node 3
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
			signalSystemOffset = linkTTSmall;
			break;
		case "signal23_3.2":
		case "signal2_32": // signals for going straight on (upper route) at node 3
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = linkTTSmall;
			break;
		case "signal3_4.1": // signal at link 3_4 (middle route at node 4)
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
			signalSystemOffset = linkTTSmall + linkTTMid;
			break;
		case "signal24_4.1":
		case "signal2_4.1": // signals at link 2_4 (lower route at node 4)
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = linkTTSmall + linkTTMid;
			break;
		case "signal45_5.1":
		case "signal4_5.1": // signals at link 4_5 (lower or middle route at node 5)
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
			signalSystemOffset = linkTTSmall + linkTTMid + linkTTSmall;
			break;
		case "signal3_5.1": // signal at link 3_5 (upper route at node 5)
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = linkTTSmall + linkTTMid + linkTTSmall;
			break;
		default:
			log.error("Signal group id " + signalGroupId + " is not known.");
			break;
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
	}

	private void createGreenWaveSOSignalControl(SignalControlDataFactory fac, SignalPlanData signalPlan, Id<SignalGroup> signalGroupId) {
		int onset = 0;
		int dropping = 0;
		int signalSystemOffset = 0;
		// set onset and dropping for each signal group and offset for each signal system
		switch (signalGroupId.toString()){
		case "signal1_2.1": // signal for turning left (upper or middle route) at node 2
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
			signalSystemOffset = 0;
			break;
		case "signal1_2.2": // signal for turning right (lower route) at node 2
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = 0;
			break;
		case "signal23_3.1":
		case "signal2_3.1": // signals for turning right (middle route) at node 3
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = linkTTSmall;
			break;
		case "signal23_3.2":
		case "signal2_32": // signals for going straight on (upper route) at node 3
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
			signalSystemOffset = linkTTSmall;
			break;
		case "signal3_4.1": // signal at link 3_4 (middle route at node 4)
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
			signalSystemOffset = linkTTBig;
			break;
		case "signal24_4.1":
		case "signal2_4.1": // signals at link 2_4 (lower route at node 4)
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = linkTTBig;
			break;
		case "signal45_5.1":
		case "signal4_5.1": // signals at link 4_5 (lower or middle route at node 5)
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = linkTTSmall + linkTTBig;
			break;
		case "signal3_5.1": // signal at link 3_5 (upper route at node 5)
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
			signalSystemOffset = linkTTSmall + linkTTBig;
			break;
		default:
			log.error("Signal group id " + signalGroupId + " is not known.");
			break;
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
	}

	private void createSignal4SettingFirstZ(SignalControlDataFactory fac, SignalPlanData signalPlan, Id<SignalGroup> signalGroupId, int second2SwitchFromZtoV) {
		int onset = 0;
		int dropping = 0;
		// set onset and dropping depending on the signal group and signal control type
		switch (signalGroupId.toString()){
		case "signal3_4.1": // signal at node 4 for the middle route
			// set second2SwitchFromZtoV1 seconds green for the middle route
			onset = 0;
			dropping = second2SwitchFromZtoV;
			break;
		case "signal24_4.1":
		case "signal2_4.1": // signal at node 4 for the lower route
			// set 60 - second2SwitchFromZtoV seconds green for the lower route
			onset = second2SwitchFromZtoV;
			dropping = 60;
			break;
		default:
			log.error("This method was called for signal group ID " + signalGroupId 
					+ " but should not be called for other signal groups than the ones of signal system 4.");
			break;
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
	}
	
	private void createSignal4SettingFirstV(SignalControlDataFactory fac, SignalPlanData signalPlan, Id<SignalGroup> signalGroupId, int second2SwitchFromVtoZ) {
		int onset = 0;
		int dropping = 0;
		// set onset and dropping depending on the signal group and signal control type
		switch (signalGroupId.toString()){
		case "signal3_4.1": // signal at node 4 for the middle route
			// set 60 - second2SwitchFromVtoZ seconds green for the middle route
			onset = second2SwitchFromVtoZ;
			dropping = 60;
			break;
		case "signal24_4.1":
		case "signal2_4.1": // signal at node 4 for the lower route
			// set second2SwitchFromVtoZ seconds green for the lower route
			onset = 0;
			dropping = second2SwitchFromVtoZ;
			break;
		default:
			log.error("This method was called for signal group ID " + signalGroupId 
					+ " but should not be called for other signal groups than the ones of signal system 4.");
			break;
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
	}

	/**
	 * Sets the signal at link 3_4 (i.e. the middle route) green for only one
	 * second a cycle. (Green for no seconds is not possible.)
	 * 
	 * Assumes that the middle link (3_4) exists.
	 */
	private void changeAllGreenSignalControlTo1Z() {

		SignalsData signalsData = (SignalsData) this.scenario	.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalControlData signalControl = signalsData.getSignalControlData();

		SignalSystemControllerData signalSystem4Control = signalControl.getSignalSystemControllerDataBySystemId().get(Id.create("signalSystem4", SignalSystem.class));
		for (SignalPlanData signalPlan : signalSystem4Control.getSignalPlanData().values()) {
			// note: every signal system has only one signal plan here

			// pick the signal at link 3_4 (which is the middle link) from the signal plan
			SignalGroupSettingsData signalGroupZSetting = signalPlan.getSignalGroupSettingsDataByGroupId().get(			Id.create("signal3_4.1", SignalGroup.class));

			// set the signal green for only one second
			signalGroupZSetting.setOnset(0);
			signalGroupZSetting.setDropping(1);
			
			// pick the signal at link 2_4 (or 24_4 respectively) from the signal plan
			// (which is the lower link in front of crossing 4) 
			SignalGroupSettingsData signalGroupVSetting;
			if (signalPlan.getSignalGroupSettingsDataByGroupId().containsKey(Id.create("signal2_4.1", SignalGroup.class)))
				signalGroupVSetting = signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create("signal2_4.1", SignalGroup.class));
			else
				signalGroupVSetting = signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create("signal24_4.1", SignalGroup.class));

			// set the signal green for only one second
			signalGroupVSetting.setOnset(1);
			signalGroupVSetting.setDropping(60);
		}
	}

	/**
	 * Sets the signal for turning right at link 1_2 and the signal for going
	 * straight on at link 2_3 green for only one second a cylce. (Green for no
	 * seconds is not possible.)
	 */
	private void changeAllGreenSignalControlTo1SO() {
		
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalControlData signalControl = signalsData.getSignalControlData();

		// adapt signal system 2
		SignalSystemControllerData signalSystem2Control = signalControl.getSignalSystemControllerDataBySystemId().get(Id.create("signalSystem2", SignalSystem.class));
		for (SignalPlanData signalPlan : signalSystem2Control.getSignalPlanData().values()) {
			// note: every signal system has only one signal plan here

			// pick the second signal at link 1_2 (turning right) from the signal plan
			SignalGroupSettingsData signalGroupSOSetting;
			signalGroupSOSetting = signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create("signal1_2.2", SignalGroup.class));

			// set the signal green for only one second
			signalGroupSOSetting.setOnset(0);
			signalGroupSOSetting.setDropping(1);
		}
		
		// adapt signal system 3
		SignalSystemControllerData signalSystem3Control = signalControl.getSignalSystemControllerDataBySystemId().get(	Id.create("signalSystem3", SignalSystem.class));
		for (SignalPlanData signalPlan : signalSystem3Control.getSignalPlanData().values()) {
			// note: every signal system has only one signal plan here

			// pick the second signal at link 2_3 (or 23_3 respectively) (going straight on) from the signal plan
			SignalGroupSettingsData signalGroupSOSetting;
			if (signalPlan.getSignalGroupSettingsDataByGroupId().containsKey(Id.create("signal2_3.2", SignalGroup.class))){
				signalGroupSOSetting = signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create("signal2_3.2", SignalGroup.class));
			} else {
				signalGroupSOSetting = signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create("signal23_3.2", SignalGroup.class));
			}

			// set the signal green for only one second
			signalGroupSOSetting.setOnset(0);
			signalGroupSOSetting.setDropping(1);
		}
	}

	public void setLaneType(LaneType laneType) {
		this.laneType = laneType;
	}

	public void setSignalType(SignalControlType signalType) {
		this.signalType = signalType;
	}

	public void writeSignalFiles(String directory) {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(directory + "signalSystems.xml");
		new SignalControlWriter20(signalsData.getSignalControlData()).write(directory + "signalControl.xml");
		new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(directory + "signalGroups.xml");
	}

}
