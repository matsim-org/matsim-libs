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
import org.matsim.contrib.signals.controler.SignalsModule;
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
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import signals.CombinedSignalsModule;
import signals.laemmer.model.LaemmerConfig;
import signals.laemmer.model.LaemmerSignalController;
import signals.laemmer.model.LaemmerSignalsModule;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by nkuehnel on 03.04.2017.
 */
public class LaemmerBasicExample {
    private static final Logger log = Logger.getLogger(LaemmerMain.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        log.info("Running Laemmer main method...");

//        for(int i = 0; i<=1200; i+=60){
//            run(i);
//        }
        run(180, 900);

    }

    private static void run(double flowNS, double flowWE) {
        final Config config = defineConfig(flowWE);

        // simulate traffic dynamics with holes (default would be without)
//        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
//        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
//        config.qsim().setNodeOffset(5.);

////         add the OTFVis config group
//        OTFVisConfigGroup otfvisConfig =
//                ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
////         make links visible beyond screen edge
//        otfvisConfig.setScaleQuadTreeRect(true);
//        otfvisConfig.setColoringScheme(OTFVisConfigGroup.ColoringScheme.byId);
//        otfvisConfig.setAgentSize(240);



        // add the general signals module

        CombinedSignalsModule module = new CombinedSignalsModule();
        //set laemmer params
        LaemmerConfig laemmerConfig = new LaemmerConfig();
        laemmerConfig.setDEFAULZT_INTERGREEN(5);
        laemmerConfig.setDESIRED_PERIOD(120);
        laemmerConfig.setMAX_PERIOD(180);
        module.setLaemmerConfig(laemmerConfig);

        final Scenario scenario = defineScenario(config, flowNS, flowWE, laemmerConfig);
        Controler controler = new Controler(scenario);

        controler.addOverridingModule(module);
//        controler.addOverridingModule(new OTFVisWithSignalsLiveModule());

        LaemmerSignalController.log.setLevel(Level.OFF);
        LaemmerSignalController.signalLog.setLevel(Level.OFF);
//        try {
//            LaemmerSignalController.log.addAppender(new FileAppender(new SimpleLayout(), "logs/main.txt"));
//            LaemmerSignalController.signalLog.addAppender(new FileAppender(new SimpleLayout(), "logs/driveways.txt"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        controler.run();
    }

    private static Config defineConfig(double flowWE) {
        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory("output/laemmerexampleStabilisierungBasic_"+flowWE+"_e_w/");

        config.controler().setLastIteration(0);
        config.travelTimeCalculator().setMaxTime(60 * 120);
        config.qsim().setStartTime(0);
        config.qsim().setEndTime(60 * 120);
        config.qsim().setUsingFastCapacityUpdate(true);

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

    private static Scenario defineScenario(Config config, double flowNS, double flowWE, LaemmerConfig laemmerConfig) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        // add missing scenario elements
        SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

        createNetwork(scenario);
        createPopulation(scenario, flowNS, flowWE);

        createSignals(scenario, laemmerConfig, flowNS, flowWE);
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
     * 1 <----> 2 <----> 3 <----> 4 <----> 5
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

    private static void createPopulation(Scenario scenario, double flowNS, double flowWE) {
        Population population = scenario.getPopulation();

        String[] linksNS = {"6_7-8_9", "9_8-7_6"};
        String[] linksWE = {"5_4-2_1", "1_2-4_5"};

        createPopulationForRelation(flowNS, population, linksNS);
        createPopulationForRelation(flowWE, population, linksWE);
    }

    private static void createPopulationForRelation(double flow, Population population, String[] links) {
        if (flow > 0) {
            for (String od : links) {
                String fromLinkId = od.split("-")[0];
                String toLinkId = od.split("-")[1];

                double nthSecond = (3600 / flow);

                for (double i = 0; i < 5400; i+=nthSecond) {


                    // create a person
                    Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + i));
                    population.addPerson(person);

                    // create a plan for the person that contains all this
                    // information
                    Plan plan = population.getFactory().createPlan();
                    person.addPlan(plan);

                    // create a start activity at the from link
                    Activity startAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(fromLinkId));
                    // distribute agents uniformly during one hour.
                    startAct.setEndTime(i);
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

    private static void createSignals(Scenario scenario, LaemmerConfig laemmerConfig, double flowNS, double flowWE) {

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

        // create a signal for every inLink
        for (Id<Link> inLinkId : scenario.getNetwork().getNodes().get(Id.createNodeId(3)).getInLinks().keySet()) {
            SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLinkId, Signal.class));
            signalSystem1.addSignalData(signal);
            signal.setLinkId(inLinkId);
        }

        // group signals with non conflicting streams
        Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
        SignalGroupData signalGroup1 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId1);
        Id<Signal> id2_3 = Id.create("Signal2_3", Signal.class);
        signalGroup1.addSignalId(id2_3);
        laemmerConfig.addArrivalRateForSignal(id2_3, flowWE);
        signalGroups.addSignalGroupData(signalGroup1);

        Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
        SignalGroupData signalGroup2 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId2);
        Id<Signal> id7_3 = Id.create("Signal7_3", Signal.class);
        signalGroup1.addSignalId(id7_3);
        laemmerConfig.addArrivalRateForSignal(id7_3, flowNS);
        signalGroups.addSignalGroupData(signalGroup2);

        Id<SignalGroup> signalGroupId3 = Id.create("SignalGroup3", SignalGroup.class);
        SignalGroupData signalGroup3 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId3);
        Id<Signal> id4_3 = Id.create("Signal4_3", Signal.class);
        signalGroup1.addSignalId(id4_3);
        laemmerConfig.addArrivalRateForSignal(id4_3, flowWE);
        signalGroups.addSignalGroupData(signalGroup3);

        Id<SignalGroup> signalGroupId4 = Id.create("SignalGroup4", SignalGroup.class);
        SignalGroupData signalGroup4 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId4);
        Id<Signal> id8_3 = Id.create("Signal8_3", Signal.class);
        signalGroup1.addSignalId(id8_3);
        laemmerConfig.addArrivalRateForSignal(id8_3, flowWE);
        signalGroups.addSignalGroupData(signalGroup4);

        // create the signal control
        SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
        signalSystemControl.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
        signalControl.addSignalSystemControllerData(signalSystemControl);
    }
}
