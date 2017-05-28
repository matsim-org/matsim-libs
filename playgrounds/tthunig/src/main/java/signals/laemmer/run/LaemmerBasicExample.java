package signals.laemmer.run;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.*;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesFactory;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import signals.CombinedSignalsModule;
import signals.laemmer.model.LaemmerConfig;
import signals.laemmer.model.LaemmerSignalController;

import java.io.IOException;
import java.util.*;

/**
 * Created by nkuehnel on 03.04.2017.
 */
public class LaemmerBasicExample {
    private static final Logger log = Logger.getLogger(LaemmerMain.class);

    private static final int LANE_CAPACITY = 1800;

    /**
     * @param args
     */
    public static void main(String[] args) {
        log.info("Running Laemmer main method...");

//        for (int i = 0; i <= 2520; i += 120) {
        run(360, 480, LaemmerConfig.Regime.COMBINED, false, false, false, true, false, true);
//            run(360, i, LaemmerConfig.Regime.OPTIMIZING, false, false, false, true, false, true);
//            run(360, i, LaemmerConfig.Regime.STABILIZING, false, false, false, true, false, true);
//        }
    }

    private static void run(double flowNS, double flowWE, LaemmerConfig.Regime regime, boolean vis, boolean log, boolean stochastic, boolean lanes, boolean liveArrivalRates, boolean grouped) {

        String outputPath;
        if (stochastic) {
            if (lanes) {
                if(liveArrivalRates) {
                    outputPath = "output/laemmerBasic" + regime.name() + "_ew" + flowWE + "_ns" + flowNS + "_stochastic_lanes_liveArrival";
                } else {
                    outputPath = "output/laemmerBasic" + regime.name() + "_ew" + flowWE + "_ns" + flowNS + "_stochastic_lanes";
                }
            } else {
                if(liveArrivalRates) {
                    outputPath = "output/laemmerBasic" + regime.name() + "_ew" + flowWE + "_ns" + flowNS + "_stochastic_noLanes_liveArrival";
                } else {
                    outputPath = "output/laemmerBasic" + regime.name() + "_ew" + flowWE + "_ns" + flowNS + "_stochastic_noLanes_";
                }
            }
        } else {
            if (lanes) {
                if(liveArrivalRates) {
                    outputPath = "output/laemmerBasic" + regime.name() + "_ew" + flowWE + "_ns" + flowNS + "_constant_lanes_liveArrival";
                } else {
                    outputPath = "output/laemmerBasic" + regime.name() + "_ew" + flowWE + "_ns" + flowNS + "_constant_lanes";
                }
            } else {
                if(liveArrivalRates) {
                    outputPath = "output/laemmerBasic" + regime.name() + "_ew" + flowWE + "_ns" + flowNS + "_constant_noLanes_liveArrival";
                } else {
                    outputPath = "output/laemmerBasic" + regime.name() + "_ew" + flowWE + "_ns" + flowNS + "_constant_noLanes";
                }
            }
        }
        final Config config = defineConfig(outputPath, lanes);

        CombinedSignalsModule module = new CombinedSignalsModule();
        LaemmerConfig laemmerConfig = new LaemmerConfig();
        laemmerConfig.setDEFAULT_INTERGREEN(5);

        if(grouped) {
            laemmerConfig.setDESIRED_PERIOD(60);
            laemmerConfig.setMAX_PERIOD(90);
        } else {
            laemmerConfig.setDESIRED_PERIOD(120);
            laemmerConfig.setMAX_PERIOD(180);
        }
        laemmerConfig.setActiveRegime(regime);
        module.setLaemmerConfig(laemmerConfig);

        final Scenario scenario = defineScenario(config, flowNS, flowWE, laemmerConfig, stochastic, lanes, liveArrivalRates, grouped);
        Controler controler = new Controler(scenario);


        controler.addOverridingModule(module);

        if (vis) {
            config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
            config.qsim().setNodeOffset(5.);
            OTFVisConfigGroup otfvisConfig =
                    ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
            otfvisConfig.setScaleQuadTreeRect(true);
            otfvisConfig.setColoringScheme(OTFVisConfigGroup.ColoringScheme.byId);
            otfvisConfig.setAgentSize(240);
            controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
        }
        if (log) {
            try {
                LaemmerSignalController.log.addAppender(new FileAppender(new SimpleLayout(), "logs/main.txt"));
//                LaemmerSignalController.signalLog.addAppender(new FileAppender(new SimpleLayout(), "logs/driveways.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LaemmerSignalController.log.setLevel(Level.OFF);
        }
        LaemmerSignalController.signalLog.setLevel(Level.OFF);
        controler.run();
    }

    private static Config defineConfig(String outputPath, boolean lanes) {
        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory(outputPath);

        config.controler().setLastIteration(0);
        config.travelTimeCalculator().setMaxTime(60 * 120);
        config.qsim().setStartTime(0);
        config.qsim().setEndTime(60 * 120);
        config.qsim().setUsingFastCapacityUpdate(false);

        if (lanes) {
            config.qsim().setUseLanes(true);
            config.controler().setLinkToLinkRoutingEnabled(true);
            config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
        }

        SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        signalConfigGroup.setUseSignalSystems(true);

        PlanCalcScoreConfigGroup.ActivityParams dummyAct = new PlanCalcScoreConfigGroup.ActivityParams("dummy");
        dummyAct.setTypicalDuration(12 * 3600);
        config.planCalcScore().addActivityParams(dummyAct);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());
        config.controler().setWritePlansInterval(config.controler().getLastIteration());
        config.vspExperimental().setWritingOutputEvents(true);
        config.planCalcScore().setWriteExperiencedPlans(true);
        config.controler().setCreateGraphs(true);

        return config;
    }

    private static Scenario defineScenario(Config config, double flowNS, double flowWE, LaemmerConfig laemmerConfig, boolean stochastic, boolean lanes, boolean liveArrivalRates, boolean grouped) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        // add missing scenario elements
        SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        createNetwork(scenario);
        if (lanes) {
            createLanes(scenario);
        }
        createPopulation(scenario, flowNS, flowWE, stochastic);
        createSignals(scenario, laemmerConfig, flowNS, flowWE, lanes, liveArrivalRates, grouped);
        return scenario;
    }

    /**
     * creates a network like this:
     * <p>
     * 6
     * ^
     * |
     * v
     * 7
     * ^
     * |
     * v
     * 1 <===> 2 <===> 3 <===> 4 <===> 5
     * ^
     * |
     * v
     * 8
     * ^
     * |
     * v
     * 9
     *
     * @param scenario
     */
    private static void createNetwork(Scenario scenario) {
        Network net = scenario.getNetwork();
        NetworkFactory fac = net.getFactory();

        //left crossing
        net.addNode(fac.createNode(Id.createNodeId(1), new Coord(-2000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(2), new Coord(-1000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(3), new Coord(0, 0)));
        net.addNode(fac.createNode(Id.createNodeId(4), new Coord(1000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(5), new Coord(2000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(6), new Coord(0, 2000)));
        net.addNode(fac.createNode(Id.createNodeId(7), new Coord(0, 1000)));
        net.addNode(fac.createNode(Id.createNodeId(8), new Coord(0, -1000)));
        net.addNode(fac.createNode(Id.createNodeId(9), new Coord(0, -2000)));


        String[] linksNorthSouth = {"6_7", "7_6", "7_3", "3_7", "3_8", "8_3", "8_9", "9_8"};

        String[] linksWestEast = {"1_2", "2_1", "2_3", "3_2", "3_4", "4_3", "4_5", "5_4"};

        for (String linkId : linksNorthSouth) {
            String fromNodeId = linkId.split("_")[0];
            String toNodeId = linkId.split("_")[1];
            Link link = fac.createLink(Id.createLinkId(linkId),
                    net.getNodes().get(Id.createNodeId(fromNodeId)),
                    net.getNodes().get(Id.createNodeId(toNodeId)));
            link.setCapacity(1800);
            link.setLength(1000);
            link.setFreespeed(13.889);
            Set<String> modes = new HashSet<>();
            modes.add("car");
            link.setAllowedModes(modes);
            net.addLink(link);
        }

        for (String linkId : linksWestEast) {
            String fromNodeId = linkId.split("_")[0];
            String toNodeId = linkId.split("_")[1];
            Link link = fac.createLink(Id.createLinkId(linkId),
                    net.getNodes().get(Id.createNodeId(fromNodeId)),
                    net.getNodes().get(Id.createNodeId(toNodeId)));
            link.setCapacity(3600);
            link.setNumberOfLanes(2);
            link.setLength(1000);
            link.setFreespeed(13.889);
            Set<String> modes = new HashSet<>();
            modes.add("car");
            link.setAllowedModes(modes);
            net.addLink(link);
        }

    }

    private static void createLanes(Scenario scenario) {
        Lanes lanes = scenario.getLanes();
        LanesFactory factory = lanes.getFactory();


        // create lanes for link 2_3
        LanesToLinkAssignment lanesForLink2_3 = factory
                .createLanesToLinkAssignment(Id.createLinkId("2_3"));
        lanes.addLanesToLinkAssignment(lanesForLink2_3);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink2_3, factory,
                Id.create("2_3.ol", Lane.class), 3600, 1000, 0, 1,
                null, Arrays.asList(Id.create("2_3.l", Lane.class), Id.create("2_3.r", Lane.class)));


        // straight and left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink2_3, factory,
                Id.create("2_3.l", Lane.class), LANE_CAPACITY, 500, 1, 1,
                Arrays.asList(Id.create("3_4", Link.class), Id.create("3_7", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink2_3, factory,
                Id.create("2_3.r", Lane.class), LANE_CAPACITY, 500, -1, 1,
                Arrays.asList(Id.create("3_4", Link.class), Id.create("3_8", Link.class)), null);


        // create lanes for link 4_3
        LanesToLinkAssignment lanesForLink4_3 = factory
                .createLanesToLinkAssignment(Id.create("4_3", Link.class));
        lanes.addLanesToLinkAssignment(lanesForLink4_3);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink4_3, factory,
                Id.create("4_3.ol", Lane.class), 3600, 1000, 0, 1,
                null, Arrays.asList(Id.create("4_3.l", Lane.class), Id.create("4_3.r", Lane.class)));

        // straight and left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink4_3, factory,
                Id.create("4_3.l", Lane.class), LANE_CAPACITY, 500, 1, 1,
                Arrays.asList(Id.create("3_2", Link.class), Id.create("3_8", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink4_3, factory,
                Id.create("4_3.r", Lane.class), LANE_CAPACITY, 500, -1, 1,
                Arrays.asList(Id.create("3_2", Link.class), Id.create("3_7", Link.class)), null);


        // create lanes for link 7_3
        LanesToLinkAssignment lanesForLink7_3 = factory
                .createLanesToLinkAssignment(Id.create("7_3", Link.class));
        lanes.addLanesToLinkAssignment(lanesForLink7_3);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink7_3, factory,
                Id.create("7_3.ol", Lane.class), LANE_CAPACITY, 1000, 0, 1,
                Arrays.asList(Id.create("3_4", Link.class), Id.create("3_2", Link.class), Id.create("3_8", Link.class)), null);

        // create lanes for link 8_3
        LanesToLinkAssignment lanesForLink8_3 = factory
                .createLanesToLinkAssignment(Id.create("8_3", Link.class));
        lanes.addLanesToLinkAssignment(lanesForLink8_3);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink8_3, factory,
                Id.create("8_3.ol", Lane.class), LANE_CAPACITY, 1000, 0, 1,
                Arrays.asList(Id.create("3_2", Link.class), Id.create("3_7", Link.class), Id.create("3_4", Link.class)), null);

    }

    private static void createPopulation(Scenario scenario, double flowNS, double flowWE, boolean stochastic) {
        Population population = scenario.getPopulation();

        String[] linksNS = {"6_7-8_9", "9_8-7_6"};
        String[] linksWE = {"5_4-2_1", "1_2-4_5"};

        Random rnd = new Random(14);
        createPopulationForRelation(flowNS, population, linksNS, stochastic, rnd);
        createPopulationForRelation(flowWE, population, linksWE, stochastic, rnd);
    }

    private static void createPopulationForRelation(double flow, Population population, String[] links, boolean stochastic, Random rnd) {

        double lambdaT = (flow / 3600) / 5;
        double lambdaN = 1. / 5.;

        if (flow == 0) {
            return;
        }


        for (String od : links) {
            String fromLinkId = od.split("-")[0];
            String toLinkId = od.split("-")[1];
            Map<Double, Integer> insertNAtSecond = new TreeMap<>();
            if (stochastic) {
                for (double i = 0; i < 5400; i++) {

                    double expT = 1 - Math.exp(-lambdaT);
                    double p1 = rnd.nextDouble();
                    if (p1 < expT) {
                        double p2 = rnd.nextDouble();
                        for (int n = 0; ; n++) {
                            double expN = Math.exp(-lambdaN * n);
                            if ((p2 > expN)) {
                                insertNAtSecond.put(i, n);
                                break;
                            }
                        }
                    }
                }
            } else {
                double nthSecond = (3600 / flow);
                for (double i = 0; i < 5400; i += nthSecond) {
                    insertNAtSecond.put(i, 1);
                }
            }

            for (Map.Entry<Double, Integer> entry : insertNAtSecond.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    // create a person
                    Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + entry.getKey() + "(" + i + ")"));
                    population.addPerson(person);

                    // create a plan for the person that contains all this
                    // information
                    Plan plan = population.getFactory().createPlan();
                    person.addPlan(plan);

                    // create a start activity at the from link
                    Activity startAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(fromLinkId));
                    // distribute agents uniformly during one hour.
                    startAct.setEndTime(entry.getKey());
                    plan.addActivity(startAct);

                    // create a dummy leg
                    plan.addLeg(population.getFactory().createLeg(TransportMode.car));

                    // create a drain activity at the to link
                    Activity drainAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(toLinkId));
                    plan.addActivity(drainAct);
                }
            }
        }
    }

    private static void createSignals(Scenario scenario, LaemmerConfig laemmerConfig, double flowNS, double flowWE, boolean useLanes, boolean liveArrivalRates, boolean grouped) {

        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
        SignalSystemsDataFactory sysFac = signalSystems.getFactory();
        SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
        SignalControlData signalControl = signalsData.getSignalControlData();
        SignalControlDataFactory conFac = signalControl.getFactory();

        // create signal system
        Id<SignalSystem> signalSystemId = Id.create("SignalSystem1", SignalSystem.class);
        SignalSystemData signalSystem1 = sysFac.createSignalSystemData(signalSystemId);
        signalSystems.addSignalSystemData(signalSystem1);


        Lanes lanes = null;

        if (useLanes) {
            lanes = scenario.getLanes();
        }

        // create a signal for every inLink
        for (Link inLink : scenario.getNetwork().getNodes().get(Id.createNodeId(3)).getInLinks().values()) {
            SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLink.getId(), Signal.class));
            if (lanes != null) {
                for (Lane lane : lanes.getLanesToLinkAssignments().get(inLink.getId()).getLanes().values()) {
                    if (lane.getToLinkIds() != null && !lane.getToLinkIds().isEmpty()) {
                        signal.addLaneId(lane.getId());
                    }
                }
            }
            signal.setLinkId(inLink.getId());
            signalSystem1.addSignalData(signal);
        }

        // group signals with non conflicting streams
        Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
        SignalGroupData signalGroup1 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId1);
        Id<Signal> id2_3 = Id.create("Signal2_3", Signal.class);
        signalGroup1.addSignalId(id2_3);


        Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
        SignalGroupData signalGroup2 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId2);
        Id<Signal> id7_3 = Id.create("Signal7_3", Signal.class);
        signalGroup2.addSignalId(id7_3);

        Id<SignalGroup> signalGroupId3 = Id.create("SignalGroup3", SignalGroup.class);
        SignalGroupData signalGroup3 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId3);
        if(grouped) {
            Id<Signal> id4_3 = Id.create("Signal4_3", Signal.class);
            signalGroup1.addSignalId(id4_3);
        } else{

            Id<Signal> id4_3 = Id.create("Signal4_3", Signal.class);
            signalGroup3.addSignalId(id4_3);
        }
        Id<SignalGroup> signalGroupId4 = Id.create("SignalGroup4", SignalGroup.class);
        SignalGroupData signalGroup4 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId4);

        if(grouped) {
            Id<Signal> id8_3 = Id.create("Signal8_3", Signal.class);
            signalGroup2.addSignalId(id8_3);
        }
        else {
            Id<Signal> id8_3 = Id.create("Signal8_3", Signal.class);
            signalGroup4.addSignalId(id8_3);
        }

        signalGroups.addSignalGroupData(signalGroup1);
        signalGroups.addSignalGroupData(signalGroup2);
        if(!grouped) {
            signalGroups.addSignalGroupData(signalGroup3);
            signalGroups.addSignalGroupData(signalGroup4);
        }

        if (!liveArrivalRates) {
            if (useLanes) {
                laemmerConfig.addArrivalRateForLane(Id.createLinkId("2_3"), Id.create("2_3.l", Lane.class), (flowWE / 3600) / 2);
                laemmerConfig.addArrivalRateForLane(Id.createLinkId("2_3"), Id.create("2_3.r", Lane.class), (flowWE / 3600) / 2);

                laemmerConfig.addArrivalRateForLane(Id.createLinkId("7_3"), Id.create("7_3.ol", Lane.class), (flowNS / 3600));

                laemmerConfig.addArrivalRateForLane(Id.createLinkId("4_3"), Id.create("4_3.l", Lane.class), (flowWE / 3600) / 2);
                laemmerConfig.addArrivalRateForLane(Id.createLinkId("4_3"), Id.create("4_3.r", Lane.class), (flowWE / 3600) / 2);

                laemmerConfig.addArrivalRateForLane(Id.createLinkId("8_3"), Id.create("8_3.ol", Lane.class), (flowNS / 3600));
            } else {
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("2_3"), flowWE / 3600);
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("7_3"), flowNS / 3600);
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("4_3"), flowWE / 3600);
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("8_3"), flowNS / 3600);
            }
        }

        // create the signal control
        SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
        signalSystemControl.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
        signalControl.addSignalSystemControllerData(signalSystemControl);
    }
}
