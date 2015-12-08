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
package scenarios.parallel.createInput;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.*;
import org.matsim.contrib.signals.data.signalsystems.v20.*;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;

/**
 * Class to create signals (signal systems, signal groups and signal control)
 * for the Parallel scenario.
 * 
 * @author gthunig
 * 
 */
public class TtCreateParallelSignals {

	private static final Logger log = Logger
			.getLogger(TtCreateParallelSignals.class);

	private static final int CYCLE_TIME = 60;
	private static final int INTERGREEN_TIME = 5;

	private Scenario scenario;

	public TtCreateParallelSignals(Scenario scenario) {
		this.scenario = scenario;
	}

	public void createSignals() {
		
		createSignalSystems();
		createSignalGroups();
		createSignalControl();
	}

	/**
	 * Creates signal systems depending on the network situation.
	 */
	private void createSignalSystems() {

		createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(2)));
		createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(3)));
		createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(4)));
		createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(5)));
		createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(7)));
		createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(8)));
		
		if (TtCreateParallelNetworkAndLanes.checkNetworkForSecondODPair(this.scenario.getNetwork())) {
			createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(9)));
			createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(11)));
		}
	}

	private void createSignalSystemAtNode(Node node) {
		SignalsData signalsData = (SignalsData) this.scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		
		SignalSystemsDataFactory fac = new SignalSystemsDataFactoryImpl();
		
		// create signal system
		SignalSystemData signalSystem = fac.createSignalSystemData(Id.create("signalSystem"
				+ node.getId(), SignalSystem.class));
		signalSystems.addSignalSystemData(signalSystem);
		
		// create a signal for every inLink outLink pair
		for (Id<Link> inLinkId : node.getInLinks().keySet()){
			int outLinkCounter = 0;
			for (Id<Link> outLinkId : node.getOutLinks().keySet()) {
				outLinkCounter++;
				SignalData signal = fac.createSignalData(Id.create("signal" + inLinkId
						+ "." + outLinkCounter, Signal.class));
				signalSystem.addSignalData(signal);
				signal.setLinkId(inLinkId);

				LanesToLinkAssignment20 linkLanes = this.scenario.getLanes().getLanesToLinkAssignments().get(inLinkId);
				// the link only contains one lane (the trivial lane)
				signal.addLaneId(linkLanes.getLanes().firstKey());
				//signal.addTurningMoveRestriction(outLinkId);
			}
		}
	}

	private void createSignalGroups() {

		SignalsData signalsData = (SignalsData) this.scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();

		// create signal groups for each signal system
		for (SignalSystemData system : signalSystems.getSignalSystemData()
				.values()) {
			SignalUtils.createAndAddSignalGroups4Signals(signalGroups, system);
		}
	}

	private void createSignalControl() {

		SignalsData signalsData = (SignalsData) this.scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory fac = new SignalControlDataFactoryImpl();

		// creates a signal control for all signal systems
		for (SignalSystemData signalSystem : signalSystems
				.getSignalSystemData().values()) {

			SignalSystemControllerData signalSystemControl = fac
					.createSignalSystemControllerData(signalSystem.getId());

			// creates a default plan for the signal system (with defined cycle
			// time and offset 0)
			SignalPlanData signalPlan = SignalUtils.createSignalPlan(fac, CYCLE_TIME, 0);
			
			signalSystemControl.addSignalPlanData(signalPlan);
			signalSystemControl
					.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			signalControl.addSignalSystemControllerData(signalSystemControl);
			
			// specifies signal group settings for all signal groups of this
			// signal system
			for (SignalGroupData signalGroup : signalGroups.
					getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
				createSignalControl(fac, signalPlan, signalGroup.getId());
			}
		}
	}

	private void createSignalControl(SignalControlDataFactory fac, SignalPlanData signalPlan,
			Id<SignalGroup> signalGroupId) {
		int onset = 0;
		int dropping = 0;
		int signalSystemOffset = 0;
		// set onset and dropping for each signal group and offset for each signal system
		switch (signalGroupId.toString()){

			case "signal1_2.1": // signal for turning left (northern route) at node 2
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal2_3.1": // signal for going straight (northern route) at node 3
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal3_4.1": // signal for going straight (northern route) at node 4
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal4_5.1": // signal for going straight (northern route) at node 5
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;

			case "signal1_2.2": // signal for turning right (southern route) at node 2
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal2_7.1": // signal for going straight (southern route) at node 7
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal7_8.1": // signal for going straight (southern route) at node 8
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal8_5.1": // signal for going straight (southern route) at node 5
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;

			case "signal6_5.1": // signal for turning left (southern route) at node 5
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal5_8.1": // signal for going straight (southern route) at node 8
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal8_7.1": // signal for going straight (southern route) at node 7
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal7_2.1": // signal for going straight (southern route) at node 2
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;

			case "signal6_5.2": // signal for turning right (northern route) at node 5
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal5_4.1": // signal for going straight (northern route) at node 4
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal4_3.1": // signal for going straight (northern route) at node 3
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal3_2.1": // signal for going straight (northern route) at node 2
				onset = 0;
				dropping = 30 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;


			case "signal9_10.1": // signal for turning left (eastern route) at node 10
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal10_4.1": // signal for going straight (eastern route) at node 4
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal4_8.1": // signal for going straight (eastern route) at node 8
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal8_11.1": // signal for going straight (eastern route) at node 11
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;

			case "signal9_10.2": // signal for turning right (western route) at node 10
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal10_3.1": // signal for going straight (western route) at node 3
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal3_7.1": // signal for going straight (western route) at node 7
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal7_11.1": // signal for going straight (western route) at node 11
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;

			case "signal12_11.1": // signal for turning left (western route) at node 11
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal11_7.1": // signal for going straight (western route) at node 7
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal7_3.1": // signal for going straight (western route) at node 3
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal3_10.1": // signal for going straight (western route) at node 10
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;

			case "signal12_11.2": // signal for turning right (eastern route) at node 11
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal11_8.1": // signal for going straight (eastern route) at node 8
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal8_4.1": // signal for going straight (eastern route) at node 4
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			case "signal4_10.1": // signal for going straight (eastern route) at node 11
				onset = 30;
				dropping = 60 - INTERGREEN_TIME;
				signalSystemOffset = 0;
				break;
			default:
				log.error("Signal group id " + signalGroupId + " is not known.");
				break;
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
	}

	public void writeSignalFiles(String directory) {
		SignalsData signalsData = (SignalsData) this.scenario
				.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(directory + "signalSystems.xml");
		new SignalControlWriter20(signalsData.getSignalControlData()).write(directory + "signalControl.xml");
		new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(directory + "signalGroups.xml");
	}

}
