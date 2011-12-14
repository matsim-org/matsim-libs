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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsFactory;
import org.matsim.lanes.LaneDefinitionsV11ToV20Conversion;
import org.matsim.lanes.LaneDefinitionsWriter11;
import org.matsim.lanes.LaneDefinitionsWriter20;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsDataImpl;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.data.ambertimes.v10.AmberTimesData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsDataFactory;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.signalsystems.model.DefaultPlanbasedSignalSystemController;

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
	
	 public static void createPhysics(ScenarioImpl scenario){
     Network network = scenario.getNetwork();
     NetworkFactory netFactory = network.getFactory(); 
     
     Node n10 = netFactory.createNode(scenario.createId("10"), scenario.createCoord(400, 400));
     Node n11 = netFactory.createNode(scenario.createId("11"), scenario.createCoord(800, 400));
     Node n12 = netFactory.createNode(scenario.createId("12"), scenario.createCoord(400, 0));
     Node n13 = netFactory.createNode(scenario.createId("13"), scenario.createCoord(0, 400));
//     Node n14 = netFactory.createNode(scenario.createId("14"), scenario.createCoord(400, 300));//??
     
     Node n20 = netFactory.createNode(scenario.createId("20"), scenario.createCoord(400, 460));
     Node n21 = netFactory.createNode(scenario.createId("21"), scenario.createCoord(500, 460));

     Node n30 = netFactory.createNode(scenario.createId("30"), scenario.createCoord(400, 620));
     Node n31 = netFactory.createNode(scenario.createId("31"), scenario.createCoord(300, 620));

     Node n40 = netFactory.createNode(scenario.createId("40"), scenario.createCoord(400, 660));
     Node n41 = netFactory.createNode(scenario.createId("41"), scenario.createCoord(500, 660));
     
     Node n50 = netFactory.createNode(scenario.createId("50"), scenario.createCoord(400, 730));
     Node n51 = netFactory.createNode(scenario.createId("51"), scenario.createCoord(300, 730));
     
     Node n70 = netFactory.createNode(scenario.createId("70"), scenario.createCoord(400, 830));
     Node n71 = netFactory.createNode(scenario.createId("71"), scenario.createCoord(700, 830));
     Node n72 = netFactory.createNode(scenario.createId("72"), scenario.createCoord(100, 830));
     
     Node n80 = netFactory.createNode(scenario.createId("80"), scenario.createCoord(400, 1040));
     Node n81 = netFactory.createNode(scenario.createId("81"), scenario.createCoord(300, 1040));
     
     Node n90 = netFactory.createNode(scenario.createId("90"), scenario.createCoord(400, 1150));
     Node n91 = netFactory.createNode(scenario.createId("91"), scenario.createCoord(700, 1150));
     Node n92 = netFactory.createNode(scenario.createId("92"), scenario.createCoord(100, 1150));
     
     network.addNode(n10);
     network.addNode(n11);
     network.addNode(n12);
     network.addNode(n13);
//     network.addNode(n14);
     network.addNode(n20);
     network.addNode(n21);
     network.addNode(n30);
     network.addNode(n31);
     network.addNode(n40);
     network.addNode(n41);
     network.addNode(n50);
     network.addNode(n51);
     network.addNode(n70);
     network.addNode(n71);
     network.addNode(n72);
     network.addNode(n80);
     network.addNode(n81);
     network.addNode(n90);
     network.addNode(n91);
     network.addNode(n92);
     
     //TODO currently nearly all links have common attributes
     double length = 500.0;
     double freespeed = 10.0;
     double capacity = 3600.0;
     Link l13_10 = netFactory.createLink(scenario.createId("13_10"), n13, n10);
     l13_10.setLength(length);
     network.addLink(l13_10);
     Link l10_13 = netFactory.createLink(scenario.createId("10_13"), n10, n13);
     l10_13.setLength(length);
     network.addLink(l10_13);
     
     Link l12_10 = netFactory.createLink(scenario.createId("12_10"), n12, n10);
     l12_10.setLength(400.0);
     l12_10.setNumberOfLanes(1.0);
     l12_10.setFreespeed(freespeed);
     l12_10.setCapacity(capacity);
     network.addLink(l12_10);
     
//     Link l14_10 = netFactory.createLink(scenario.createId("14_10"), n14, n10);
//     l14_10.setLength(100);
//     l14_10.setNumberOfLanes(2.0);
//     l14_10.setFreespeed(freespeed);
//     l14_10.setCapacity(capacity);
//     network.addLink(l14_10);
     
     Link l10_12 = netFactory.createLink(scenario.createId("10_12"), n10, n12);
     l10_12.setLength(length);
     network.addLink(l10_12);
     
     Link l11_10 = netFactory.createLink(scenario.createId("11_10"), n11, n10);
     l11_10.setLength(length);
     network.addLink(l11_10);
     Link l10_11 = netFactory.createLink(scenario.createId("10_11"), n10, n11);
     l10_11.setLength(length);
     network.addLink(l10_11);
     Link l30_10 = netFactory.createLink(scenario.createId("30_10"), n30, n10);
     l30_10.setLength(length);
     network.addLink(l30_10);
     
     Link l10_20 = netFactory.createLink(scenario.createId("10_20"), n10, n20);
     l10_20.setLength(250.0);
     network.addLink(l10_20);
     Link l20_30 = netFactory.createLink(scenario.createId("20_30"), n20, n30);
     l20_30.setLength(250.0);
     network.addLink(l20_30);
     
     Link l20_21 = netFactory.createLink(scenario.createId("20_21"), n20, n21);
     l20_21.setLength(length);
     network.addLink(l20_21);
     Link l21_20 = netFactory.createLink(scenario.createId("21_20"), n21, n20);
     l21_20.setLength(length);
     network.addLink(l21_20);
     
     Link l30_31 = netFactory.createLink(scenario.createId("30_31"), n30, n31);
     l30_31.setLength(length);
     network.addLink(l30_31);
     Link l31_30 = netFactory.createLink(scenario.createId("31_30"), n31, n30);
     l31_30.setLength(length);
     network.addLink(l31_30);
     
     Link l30_40 = netFactory.createLink(scenario.createId("30_40"), n30, n40);
     l30_40.setLength(length);
     l30_40.setCapacity(capacity);
     l30_40.setFreespeed(freespeed);
     l30_40.setNumberOfLanes(1.0);
     network.addLink(l30_40);
     
     Link l40_30 = netFactory.createLink(scenario.createId("40_30"), n40, n30);
     l40_30.setLength(length);
     l40_30.setCapacity(capacity);
     l40_30.setFreespeed(freespeed);
     l40_30.setNumberOfLanes(1.0);
     network.addLink(l40_30);

     Link l40_41 = netFactory.createLink(scenario.createId("40_41"), n40, n41);
     l40_41.setLength(length);
     l40_41.setCapacity(capacity);
     l40_41.setFreespeed(freespeed);
     l40_41.setNumberOfLanes(1.0);
     network.addLink(l40_41);

     Link l41_40 = netFactory.createLink(scenario.createId("41_40"), n41, n40);
     l41_40.setLength(length);
     l41_40.setCapacity(capacity);
     l41_40.setFreespeed(freespeed);
     l41_40.setNumberOfLanes(1.0);
     network.addLink(l41_40);

     Link l40_70 = netFactory.createLink(scenario.createId("40_70"), n40, n70);
     l40_70.setLength(length);
     l40_70.setCapacity(capacity);
     l40_70.setFreespeed(freespeed);
     l40_70.setNumberOfLanes(1.0);
     network.addLink(l40_70);

     Link l50_40 = netFactory.createLink(scenario.createId("50_40"), n50, n40);
     l50_40.setLength(length);
     l50_40.setCapacity(capacity);
     l50_40.setFreespeed(freespeed);
     l50_40.setNumberOfLanes(1.0);
     network.addLink(l50_40);

     Link l50_51 = netFactory.createLink(scenario.createId("50_51"), n50, n51);
     l50_51.setLength(length);
     l50_51.setCapacity(capacity);
     l50_51.setFreespeed(freespeed);
     l50_51.setNumberOfLanes(1.0);
     network.addLink(l50_51);

     Link l51_50 = netFactory.createLink(scenario.createId("51_50"), n51, n50);
     l51_50.setLength(length);
     l51_50.setCapacity(capacity);
     l51_50.setFreespeed(freespeed);
     l51_50.setNumberOfLanes(1.0);
     network.addLink(l51_50);

     Link l70_50 = netFactory.createLink(scenario.createId("70_50"), n70, n50);
     l70_50.setLength(length);
     l70_50.setCapacity(capacity);
     l70_50.setFreespeed(freespeed);
     l70_50.setNumberOfLanes(1.0);
     network.addLink(l70_50);

     Link l70_71 = netFactory.createLink(scenario.createId("70_71"), n70, n71);
     l70_71.setLength(length);
     l70_71.setCapacity(capacity);
     l70_71.setFreespeed(freespeed);
     l70_71.setNumberOfLanes(1.0);
     network.addLink(l70_71);
     
     Link l71_70 = netFactory.createLink(scenario.createId("71_70"), n71, n70);
     l71_70.setLength(length);
     l71_70.setCapacity(capacity);
     l71_70.setFreespeed(freespeed);
     l71_70.setNumberOfLanes(1.0);
     network.addLink(l71_70);

     Link l72_70 = netFactory.createLink(scenario.createId("72_70"), n72, n70);
     l72_70.setLength(length);
     l72_70.setCapacity(capacity);
     l72_70.setFreespeed(freespeed);
     l72_70.setNumberOfLanes(1.0);
     network.addLink(l72_70);

     Link l70_90 = netFactory.createLink(scenario.createId("70_90"), n70, n90);
     l70_90.setLength(length);
     l70_90.setCapacity(capacity);
     l70_90.setFreespeed(freespeed);
     l70_90.setNumberOfLanes(1.0);
     network.addLink(l70_90);

     Link l90_80 = netFactory.createLink(scenario.createId("90_80"), n90, n80);
     l90_80.setLength(length);
     l90_80.setCapacity(capacity);
     l90_80.setFreespeed(freespeed);
     l90_80.setNumberOfLanes(1.0);
     network.addLink(l90_80);

     Link l80_70 = netFactory.createLink(scenario.createId("80_70"), n80, n70);
     l80_70.setLength(length);
     l80_70.setCapacity(capacity);
     l80_70.setFreespeed(freespeed);
     l80_70.setNumberOfLanes(1.0);
     network.addLink(l80_70);

     Link l80_81 = netFactory.createLink(scenario.createId("80_81"), n80, n81);
     l80_81.setLength(length);
     l80_81.setCapacity(capacity);
     l80_81.setFreespeed(freespeed);
     l80_81.setNumberOfLanes(1.0);
     network.addLink(l80_81);

     Link l81_80 = netFactory.createLink(scenario.createId("81_80"), n81, n80);
     l81_80.setLength(length);
     l81_80.setCapacity(capacity);
     l81_80.setFreespeed(freespeed);
     l81_80.setNumberOfLanes(1.0);
     network.addLink(l81_80);

     Link l90_91 = netFactory.createLink(scenario.createId("90_91"), n90, n91);
     l90_91.setLength(length);
     l90_91.setCapacity(capacity);
     l90_91.setFreespeed(freespeed);
     l90_91.setNumberOfLanes(1.0);
     network.addLink(l90_91);

     Link l91_90 = netFactory.createLink(scenario.createId("91_90"), n91, n90);
     l91_90.setLength(length);
     l91_90.setCapacity(capacity);
     l91_90.setFreespeed(freespeed);
     l91_90.setNumberOfLanes(2.0);
     network.addLink(l91_90);

     Link l90_92 = netFactory.createLink(scenario.createId("90_92"), n90, n92);
     l90_92.setLength(length);
     l90_92.setCapacity(capacity);
     l90_92.setFreespeed(freespeed);
     l90_92.setNumberOfLanes(2.0);
     network.addLink(l90_92);

    
     LaneDefinitions lanes = scenario.getLaneDefinitions();
     LaneDefinitionsFactory laneFactory = lanes.getFactory();
     //lanes for link 12_10 will not work with current network -> modified the network -> tutorial
     LanesToLinkAssignment l2l = laneFactory.createLanesToLinkAssignment(l12_10.getId());
     Lane lane = laneFactory.createLane(scenario.createId("12_10_1"));
     lane.addToLinkId(l10_13.getId());
     lane.setStartsAtMeterFromLinkEnd(90.0);
     l2l.addLane(lane);

     lane = laneFactory.createLane(scenario.createId("12_10_2"));
     lane.addToLinkId(l10_20.getId());
     lane.setStartsAtMeterFromLinkEnd(90.0);
     l2l.addLane(lane);
     
     lane = laneFactory.createLane(scenario.createId("12_10_3"));
     lane.addToLinkId(l10_20.getId());
     lane.addToLinkId(l10_11.getId());
     lane.setStartsAtMeterFromLinkEnd(100.0);
     l2l.addLane(lane);
     
     lanes.addLanesToLinkAssignment(l2l);
   
     //lanes for link 11_10
     l2l = laneFactory.createLanesToLinkAssignment(l11_10.getId());
     lane = laneFactory.createLane(scenario.createId("11_10_1"));
     lane.addToLinkId(l10_12.getId());
     lane.setStartsAtMeterFromLinkEnd(320.0);
     l2l.addLane(lane);

     lane = laneFactory.createLane(scenario.createId("11_10_2"));
     lane.addToLinkId(l10_13.getId());
     lane.setStartsAtMeterFromLinkEnd(220.0);
     lane.setNumberOfRepresentedLanes(2.0);
     l2l.addLane(lane);
     
     lane = laneFactory.createLane(scenario.createId("11_10_3"));
     lane.addToLinkId(l10_20.getId());
     lane.setStartsAtMeterFromLinkEnd(220.0);
     l2l.addLane(lane);

     lanes.addLanesToLinkAssignment(l2l);
     
     //lanes for link 30_10
     l2l = laneFactory.createLanesToLinkAssignment(l30_10.getId());
     lane = laneFactory.createLane(scenario.createId("30_10_1"));
     lane.addToLinkId(l10_11.getId());
     lane.setStartsAtMeterFromLinkEnd(120.0);
     lane.setNumberOfRepresentedLanes(1.67);
     l2l.addLane(lane);
     
     lane = laneFactory.createLane(scenario.createId("30_10_2"));
     lane.addToLinkId(l10_12.getId());
     lane.setNumberOfRepresentedLanes(2.0);
     lane.setStartsAtMeterFromLinkEnd(50.0);
     l2l.addLane(lane);
     
     lane = laneFactory.createLane(scenario.createId("30_10_3"));
     lane.addToLinkId(l10_13.getId());
     lane.setStartsAtMeterFromLinkEnd(50.0);
     lane.setNumberOfRepresentedLanes(1.0);
     l2l.addLane(lane);
     
     lanes.addLanesToLinkAssignment(l2l);
     
     //lanes for link 13_10
     l2l = laneFactory.createLanesToLinkAssignment(l13_10.getId());
     lane = laneFactory.createLane(scenario.createId("13_10_1"));
     lane.addToLinkId(l10_20.getId());
     lane.setStartsAtMeterFromLinkEnd(200.0);
     l2l.addLane(lane);

     lane = laneFactory.createLane(scenario.createId("13_10_2"));
     lane.addToLinkId(l10_11.getId());
     lane.setStartsAtMeterFromLinkEnd(200.0);
     lane.setNumberOfRepresentedLanes(2.0);
     l2l.addLane(lane);
     
     lane = laneFactory.createLane(scenario.createId("13_10_3"));
     lane.addToLinkId(l10_12.getId());
     lane.setStartsAtMeterFromLinkEnd(250.0);
     l2l.addLane(lane);
     
     lanes.addLanesToLinkAssignment(l2l);

     //lanes for link 30_40
     l2l = laneFactory.createLanesToLinkAssignment(l30_40.getId());
     lane = laneFactory.createLane(scenario.createId("30_40_1"));
     lane.addToLinkId(l40_70.getId());
     lane.setStartsAtMeterFromLinkEnd(20.0);
     l2l.addLane(lane);

     lane = laneFactory.createLane(scenario.createId("30_40_2"));
     lane.addToLinkId(l40_70.getId());
     lane.addToLinkId(l40_41.getId());
     lane.setStartsAtMeterFromLinkEnd(20.0);
     l2l.addLane(lane);
     
     lanes.addLanesToLinkAssignment(l2l);

     
     //lanes for link 40_70
     l2l = laneFactory.createLanesToLinkAssignment(l40_70.getId());
     lane = laneFactory.createLane(scenario.createId("40_70_1"));
     lane.addToLinkId(l70_90.getId());
     lane.setStartsAtMeterFromLinkEnd(100.0);
     l2l.addLane(lane);

     lane = laneFactory.createLane(scenario.createId("40_70_2"));
     lane.addToLinkId(l70_90.getId());
     lane.addToLinkId(l70_71.getId());
     lane.setNumberOfRepresentedLanes(1.0);
     lane.setStartsAtMeterFromLinkEnd(100.0);
     l2l.addLane(lane);
     
     lanes.addLanesToLinkAssignment(l2l);
     
     //lanes for link 72_70
     l2l = laneFactory.createLanesToLinkAssignment(l72_70.getId());
     lane = laneFactory.createLane(scenario.createId("72_70_1"));
     lane.addToLinkId(l70_90.getId());
     lane.setStartsAtMeterFromLinkEnd(70.0);
     l2l.addLane(lane);

     lane = laneFactory.createLane(scenario.createId("72_70_2"));
     lane.addToLinkId(l70_71.getId());
     lane.setStartsAtMeterFromLinkEnd(70.0);
     l2l.addLane(lane);
     
     lane = laneFactory.createLane(scenario.createId("72_70_3"));
     lane.addToLinkId(l70_50.getId());
     lane.setStartsAtMeterFromLinkEnd(70.0);
     l2l.addLane(lane);
     
     lanes.addLanesToLinkAssignment(l2l);
     
     //lanes for link 80_70
     l2l = laneFactory.createLanesToLinkAssignment(l80_70.getId());
     lane = laneFactory.createLane(scenario.createId("80_70_1"));
     lane.addToLinkId(l70_50.getId());
     lane.setNumberOfRepresentedLanes(2.0);
     lane.setStartsAtMeterFromLinkEnd(70.0);
     l2l.addLane(lane);

     lanes.addLanesToLinkAssignment(l2l);

     //lanes for link 70_90
     l2l = laneFactory.createLanesToLinkAssignment(l70_90.getId());
     lane = laneFactory.createLane(scenario.createId("70_90_1"));
     lane.addToLinkId(l90_92.getId());
     lane.setNumberOfRepresentedLanes(2.0);
     lane.setStartsAtMeterFromLinkEnd(100.0);
     l2l.addLane(lane);

     lane = laneFactory.createLane(scenario.createId("70_90_2"));
     lane.addToLinkId(l90_91.getId());
     lane.setNumberOfRepresentedLanes(1.0);
     lane.setStartsAtMeterFromLinkEnd(100.0);
     l2l.addLane(lane);
     
     lanes.addLanesToLinkAssignment(l2l);

     //lanes for link 91_90
     l2l = laneFactory.createLanesToLinkAssignment(l91_90.getId());
     lane = laneFactory.createLane(scenario.createId("91_90_1"));
     lane.addToLinkId(l90_80.getId());
     lane.setStartsAtMeterFromLinkEnd(100.0);
     l2l.addLane(lane);

     lane = laneFactory.createLane(scenario.createId("91_90_2"));
     lane.addToLinkId(l90_92.getId());
     lane.setStartsAtMeterFromLinkEnd(100.0);
     l2l.addLane(lane);
     
     lanes.addLanesToLinkAssignment(l2l);
     
     //create the traffic signal infrastructure
     SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);
     SignalSystemsData signals = signalsData.getSignalSystemsData();
     SignalGroupsData groups = signalsData.getSignalGroupsData();

     SignalSystemsDataFactory sf = signals.getFactory();
     
     //signals at node 10
     SignalSystemData system = sf.createSignalSystemData(scenario.createId("ss10"));
     signals.addSignalSystemData(system);

     SignalData signal = sf.createSignalData(scenario.createId("s12_10_1"));
     signal.setLinkId(scenario.createId("12_10"));
     signal.addLaneId(scenario.createId("12_10_1"));
     system.addSignalData(signal);
     
     signal = sf.createSignalData(scenario.createId("s12_10_2"));
     signal.setLinkId(scenario.createId("12_10"));
     signal.addLaneId(scenario.createId("12_10_2"));
     signal.addLaneId(scenario.createId("12_10_3"));
     system.addSignalData(signal);

     signal = sf.createSignalData(scenario.createId("s12_10_3"));
     signal.setLinkId(scenario.createId("12_10"));
     signal.addLaneId(scenario.createId("12_10_3"));
     system.addSignalData(signal);
     
     signal = sf.createSignalData(scenario.createId("s11_10_1"));
     signal.setLinkId(scenario.createId("11_10"));
     signal.addLaneId(scenario.createId("11_10_1"));
     system.addSignalData(signal);
     
     signal = sf.createSignalData(scenario.createId("s11_10_2"));
     signal.setLinkId(scenario.createId("11_10"));
     signal.addLaneId(scenario.createId("11_10_2"));
     system.addSignalData(signal);
     
     signal = sf.createSignalData(scenario.createId("s11_10_3"));
     signal.setLinkId(scenario.createId("11_10"));
     signal.addLaneId(scenario.createId("11_10_3"));
     system.addSignalData(signal);

     signal = sf.createSignalData(scenario.createId("s30_10_1"));
     signal.setLinkId(scenario.createId("30_10"));
     signal.addLaneId(scenario.createId("30_10_1"));
     system.addSignalData(signal);
     
     signal = sf.createSignalData(scenario.createId("s30_10_2"));
     signal.setLinkId(scenario.createId("30_10"));
     signal.addLaneId(scenario.createId("30_10_2"));
     system.addSignalData(signal);
     
     signal = sf.createSignalData(scenario.createId("s30_10_3"));
     signal.setLinkId(scenario.createId("30_10"));
     signal.addLaneId(scenario.createId("30_10_3"));
     system.addSignalData(signal);

     signal = sf.createSignalData(scenario.createId("s30_10_1"));
     signal.setLinkId(scenario.createId("30_10"));
     signal.addLaneId(scenario.createId("30_10_1"));
     system.addSignalData(signal);
     
     signal = sf.createSignalData(scenario.createId("s30_10_2"));
     signal.setLinkId(scenario.createId("30_10"));
     signal.addLaneId(scenario.createId("30_10_2"));
     system.addSignalData(signal);
     
     signal = sf.createSignalData(scenario.createId("s30_10_3"));
     signal.setLinkId(scenario.createId("30_10"));
     signal.addLaneId(scenario.createId("30_10_3"));
     system.addSignalData(signal);

     signal = sf.createSignalData(scenario.createId("s13_10_1"));
     signal.setLinkId(scenario.createId("13_10"));
     signal.addLaneId(scenario.createId("13_10_1"));
     system.addSignalData(signal);
     
     signal = sf.createSignalData(scenario.createId("s13_10_2"));
     signal.setLinkId(scenario.createId("13_10"));
     signal.addLaneId(scenario.createId("13_10_2"));
     system.addSignalData(signal);
     
     signal = sf.createSignalData(scenario.createId("s13_10_3"));
     signal.setLinkId(scenario.createId("13_10"));
     signal.addLaneId(scenario.createId("13_10_3"));
     system.addSignalData(signal);

     //create SignalGroups 
     SignalGroupsDataFactory gf = groups.getFactory();
     SignalGroupData group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_1"));
     group.addSignalId(scenario.createId("s30_10_1"));
     group.addSignalId(scenario.createId("s30_10_2"));
     groups.addSignalGroupData(group);
     
     group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_0"));
     group.addSignalId(scenario.createId("s30_10_3"));
     groups.addSignalGroupData(group);
     
     group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_2"));
     group.addSignalId(scenario.createId("s11_10_3"));
     groups.addSignalGroupData(group);
     
     group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_3"));
     group.addSignalId(scenario.createId("s11_10_1"));
     group.addSignalId(scenario.createId("s11_10_2"));
     groups.addSignalGroupData(group);
     
     group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_4_1"));
     group.addSignalId(scenario.createId("s12_10_3"));
     groups.addSignalGroupData(group);

     group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_4_2"));
     group.addSignalId(scenario.createId("s12_10_3"));
     groups.addSignalGroupData(group);
     
     group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_5"));
     group.addSignalId(scenario.createId("s12_10_1"));
     group.addSignalId(scenario.createId("s12_10_2"));
     groups.addSignalGroupData(group);

     group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_6_1"));
     group.addSignalId(scenario.createId("s13_10_3"));
     groups.addSignalGroupData(group);

     group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_6_2"));
     group.addSignalId(scenario.createId("s13_10_3"));
     groups.addSignalGroupData(group);
     
     group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_7"));
     group.addSignalId(scenario.createId("s13_10_2"));
     groups.addSignalGroupData(group);
     
     group = gf.createSignalGroupData(scenario.createId("ss10"), 
             scenario.createId("sg_8"));
     group.addSignalId(scenario.createId("s13_10_1"));
     groups.addSignalGroupData(group);
     
     
     //TODO signal system for next node
     
     
     
	 }
	
	 
		private static void createSignalControl(Scenario scenario) {
			SignalsData sd = scenario.getScenarioElement(SignalsData.class);
			SignalControlData control = sd.getSignalControlData();
			SignalControlDataFactory scf = control.getFactory();
			
			SignalSystemControllerData ssController = scf.createSignalSystemControllerData(scenario.createId("ss10"));
			control.addSignalSystemControllerData(ssController);
			//fixed-time control
			ssController.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER); 
			SignalPlanData plan = scf.createSignalPlanData(scenario.createId("ss10_p1"));
			ssController.addSignalPlanData(plan);
			plan.setCycleTime(113);
			plan.setOffset(0); //coordination offset
			//now the single signals (signal groups)
			SignalGroupSettingsData settings = scf.createSignalGroupSettingsData(scenario.createId("sg_0"));
			//green from second 0 to 30 in cycle
			settings.setOnset(25);
			settings.setDropping(20); 
			plan.addSignalGroupSettings(settings);
			
			settings = scf.createSignalGroupSettingsData(scenario.createId("sg_1"));
			//green from second 0 to 30 in cycle
			settings.setOnset(112);
			settings.setDropping(20); 
//			plan.addSignalGroupSettings(settings);
			
			settings = scf.createSignalGroupSettingsData(scenario.createId("sg_2"));
			//green from second 0 to 30 in cycle
			settings.setOnset(1);
			settings.setDropping(88); 
			plan.addSignalGroupSettings(settings);

			settings = scf.createSignalGroupSettingsData(scenario.createId("sg_3"));
			//green from second 0 to 30 in cycle
			settings.setOnset(50);
			settings.setDropping(88); 
			plan.addSignalGroupSettings(settings);

			settings = scf.createSignalGroupSettingsData(scenario.createId("sg_4_1"));
			//green from second 0 to 30 in cycle
			settings.setOnset(67);
			settings.setDropping(16);
			plan.addSignalGroupSettings(settings);

			settings = scf.createSignalGroupSettingsData(scenario.createId("sg_4_2"));
			//green from second 0 to 30 in cycle
			settings.setOnset(25);
			settings.setDropping(44);
			plan.addSignalGroupSettings(settings);
			
			settings = scf.createSignalGroupSettingsData(scenario.createId("sg_5"));
			//green from second 0 to 30 in cycle
			settings.setOnset(25);
			settings.setDropping(44); 
			plan.addSignalGroupSettings(settings);

			settings = scf.createSignalGroupSettingsData(scenario.createId("sg_6_1"));
			//green from second 0 to 30 in cycle
			settings.setOnset(15);
			settings.setDropping(44); 
			plan.addSignalGroupSettings(settings);

			settings = scf.createSignalGroupSettingsData(scenario.createId("sg_6_2"));
            //green from second 0 to 30 in cycle
            settings.setOnset(50);
            settings.setDropping(88); 
            plan.addSignalGroupSettings(settings);
			
			settings = scf.createSignalGroupSettingsData(scenario.createId("sg_7"));
			//green from second 0 to 30 in cycle
			settings.setOnset(50);
			settings.setDropping(88); 
			plan.addSignalGroupSettings(settings);
			
			settings = scf.createSignalGroupSettingsData(scenario.createId("sg_8"));
			//green from second 30 to 55 in cycle
			settings.setOnset(93);
			settings.setDropping(110); 
			plan.addSignalGroupSettings(settings);

}

		private static void createAmbertimes(SignalsData signalsData) {
		    AmberTimesData amberTimes = signalsData.getAmberTimesData();
		    amberTimes.setDefaultAmber(3);
		    amberTimes.setDefaultRedAmber(1);
		}
		
    
	    private static void createPopulation(ScenarioImpl scenario)
	    {
	        Population pop = scenario.getPopulation();
	        PopulationFactory pf = pop.getFactory();
	        for (int i = 1; i <= 100; i++) {
	            Person person = pf.createPerson(scenario.createId(Integer.toString(i)));
	            pop.addPerson(person);
	            Plan plan = pf.createPlan();
	            Activity homeAct = pf.createActivityFromLinkId("home", scenario.createId("91_90"));
	            homeAct.setEndTime(120 + i * 5);
	            plan.addActivity(homeAct);
	            Leg leg = pf.createLeg(TransportMode.car);
	            plan.addLeg(leg);
	            homeAct = pf.createActivityFromLinkId("home", scenario.createId("10_12"));
	            plan.addActivity(homeAct);
	            person.addPlan(plan);
	        }
	    }

		
	 
    public static void main(String[] args)
    {
    		//general setup
        Config config = ConfigUtils.createConfig();
        config.scenario().setUseLanes(true);
        config.scenario().setUseSignalSystems(true);
        ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(config);
        scenario.addScenarioElement(new SignalsDataImpl());
        
        //create network lanes and signals
        createPhysics(scenario);
        LaneDefinitions lanes20 = new LaneDefinitionsV11ToV20Conversion().convertTo20(scenario.getLaneDefinitions(), scenario.getNetwork());
        LanesConsistencyChecker lcc = new LanesConsistencyChecker(scenario.getNetwork(), lanes20);
        lcc.checkConsistency();
        SignalSystemsDataConsistencyChecker sscc = new SignalSystemsDataConsistencyChecker(scenario);
        sscc.checkConsistency();
        
        SignalGroupsDataConsistencyChecker sgcc = new SignalGroupsDataConsistencyChecker(scenario);
        sgcc.checkConsistency();
        
        //create the signal control
        createSignalControl(scenario);
        SignalControlDataConsistencyChecker sccc = new SignalControlDataConsistencyChecker(scenario);
        sccc.checkConsistency();
        
        createAmbertimes(scenario.getScenarioElement(SignalsData.class));
        
        createPopulation(scenario);
        
//        System.exit(0);
        
        //output
//        String baseDir = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\";
        String baseDir = "/media/data/work/matsim/examples/poznan/";
        
        SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
        String signalSystemsFile = baseDir + "signal_systems.xml";
    		signalsWriter.setSignalSystemsOutputFilename(signalSystemsFile);
    		config.signalSystems().setSignalSystemFile(signalSystemsFile);
    		String signalGroupsFile = baseDir + "signal_groups.xml";
    		signalsWriter.setSignalGroupsOutputFilename(signalGroupsFile);
    		config.signalSystems().setSignalGroupsFile(signalGroupsFile);
    		String signalControlFile = baseDir + "signal_control.xml";
    		signalsWriter.setSignalControlOutputFilename(signalControlFile);
    		config.signalSystems().setSignalControlFile(signalControlFile);
    		String amberTimesFile = baseDir + "amber_times.xml";
    		signalsWriter.setAmberTimesOutputFilename(amberTimesFile);
    		config.signalSystems().setAmberTimesFile(amberTimesFile);
    		signalsWriter.writeSignalsData(scenario.getScenarioElement(SignalsData.class));
        
        String lanesOutputFile = baseDir + "lanes.xml";
//        String lanesOutputFile = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\lanes.xml";
        new LaneDefinitionsWriter11(scenario.getLaneDefinitions()).write(lanesOutputFile);
//        config.network().setLaneDefinitionsFile(lanesOutputFile);
        
        String lanes20OutputFile = baseDir + "lanes20.xml";        
//        String lanes20OutputFile = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\lanes20.xml";        
        new LaneDefinitionsWriter20(lanes20).write(lanes20OutputFile);
        config.network().setLaneDefinitionsFile(lanes20OutputFile);

        String popFilename = baseDir + "population.xml";
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(popFilename);
        config.plans().setInputFile(popFilename);
        
        String networkFilename = baseDir + "network.xml";
//        String networkFilename = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\network.xml";
        new NetworkWriter(scenario.getNetwork()).write(networkFilename);
        config.network().setInputFile(networkFilename);
        String configFilename = baseDir + "config.xml";
        new ConfigWriter(config).write(configFilename);
//        String configFilename = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\config.xml";
        
        
        
        //visualization
        new DgOTFVis().playAndRouteConfig(configFilename);
    }


}