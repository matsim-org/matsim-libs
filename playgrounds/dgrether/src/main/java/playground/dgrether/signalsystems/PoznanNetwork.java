/* *********************************************************************** *
 * project: org.matsim.*
 * PoznanNetwork
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.signals.data.SignalsDataImpl;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.signalgroups.v20.*;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v11.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.data.v11.*;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.ambertimes.v10.AmberTimesData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.dgrether.DgOTFVis;
import playground.dgrether.lanes.LanesConsistencyChecker;
import playground.dgrether.signalsystems.data.consistency.SignalControlDataConsistencyChecker;
import playground.dgrether.signalsystems.data.consistency.SignalGroupsDataConsistencyChecker;
import playground.dgrether.signalsystems.data.consistency.SignalSystemsDataConsistencyChecker;


/**
 * TODO show michal gpl header constraint and how to 
 * 
 * @author michalm
 * @author dgrether
 *
 */
public class PoznanNetwork
{
    private static Network network;
    private static NetworkFactory netFactory;
    private static ScenarioImpl scenario;
    private static LaneDefinitions11 lanes;
    private static LaneDefinitionsFactory11 laneFactory;
    private static SignalGroupsDataFactory gf;
    private static SignalGroupsData groups;

    static double freespeed = 10.0;
    static double capacity = 3600.0;


    public static void createPhysics()
    {
        network = scenario.getNetwork();
        netFactory = network.getFactory();

        Node n10 = createAndAddNode("10", 400, 400);
        Node n11 = createAndAddNode("11", 700, 400);
        Node n12 = createAndAddNode("12", 400, 100);
        Node n13 = createAndAddNode("13", 100, 400);

        Node n20 = createAndAddNode("20", 400, 460);
        Node n21 = createAndAddNode("21", 500, 460);

        Node n30 = createAndAddNode("30", 400, 620);
        Node n31 = createAndAddNode("31", 300, 620);

        Node n40 = createAndAddNode("40", 400, 660);
        Node n41 = createAndAddNode("41", 500, 660);

        Node n50 = createAndAddNode("50", 400, 730);
        Node n51 = createAndAddNode("51", 300, 730);

        Node n70 = createAndAddNode("70", 400, 830);
        Node n71 = createAndAddNode("71", 700, 830);
        Node n72 = createAndAddNode("72", 100, 830);

        Node n80 = createAndAddNode("80", 400, 1040);
        Node n81 = createAndAddNode("81", 300, 1040);

        Node n90 = createAndAddNode("90", 400, 1150);
        Node n91 = createAndAddNode("91", 700, 1150);
        Node n92 = createAndAddNode("92", 100, 1150);

        // === 13<->10
        Link l13_10 = createAndAddLink(n13, n10, 2);
        Link l10_13 = createAndAddLink(n10, n13, 2);

        // === 12<->10
        Link l12_10 = createAndAddLink(n12, n10, 1);
        Link l10_12 = createAndAddLink(n10, n12, 2);

        // === 11<->10
        Link l11_10 = createAndAddLink(n11, n10, 2);
        Link l10_11 = createAndAddLink(n10, n11, 2);

        // === 10-->20-->30; 10<--30
        Link l10_20 = createAndAddLink(n10, n20, 2);
        Link l20_30 = createAndAddLink(n20, n30, 2);
        Link l30_10 = createAndAddLink(n30, n10, 2);

        // === 21<->20
        Link l20_21 = createAndAddLink(n20, n21, 1);
        Link l21_20 = createAndAddLink(n21, n20, 1);

        // === 31<->30
        Link l30_31 = createAndAddLink(n30, n31, 1);
        Link l31_30 = createAndAddLink(n31, n30, 1);

        // === 30<->40
        Link l30_40 = createAndAddLink(n30, n40, 2);
        Link l40_30 = createAndAddLink(n40, n30, 2);

        // === 41<->40
        Link l40_41 = createAndAddLink(n40, n41, 1);
        Link l41_40 = createAndAddLink(n41, n40, 1);

        // === 70-->50-->40; 70<--40
        Link l70_50 = createAndAddLink(n70, n50, 2);
        Link l50_40 = createAndAddLink(n50, n40, 2);
        Link l40_70 = createAndAddLink(n40, n70, 2);

        // === 51<->50
        Link l50_51 = createAndAddLink(n50, n51, 1);
        Link l51_50 = createAndAddLink(n51, n50, 1);

        // === 71<->70
        Link l70_71 = createAndAddLink(n70, n71, 1);
        Link l71_70 = createAndAddLink(n71, n70, 1);

        // === 72-->70
        Link l72_70 = createAndAddLink(n72, n70, 2);

        // === 70<->80
        Link l70_80 = createAndAddLink(n70, n80, 2);
        Link l80_70 = createAndAddLink(n80, n70, 1);

        // === 80<->90
        Link l80_90 = createAndAddLink(n80, n90, 2);
        Link l90_80 = createAndAddLink(n90, n80, 1);

        // === 81-->80
        Link l81_80 = createAndAddLink(n81, n80, 1);

        // === 91<->90
        Link l90_91 = createAndAddLink(n90, n91, 1);
        Link l91_90 = createAndAddLink(n91, n90, 2);

        // === 92<->90
        Link l90_92 = createAndAddLink(n90, n92, 2);

        lanes = new LaneDefinitions11Impl();
        laneFactory = lanes.getFactory();

        LanesToLinkAssignment11 l2l = laneFactory.createLanesToLinkAssignment(l12_10.getId());
        createAndAddLanes(l2l, 1, 90, l10_13);
        createAndAddLanes(l2l, 2, 90, l10_20);
        createAndAddLanes(l2l, 3, 100, l10_20, l10_11);
        lanes.addLanesToLinkAssignment(l2l);

        // lanes for link 11_10
        l2l = laneFactory.createLanesToLinkAssignment(l11_10.getId());
        createAndAddLanes(l2l, 1, 110, l10_12);
        createAndAddLanes(l2l, 2, 80, 2.0, l10_13);
        createAndAddLanes(l2l, 3, 80, l10_20);
        lanes.addLanesToLinkAssignment(l2l);

        LaneData11 lane;
        // lanes for link 30_10
        l2l = laneFactory.createLanesToLinkAssignment(l30_10.getId());
        createAndAddLanes(l2l, 1, 90, 1.8, l10_11);
        createAndAddLanes(l2l, 2, 90, 2.0, l10_12);
        createAndAddLanes(l2l, 3, 105, l10_13);
        lanes.addLanesToLinkAssignment(l2l);

        // lanes for link 13_10
        l2l = laneFactory.createLanesToLinkAssignment(l13_10.getId());
        createAndAddLanes(l2l, 1, 160, l10_20);
        createAndAddLanes(l2l, 2, 160, 2.0, l10_11);
        createAndAddLanes(l2l, 3, 240, l10_12);
        lanes.addLanesToLinkAssignment(l2l);

        // lanes for link 30_40
        l2l = laneFactory.createLanesToLinkAssignment(l30_40.getId());
        createAndAddLanes(l2l, 1, 20, l40_70);
        createAndAddLanes(l2l, 2, 20, l40_70, l40_41);
        lanes.addLanesToLinkAssignment(l2l);

        // lanes for link 40_30
        l2l = laneFactory.createLanesToLinkAssignment(l40_30.getId());
        createAndAddLanes(l2l, 1, 20, l30_10);
        createAndAddLanes(l2l, 2, 20, l30_10, l30_31);
        lanes.addLanesToLinkAssignment(l2l);

        // lanes for link 40_70
        l2l = laneFactory.createLanesToLinkAssignment(l40_70.getId());
        createAndAddLanes(l2l, 1, 100, l70_80);
        createAndAddLanes(l2l, 2, 100, l70_80, l70_71);
        lanes.addLanesToLinkAssignment(l2l);

        // lanes for link 72_70
        l2l = laneFactory.createLanesToLinkAssignment(l72_70.getId());
        createAndAddLanes(l2l, 1, 70, l70_80);
        createAndAddLanes(l2l, 2, 70, l70_71);
        createAndAddLanes(l2l, 3, 70, l70_50);
        lanes.addLanesToLinkAssignment(l2l);

        // lanes for link 80_70
        l2l = laneFactory.createLanesToLinkAssignment(l80_70.getId());
        createAndAddLanes(l2l, 1, 70, 2.0, l70_50);
        lanes.addLanesToLinkAssignment(l2l);

        // lanes for link 80_90
        l2l = laneFactory.createLanesToLinkAssignment(l80_90.getId());
        createAndAddLanes(l2l, 1, 100, 2.0, l90_92);
        createAndAddLanes(l2l, 2, 100, l90_91);
        lanes.addLanesToLinkAssignment(l2l);

        // lanes for link 91_90
        l2l = laneFactory.createLanesToLinkAssignment(l91_90.getId());
        createAndAddLanes(l2l, 1, 100, l90_80);
        createAndAddLanes(l2l, 2, 100, l90_92);

        lanes.addLanesToLinkAssignment(l2l);

        // create the traffic signal infrastructure
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataImpl(ConfigUtils.addOrGetModule(scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class)));
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signals = signalsData.getSignalSystemsData();

        SignalSystemsDataFactory sf = signals.getFactory();

        // signals at node 10
        SignalSystemData sys10 = sf.createSignalSystemData(Id.create("ss10", SignalSystem.class));
        signals.addSignalSystemData(sys10);

        SignalData signal = sf.createSignalData(Id.create("s12_10_1", Signal.class));
        signal.setLinkId(Id.create("12_10", Link.class));
        signal.addLaneId(Id.create("12_10_1", Lane.class));
        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s12_10_2", Signal.class));
        signal.setLinkId(Id.create("12_10", Link.class));
        signal.addLaneId(Id.create("12_10_2", Lane.class));
        signal.addLaneId(Id.create("12_10_3", Lane.class));
        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s12_10_3", Signal.class));
        signal.setLinkId(Id.create("12_10", Link.class));
        signal.addLaneId(Id.create("12_10_3", Lane.class));
        signal.addTurningMoveRestriction(Id.create("10_11", Link.class));
        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s11_10_1", Signal.class));
        signal.setLinkId(Id.create("11_10", Link.class));
        signal.addLaneId(Id.create("11_10_1", Lane.class));
        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s11_10_2", Signal.class));
        signal.setLinkId(Id.create("11_10", Link.class));
        signal.addLaneId(Id.create("11_10_2", Lane.class));
        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s11_10_3", Signal.class));
        signal.setLinkId(Id.create("11_10", Link.class));
        signal.addLaneId(Id.create("11_10_3", Lane.class));
        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s30_10_1", Signal.class));
        signal.setLinkId(Id.create("30_10", Link.class));
        signal.addLaneId(Id.create("30_10_1", Lane.class));
        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s30_10_2", Signal.class));
        signal.setLinkId(Id.create("30_10", Link.class));
        signal.addLaneId(Id.create("30_10_2", Lane.class));
        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s30_10_3", Signal.class));
        signal.setLinkId(Id.create("30_10", Link.class));
        signal.addLaneId(Id.create("30_10_3", Lane.class));
        sys10.addSignalData(signal);

//        signal = sf.createSignalData(Id.create("s30_10_1"));
//        signal.setLinkId(Id.create("30_10"));
//        signal.addLaneId(Id.create("30_10_1"));
//        sys10.addSignalData(signal);
//
//        signal = sf.createSignalData(Id.create("s30_10_2"));
//        signal.setLinkId(Id.create("30_10"));
//        signal.addLaneId(Id.create("30_10_2"));
//        sys10.addSignalData(signal);
//
//        signal = sf.createSignalData(Id.create("s30_10_3"));
//        signal.setLinkId(Id.create("30_10"));
//        signal.addLaneId(Id.create("30_10_3"));
//        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s13_10_1", Signal.class));
        signal.setLinkId(Id.create("13_10", Link.class));
        signal.addLaneId(Id.create("13_10_1", Lane.class));
        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s13_10_2", Signal.class));
        signal.setLinkId(Id.create("13_10", Link.class));
        signal.addLaneId(Id.create("13_10_2", Lane.class));
        sys10.addSignalData(signal);

        signal = sf.createSignalData(Id.create("s13_10_3", Signal.class));
        signal.setLinkId(Id.create("13_10", Link.class));
        signal.addLaneId(Id.create("13_10_3", Lane.class));
        sys10.addSignalData(signal);

        // create SignalGroups
        groups = signalsData.getSignalGroupsData();
        gf = groups.getFactory();

        SignalGroupData group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_0", SignalGroup.class));
        group.addSignalId(Id.create("s30_10_3", Signal.class));
        groups.addSignalGroupData(group);

        group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_1", SignalGroup.class));
        group.addSignalId(Id.create("s30_10_1", Signal.class));
        group.addSignalId(Id.create("s30_10_2", Signal.class));
        groups.addSignalGroupData(group);

        group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_2", SignalGroup.class));
        group.addSignalId(Id.create("s11_10_3", Signal.class));
        groups.addSignalGroupData(group);

        group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_3", SignalGroup.class));
        group.addSignalId(Id.create("s11_10_1", Signal.class));
        group.addSignalId(Id.create("s11_10_2", Signal.class));
        groups.addSignalGroupData(group);

        group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_4_1", SignalGroup.class));
        group.addSignalId(Id.create("s12_10_3", Signal.class));
        groups.addSignalGroupData(group);

        group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_4_2", SignalGroup.class));
        group.addSignalId(Id.create("s12_10_3", Signal.class));
        groups.addSignalGroupData(group);

        group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_5", SignalGroup.class));
        group.addSignalId(Id.create("s12_10_1", Signal.class));
        group.addSignalId(Id.create("s12_10_2", Signal.class));
        groups.addSignalGroupData(group);

        group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_6_1", SignalGroup.class));
        group.addSignalId(Id.create("s13_10_3", Signal.class));
        groups.addSignalGroupData(group);

        group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_6_2", SignalGroup.class));
        group.addSignalId(Id.create("s13_10_3", Signal.class));
        groups.addSignalGroupData(group);

        group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_7", SignalGroup.class));
        group.addSignalId(Id.create("s13_10_2", Signal.class));
        groups.addSignalGroupData(group);

        group = gf.createSignalGroupData(sys10.getId(), Id.create("sg_8", SignalGroup.class));
        group.addSignalId(Id.create("s13_10_1", Signal.class));
        groups.addSignalGroupData(group);

        // TODO signal system for next node

    }


    private static Node createAndAddNode(String id, double x, double y)
    {
        Node node = netFactory.createNode(Id.create(id, Node.class), scenario.createCoord(x, y));
        network.addNode(node);
        return node;
    }


    private static Link createAndAddLink(Node n1, Node n2, double lanes)
    {
        String id = n1.getId() + "_" + n2.getId();
        Link link = netFactory.createLink(Id.create(id, Link.class), n1, n2);

        double diffX = n1.getCoord().getX() - n2.getCoord().getX();
        double diffY = n1.getCoord().getY() - n2.getCoord().getY();
        link.setLength(Math.round(Math.sqrt(diffX * diffX + diffY * diffY)));

        link.setNumberOfLanes(lanes);
        link.setCapacity(capacity);
        link.setFreespeed(freespeed);

        network.addLink(link);
        return link;
    }


    private static void createAndAddLanes(LanesToLinkAssignment11 l2l, int id, double length,
            Link... toLinks)
    {
        createAndAddLanes(l2l, id, length, 1, toLinks);
    }


    private static void createAndAddLanes(LanesToLinkAssignment11 l2l, int id, double length,
            double noLanes, Link... toLinks)
    {
    	LaneData11 lane = laneFactory.createLane(Id.create(l2l.getLinkId() + "_" + id, Lane.class));

        for (Link toLink : toLinks) {
            lane.addToLinkId(toLink.getId());
        }

        lane.setStartsAtMeterFromLinkEnd(length);
        lane.setNumberOfRepresentedLanes(noLanes);
        l2l.addLane(lane);
    }


    private static void createSignalControl(Scenario scenario)
    {
        SignalsData sd = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalControlData control = sd.getSignalControlData();
        SignalControlDataFactory scf = control.getFactory();

        SignalSystemControllerData ssController = scf.createSignalSystemControllerData(Id.create("ss10", SignalSystem.class));
        control.addSignalSystemControllerData(ssController);
        // fixed-time control
        ssController.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
        SignalPlanData plan = scf.createSignalPlanData(Id.create("ss10_p1", SignalPlan.class));
        ssController.addSignalPlanData(plan);
        plan.setCycleTime(120);
        plan.setOffset(0); // coordination offset
        // now the single signals (signal groups)
        SignalGroupSettingsData settings = scf.createSignalGroupSettingsData(Id.create("sg_0", SignalGroup.class));
        // green from second 0 to 30 in cycle
        settings.setOnset(0);
        settings.setDropping(5);
        plan.addSignalGroupSettings(settings);

        settings = scf.createSignalGroupSettingsData(Id.create("sg_1", SignalGroup.class));
        // green from second 0 to 30 in cycle
        settings.setOnset(10);
        settings.setDropping(15);
         plan.addSignalGroupSettings(settings);

        settings = scf.createSignalGroupSettingsData(Id.create("sg_2", SignalGroup.class));
        // green from second 0 to 30 in cycle
        settings.setOnset(20);
        settings.setDropping(25);
        plan.addSignalGroupSettings(settings);

        settings = scf.createSignalGroupSettingsData(Id.create("sg_3", SignalGroup.class));
        // green from second 0 to 30 in cycle
        settings.setOnset(30);
        settings.setDropping(35);
        plan.addSignalGroupSettings(settings);

        settings = scf.createSignalGroupSettingsData(Id.create("sg_4_1", SignalGroup.class));
        // green from second 0 to 30 in cycle
        settings.setOnset(40);
        settings.setDropping(45);
        plan.addSignalGroupSettings(settings);

        settings = scf.createSignalGroupSettingsData(Id.create("sg_4_2", SignalGroup.class));
        // green from second 0 to 30 in cycle
        settings.setOnset(50);
        settings.setDropping(55);
        plan.addSignalGroupSettings(settings);

        settings = scf.createSignalGroupSettingsData(Id.create("sg_5", SignalGroup.class));
        // green from second 0 to 30 in cycle
        settings.setOnset(60);
        settings.setDropping(65);
        plan.addSignalGroupSettings(settings);

        settings = scf.createSignalGroupSettingsData(Id.create("sg_6_1", SignalGroup.class));
        // green from second 0 to 30 in cycle
        settings.setOnset(70);
        settings.setDropping(75);
        plan.addSignalGroupSettings(settings);

        settings = scf.createSignalGroupSettingsData(Id.create("sg_6_2", SignalGroup.class));
        // green from second 0 to 30 in cycle
        settings.setOnset(80);
        settings.setDropping(85);
        plan.addSignalGroupSettings(settings);

        settings = scf.createSignalGroupSettingsData(Id.create("sg_7", SignalGroup.class));
        // green from second 0 to 30 in cycle
        settings.setOnset(90);
        settings.setDropping(95);
        plan.addSignalGroupSettings(settings);

        settings = scf.createSignalGroupSettingsData(Id.create("sg_8", SignalGroup.class));
        // green from second 30 to 55 in cycle
        settings.setOnset(100);
        settings.setDropping(105);
        plan.addSignalGroupSettings(settings);
    }


    private static void createAmbertimes(SignalsData signalsData)
    {
        AmberTimesData amberTimes = signalsData.getAmberTimesData();
        amberTimes.setDefaultAmber(3);
        amberTimes.setDefaultRedAmber(1);
    }


    private static void createPopulation(ScenarioImpl scenario)
    {
        Population pop = scenario.getPopulation();
        PopulationFactory pf = pop.getFactory();
        for (int i = 1; i <= 1000; i++) {
            Person person = pf.createPerson(Id.create(Integer.toString(i), Person.class));
            pop.addPerson(person);
            Plan plan = pf.createPlan();
            Activity homeAct = pf.createActivityFromLinkId("home", Id.create("91_90", Link.class));
            homeAct.setEndTime(120 + i * 5);
            plan.addActivity(homeAct);
            Leg leg = pf.createLeg(TransportMode.car);
            leg.setRoute(null);
            plan.addLeg(leg);
            homeAct = pf.createActivityFromLinkId("home", Id.create("10_12", Link.class));
            plan.addActivity(homeAct);
            person.addPlan(plan);
        }
    }


    public static void main(String[] args)
    {
        // general setup
        Config config = ConfigUtils.createConfig();
        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        config.qsim().setStartTime(0.0);
        config.qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
        config.qsim().setUseLanes(true);
        ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);
        OTFVisConfigGroup otfconfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
        otfconfig.setAgentSize(70.0f);
        otfconfig.setMapOverlayMode(false);
        config.qsim().setNodeOffset(30);
        ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseAmbertimes(true);
        scenario = (ScenarioImpl)ScenarioUtils.createScenario(config);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataImpl(ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class)));

        // create network lanes and signals
        createPhysics();
        LaneDefinitions20 lanes20 = LaneDefinitionsV11ToV20Conversion.convertTo20(
                lanes, scenario.getNetwork());
        LanesConsistencyChecker lcc = new LanesConsistencyChecker(scenario.getNetwork(), lanes20);
        lcc.checkConsistency();
        SignalSystemsDataConsistencyChecker sscc = new SignalSystemsDataConsistencyChecker(scenario.getNetwork(), lanes20, (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME));
        sscc.checkConsistency();

        SignalGroupsDataConsistencyChecker sgcc = new SignalGroupsDataConsistencyChecker(scenario);
        sgcc.checkConsistency();

        // create the signal control
        createSignalControl(scenario);
        SignalControlDataConsistencyChecker sccc = new SignalControlDataConsistencyChecker(scenario);
        sccc.checkConsistency();

        createAmbertimes((SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME));

        createPopulation(scenario);

        // System.exit(0);

        // output
//        String baseDir = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\";
         String baseDir = "/media/data/work/matsim/examples/poznan/";

        SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
        String signalSystemsFile = baseDir + "signal_systems.xml";
        signalsWriter.setSignalSystemsOutputFilename(signalSystemsFile);
        ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalSystemFile(signalSystemsFile);
        String signalGroupsFile = baseDir + "signal_groups.xml";
        signalsWriter.setSignalGroupsOutputFilename(signalGroupsFile);
        ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalGroupsFile(signalGroupsFile);
        String signalControlFile = baseDir + "signal_control.xml";
        signalsWriter.setSignalControlOutputFilename(signalControlFile);
        ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(signalControlFile);
        String amberTimesFile = baseDir + "amber_times.xml";
        signalsWriter.setAmberTimesOutputFilename(amberTimesFile);
        ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setAmberTimesFile(amberTimesFile);
        signalsWriter.writeSignalsData((SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME));

        String lanesOutputFile = baseDir + "lanes.xml";
        // String lanesOutputFile = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\lanes.xml";
        new LaneDefinitionsWriter11(lanes).write(lanesOutputFile);
        // config.network().setLaneDefinitionsFile(lanesOutputFile);

        String lanes20OutputFile = baseDir + "lanes20.xml";
        // String lanes20OutputFile = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\lanes20.xml";
        LaneDefinitionsWriter20 writerDelegate = new LaneDefinitionsWriter20(lanes20);
        writerDelegate.write(lanes20OutputFile);
        config.network().setLaneDefinitionsFile(lanes20OutputFile);

        String popFilename = baseDir + "population.xml";
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(popFilename);
        config.plans().setInputFile(popFilename);

        String networkFilename = baseDir + "network.xml";
        // String networkFilename = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\network.xml";
        new NetworkWriter(scenario.getNetwork()).write(networkFilename);
        config.network().setInputFile(networkFilename);
        String configFilename = baseDir + "config.xml";
        new ConfigWriter(config).write(configFilename);
        // String configFilename = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\config.xml";

				// visualization
        new DgOTFVis().playAndRouteConfig(configFilename);
//        String[] c = {configFilename};
//        try {
//					XVis.main(c);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
    }

}