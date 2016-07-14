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
package scenarios.illustrative.parallel.createInput;

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
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;

import java.util.*;

/**
 * Class to create signals (signal systems, signal groups and signal control)
 * for the Parallel scenario.
 *
 * @author gthunig
 */
public final class TtCreateParallelSignals {

    private static final Logger log = Logger
            .getLogger(TtCreateParallelSignals.class);

    private static final int CYCLE_TIME = 60;
    private static final int INTERGREEN_TIME = 5;

    private Scenario scenario;

    private Map<Id<Link>, List<Id<Link>>> possibleSignalMoves = new HashMap<>();
    private List<Id<SignalGroup>> signalGroupsFirstODPair = new ArrayList<>();
    
    public TtCreateParallelSignals(Scenario scenario) {
        this.scenario = scenario;
    }

    public void createSignals() {
        log.info("Create signals ...");

        initPossibleSignalMoves();
        prepareSignalControlInfo();

        createSignalSystems();
        createSignalGroups();
        createSignalControlData();
    }

    private void initPossibleSignalMoves() {

        //signals at node 2
        possibleSignalMoves.put(Id.createLinkId("3_2"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("2_1"))));
        possibleSignalMoves.put(Id.createLinkId("7_2"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("2_1"))));
        possibleSignalMoves.put(Id.createLinkId("1_2"),
                new ArrayList<>(Arrays.asList(Id.createLinkId("2_3"), Id.createLinkId("2_7"))));

        //signals at node 5
        possibleSignalMoves.put(Id.createLinkId("8_5"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("5_6"))));
        possibleSignalMoves.put(Id.createLinkId("4_5"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("5_6"))));
        possibleSignalMoves.put(Id.createLinkId("6_5"),
                new ArrayList<>(Arrays.asList(Id.createLinkId("5_4"), Id.createLinkId("5_8"))));

        //signals at node 10
        possibleSignalMoves.put(Id.createLinkId("3_10"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("10_9"))));
        possibleSignalMoves.put(Id.createLinkId("4_10"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("10_9"))));
        possibleSignalMoves.put(Id.createLinkId("9_10"),
                new ArrayList<>(Arrays.asList(Id.createLinkId("10_3"), Id.createLinkId("10_4"))));

        //signals at node 11
        possibleSignalMoves.put(Id.createLinkId("7_11"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("11_12"))));
        possibleSignalMoves.put(Id.createLinkId("8_11"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("11_12"))));
        possibleSignalMoves.put(Id.createLinkId("12_11"),
                new ArrayList<>(Arrays.asList(Id.createLinkId("11_7"), Id.createLinkId("11_8"))));

        //signals at node 3
        possibleSignalMoves.put(Id.createLinkId("2_3"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("3_4"))));
        possibleSignalMoves.put(Id.createLinkId("10_3"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("3_7"))));
        possibleSignalMoves.put(Id.createLinkId("4_3"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("3_2"))));
        possibleSignalMoves.put(Id.createLinkId("7_3"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("3_10"))));

        //signals at node 4
        possibleSignalMoves.put(Id.createLinkId("3_4"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("4_5"))));
        possibleSignalMoves.put(Id.createLinkId("10_4"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("4_8"))));
        possibleSignalMoves.put(Id.createLinkId("5_4"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("4_3"))));
        possibleSignalMoves.put(Id.createLinkId("8_4"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("4_10"))));

        //signals at node 7
        possibleSignalMoves.put(Id.createLinkId("2_7"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("7_8"))));
        possibleSignalMoves.put(Id.createLinkId("3_7"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("7_11"))));
        possibleSignalMoves.put(Id.createLinkId("11_7"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("7_3"))));
        possibleSignalMoves.put(Id.createLinkId("8_7"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("7_2"))));

        //signals at node 8
        possibleSignalMoves.put(Id.createLinkId("4_8"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("8_11"))));
        possibleSignalMoves.put(Id.createLinkId("5_8"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("8_7"))));
        possibleSignalMoves.put(Id.createLinkId("11_8"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("8_4"))));
        possibleSignalMoves.put(Id.createLinkId("7_8"),
                new ArrayList<>(Collections.singletonList(Id.createLinkId("8_5"))));
    }

    private void prepareSignalControlInfo() {    	
    	
    	signalGroupsFirstODPair.add(Id.create("signal1_2.2_3", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal1_2.2_7", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal3_2.2_1", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal7_2.2_1", SignalGroup.class));
    	
    	signalGroupsFirstODPair.add(Id.create("signal6_5.5_4", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal6_5.5_8", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal4_5.5_6", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal8_5.5_6", SignalGroup.class));
    	
    	signalGroupsFirstODPair.add(Id.create("signal7_8.8_5", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal5_8.8_7", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal4_8.8_11", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal11_8.8_4", SignalGroup.class));
    	
    	signalGroupsFirstODPair.add(Id.create("signal8_4.4_10", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal10_4.4_8", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal3_4.4_5", SignalGroup.class));
    	signalGroupsFirstODPair.add(Id.create("signal5_4.4_3", SignalGroup.class));
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
            createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(10)));
            createSignalSystemAtNode(this.scenario.getNetwork().getNodes().get(Id.createNodeId(11)));
        }
    }

    private void createSignalSystemAtNode(Node node) {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();

        SignalSystemsDataFactory fac = new SignalSystemsDataFactoryImpl();

        // create signal system
		SignalSystemData signalSystem = fac.createSignalSystemData(Id.create("signalSystem" + node.getId(), SignalSystem.class));
        signalSystems.addSignalSystemData(signalSystem);

        // create a signal for every inLink outLink pair that is contained in possibleSignalMoves
        for (Id<Link> inLinkId : node.getInLinks().keySet()) {
            for (Id<Link> outLinkId : node.getOutLinks().keySet()) {
				if (possibleSignalMoves.containsKey(inLinkId) && possibleSignalMoves.get(inLinkId).contains(outLinkId)) {

					SignalData signal = fac.createSignalData(Id.create("signal" + inLinkId + "." + outLinkId, Signal.class));
                    signal.setLinkId(inLinkId);
                    signal.addTurningMoveRestriction(outLinkId);

                    LanesToLinkAssignment20 linkLanes = this.scenario.getLanes().getLanesToLinkAssignments().get(inLinkId);
                    if (linkLanes != null) {
                        for (Lane l : linkLanes.getLanes().values()) {
                            if (l.getToLinkIds() != null) {
                                for (Id<Link> toLinkId : l.getToLinkIds()) {
                                    if (toLinkId.toString().equals(outLinkId.toString())) {
                                        signal.addLaneId(l.getId());
                                    }
                                }
                            }
                        }

                    }
                    signalSystem.addSignalData(signal);
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

    private void createSignalControlData() {

		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
        SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
        SignalControlData signalControl = signalsData.getSignalControlData();
        SignalControlDataFactory fac = new SignalControlDataFactoryImpl();

        // creates a signal control for all signal systems
		for (SignalSystemData signalSystem : signalSystems.getSignalSystemData().values()) {

			SignalSystemControllerData signalSystemControl = fac.createSignalSystemControllerData(signalSystem.getId());

			// creates a default plan for the signal system (with defined cycle time and offset 0)
            SignalPlanData signalPlan = SignalUtils.createSignalPlan(fac, CYCLE_TIME, 0);

            signalSystemControl.addSignalPlanData(signalPlan);
			signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
            signalControl.addSignalSystemControllerData(signalSystemControl);

            // specifies signal group settings for all signal groups of this signal system
			for (SignalGroupData signalGroup : signalGroups.getSignalGroupDataBySystemId(signalSystem.getId()).values()) {
                createSignalControlForSignalGroup(fac, signalPlan, signalGroup.getId());
            }
        }
    }

	private void createSignalControlForSignalGroup(SignalControlDataFactory fac, SignalPlanData signalPlan, Id<SignalGroup> signalGroupId) {
		int onset;
		int dropping;
		int signalSystemOffset;
		if (signalGroupsFirstODPair.contains(signalGroupId)) {
			// the signal belongs to the first OD-pair
			onset = 0;
			dropping = 30 - INTERGREEN_TIME;
			signalSystemOffset = 0;
		} else {
			// the signal belongs to the second OD-pair
			onset = 30;
			dropping = 60 - INTERGREEN_TIME;
			signalSystemOffset = 0;
		}
		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(fac, signalGroupId, onset, dropping));
		signalPlan.setOffset(signalSystemOffset);
    }

    public void writeSignalFiles(String directory) {
		SignalsData signalsData = (SignalsData) this.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);

        new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(directory + "signalSystems.xml");
        new SignalControlWriter20(signalsData.getSignalControlData()).write(directory + "signalControl.xml");
        new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(directory + "signalGroups.xml");
    }

}
