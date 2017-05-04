package signals.laemmer.run;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.*;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.*;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import signals.laemmer.model.LaemmerSignalController;
import signals.laemmer.model.LaemmerSignalsModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by nkuehnel on 13.03.2017.
 */
public class LaemmerExample {

    private static final Logger log = Logger.getLogger(LaemmerMain.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        log.info("Running Laemmer main method...");

        final Config config = defineConfig();

        // simulate traffic dynamics with holes (default would be without)
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
        config.qsim().setNodeOffset(5.);

        // add the OTFVis config group
        OTFVisConfigGroup otfvisConfig =
                ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
        // make links visible beyond screen edge
        otfvisConfig.setScaleQuadTreeRect(true);
        otfvisConfig.setColoringScheme(OTFVisConfigGroup.ColoringScheme.byId);
        otfvisConfig.setAgentSize(240);

        final Scenario scenario = defineScenario(config);
        Controler controler = new Controler(scenario);

        // add the general signals module
        controler.addOverridingModule(new LaemmerSignalsModule());
        controler.addOverridingModule( new OTFVisWithSignalsLiveModule() );

        try {
            LaemmerSignalController.log.addAppender(new FileAppender(new SimpleLayout(), "logs/main.txt"));

            LaemmerSignalController.signalLog.addAppender(new FileAppender(new SimpleLayout(), "logs/driveways.txt"));


        } catch (IOException e) {
            e.printStackTrace();
        }

        controler.run();
    }

    private static Config defineConfig() {
        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory("output/laemmerexample/");

        config.controler().setLastIteration(1);
        config.travelTimeCalculator().setMaxTime(3600 * 5);
        config.qsim().setStartTime(0);
        config.qsim().setEndTime(3600 * 5);

        SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        signalConfigGroup.setUseSignalSystems(true);

        // here is how to also use intergreen and amber times:
//        signalConfigGroup.setUseIntergreenTimes(true);

//		// set a suitable action for the case that intergreens are violated
//        signalConfigGroup.setActionOnIntergreenViolation(SignalSystemsConfigGroup.ActionOnIntergreenViolation.EXCEPTION);
//        signalConfigGroup.setUseAmbertimes(true);

        config.transit().setUseTransit(true);

        PlanCalcScoreConfigGroup.ActivityParams dummyAct = new PlanCalcScoreConfigGroup.ActivityParams("dummy");
        dummyAct.setTypicalDuration(12 * 3600);
        config.planCalcScore().addActivityParams(dummyAct);

        StrategyConfigGroup.StrategySettings strat = new StrategyConfigGroup.StrategySettings();
        strat.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected.toString());
        strat.setWeight(0.0);
        strat.setDisableAfter(config.controler().getLastIteration());
        config.strategy().addStrategySettings(strat);


        config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists ) ;
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());
        config.controler().setWritePlansInterval(config.controler().getLastIteration());
        config.vspExperimental().setWritingOutputEvents(true);
        config.planCalcScore().setWriteExperiencedPlans(true);
        config.controler().setCreateGraphs(true);

        return config;
    }

    private static Scenario defineScenario(Config config) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        // add missing scenario elements
       SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

        createNetwork(scenario);
        createPopulation(scenario);
        createTransit(scenario);
        createSignals(scenario);
        return scenario;
    }

    /** creates a network like this:
     *
     * 					 6                                    14
     * 					 ^                                     ^
     * 					 |                                     |
     * 					 v                                     v
     * 					 7                                    15
     * 					 ^                                     ^
     * 		   (H1)	     |                (H2)                 |       (H3)
     * 					 v                                     v
     * 1 <----> 2 <----> 3 <----> 4 <----> 5 <----> 10 <----> 11 <----> 12 <----> 13
     * 					 ^                                     ^
     * 					 |                                     |
     * 					 v                                     v
     * 					 8                                    16
     * 					 ^                                     ^
     * 					 |                                     |
     * 					 v                                     v
     * 					 9                                    17
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

        //right crossing
        net.addNode(fac.createNode(Id.createNodeId(10), new Coord(3000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(11), new Coord(4000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(12), new Coord(5000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(13), new Coord(6000, 0)));
        net.addNode(fac.createNode(Id.createNodeId(14), new Coord(4000, 2000)));
        net.addNode(fac.createNode(Id.createNodeId(15), new Coord(4000, 1000)));
        net.addNode(fac.createNode(Id.createNodeId(16), new Coord(4000, -1000)));
        net.addNode(fac.createNode(Id.createNodeId(17), new Coord(4000, -2000)));

        String[] links = {"1_2", "2_1", "2_3", "3_2", "3_4", "4_3", "4_5", "5_4",
                "6_7", "7_6", "7_3", "3_7", "3_8", "8_3", "8_9", "9_8", "5_10", "10_5",
                "10_11", "11_10", "11_12", "12_11", "12_13", "13_12", "14_15", "15_14",
                "15_11", "11_15", "11_16", "16_11", "16_17", "17_16"};

        int i = 0;
        for (String linkId : links){
            String fromNodeId = linkId.split("_")[0];
            String toNodeId = linkId.split("_")[1];
            Link link = fac.createLink(Id.createLinkId(linkId),
                    net.getNodes().get(Id.createNodeId(fromNodeId)),
                    net.getNodes().get(Id.createNodeId(toNodeId)));
            link.setCapacity(1800);
            link.setLength(1000);
            link.setFreespeed(10);
            Set<String> modes = new HashSet<>();
            modes.add("car");
            modes.add("bus");

            link.setAllowedModes(modes);
            net.addLink(link);
        }
    }

    private static void createPopulation(Scenario scenario) {
        Population population = scenario.getPopulation();

        String[] odRelations = {"1_2-12_13", "6_7-8_9", "14_15-16_17"};

        for (String od : odRelations) {
            String fromLinkId = od.split("-")[0];
            String toLinkId = od.split("-")[1];

            for (int i = 1; i < 800; i+=6) {
                if(i%100==0) {
                    i+= 100;
                }
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

    private static void createSignals(Scenario scenario) {
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
        SignalSystemsDataFactory sysFac = signalSystems.getFactory();
        SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
        SignalControlData signalControl = signalsData.getSignalControlData();
        SignalControlDataFactory conFac = signalControl.getFactory();
//        IntergreenTimesData ig = signalsData.getIntergreenTimesData();
//        IntergreenTimesDataFactory igdf = ig.getFactory();
//        signalsData.getAmberTimesData().setDefaultRedAmber(3);

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
        signalGroup1.addSignalId(Id.create("Signal2_3", Signal.class));
        signalGroups.addSignalGroupData(signalGroup1);

        Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
        SignalGroupData signalGroup2 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId2);
        signalGroup2.addSignalId(Id.create("Signal7_3", Signal.class));
        signalGroups.addSignalGroupData(signalGroup2);

        Id<SignalGroup> signalGroupId3 = Id.create("SignalGroup3", SignalGroup.class);
        SignalGroupData signalGroup3 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId3);
        signalGroup3.addSignalId(Id.create("Signal4_3", Signal.class));
        signalGroups.addSignalGroupData(signalGroup3);

        Id<SignalGroup> signalGroupId4 = Id.create("SignalGroup4", SignalGroup.class);
        SignalGroupData signalGroup4 = signalGroups.getFactory()
                .createSignalGroupData(signalSystemId, signalGroupId4);
        signalGroup4.addSignalId(Id.create("Signal8_3", Signal.class));
        signalGroups.addSignalGroupData(signalGroup4);

        // create the signal control
        SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
        signalSystemControl.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
        signalControl.addSignalSystemControllerData(signalSystemControl);

        //Intergreens
        // Create a data object for signal system with id 1
//        IntergreensForSignalSystemData ig1 = igdf.createIntergreensForSignalSystem(Id.create("SignalSystem1", SignalSystem.class));
        // Request at least x seconds red between the end of green of signal group y
        // and the beginning of green of signal group z
//        ig1.setIntergreenTime(6, Id.create("SignalGroup1", SignalGroup.class), Id.create("SignalGroup2", SignalGroup.class));
//        ig1.setIntergreenTime(0, Id.create("SignalGroup1", SignalGroup.class), Id.create("SignalGroup3", SignalGroup.class));
//        ig1.setIntergreenTime(6, Id.create("SignalGroup1", SignalGroup.class), Id.create("SignalGroup4", SignalGroup.class));
//
//        ig1.setIntergreenTime(6, Id.create("SignalGroup2", SignalGroup.class), Id.create("SignalGroup1", SignalGroup.class));
//        ig1.setIntergreenTime(6, Id.create("SignalGroup2", SignalGroup.class), Id.create("SignalGroup3", SignalGroup.class));
//        ig1.setIntergreenTime(0, Id.create("SignalGroup2", SignalGroup.class), Id.create("SignalGroup4", SignalGroup.class));
//
//        ig1.setIntergreenTime(0, Id.create("SignalGroup3", SignalGroup.class), Id.create("SignalGroup1", SignalGroup.class));
//        ig1.setIntergreenTime(6, Id.create("SignalGroup3", SignalGroup.class), Id.create("SignalGroup2", SignalGroup.class));
//        ig1.setIntergreenTime(6, Id.create("SignalGroup3", SignalGroup.class), Id.create("SignalGroup4", SignalGroup.class));
//
//        ig1.setIntergreenTime(6, Id.create("SignalGroup4", SignalGroup.class), Id.create("SignalGroup1", SignalGroup.class));
//        ig1.setIntergreenTime(0, Id.create("SignalGroup4", SignalGroup.class), Id.create("SignalGroup2", SignalGroup.class));
//        ig1.setIntergreenTime(6, Id.create("SignalGroup4", SignalGroup.class), Id.create("SignalGroup3", SignalGroup.class));
//
//        // add the data object to the container
//        ig.addIntergreensForSignalSystem(ig1);


        //------------------------------------------------------------------------------------------------------------


//        // create signal system
//        Id<SignalSystem> signalSystemId2 = Id.create("SignalSystem2", SignalSystem.class);
//        SignalSystemData signalSystem2 = sysFac.createSignalSystemData(signalSystemId2);
//        signalSystems.addSignalSystemData(signalSystem2);
//
//        // create a signal for every inLink
//        for (Id<Link> inLinkId : scenario.getNetwork().getNodes().get(Id.createNodeId(11)).getInLinks().keySet()) {
//            SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLinkId, Signal.class));
//            signalSystem2.addSignalData(signal);
//            signal.setLinkId(inLinkId);
//        }
//
//        // group signals with non conflicting streams
//        Id<SignalGroup> signalGroupId5 = Id.create("SignalGroup5", SignalGroup.class);
//        SignalGroupData signalGroup5 = signalGroups.getFactory()
//                .createSignalGroupData(signalSystemId2, signalGroupId5);
//        signalGroup5.addSignalId(Id.create("Signal10_11", Signal.class));
//        signalGroups.addSignalGroupData(signalGroup5);
//
//        Id<SignalGroup> signalGroupId6 = Id.create("SignalGroup6", SignalGroup.class);
//        SignalGroupData signalGroup6 = signalGroups.getFactory()
//                .createSignalGroupData(signalSystemId2, signalGroupId6);
//        signalGroup6.addSignalId(Id.create("Signal15_11", Signal.class));
//        signalGroups.addSignalGroupData(signalGroup6);
//
//        Id<SignalGroup> signalGroupId7 = Id.create("SignalGroup7", SignalGroup.class);
//        SignalGroupData signalGroup7 = signalGroups.getFactory()
//                .createSignalGroupData(signalSystemId2, signalGroupId7);
//        signalGroup7.addSignalId(Id.create("Signal12_11", Signal.class));
//        signalGroups.addSignalGroupData(signalGroup7);
//
//        Id<SignalGroup> signalGroupId8 = Id.create("SignalGroup8", SignalGroup.class);
//        SignalGroupData signalGroup8 = signalGroups.getFactory()
//                .createSignalGroupData(signalSystemId2, signalGroupId8);
//        signalGroup8.addSignalId(Id.create("Signal16_11", Signal.class));
//        signalGroups.addSignalGroupData(signalGroup8);
//
////      create the signal control
//        SignalSystemControllerData signalSystemControl2 = conFac.createSignalSystemControllerData(signalSystemId2);
//        signalSystemControl2.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
//        signalControl.addSignalSystemControllerData(signalSystemControl2);

//        //Intergreens
//        // Create a data object for signal system with id 2
//        IntergreensForSignalSystemData ig2 = igdf.createIntergreensForSignalSystem(Id.create("SignalSystem2", SignalSystem.class));
//        // Request at least x seconds red between the end of green of signal group y
//        // and the beginning of green of signal group z
//        ig2.setIntergreenTime(6, Id.create("SignalGroup5", SignalGroup.class), Id.create("SignalGroup6", SignalGroup.class));
//        ig2.setIntergreenTime(0, Id.create("SignalGroup5", SignalGroup.class), Id.create("SignalGroup7", SignalGroup.class));
//        ig2.setIntergreenTime(6, Id.create("SignalGroup5", SignalGroup.class), Id.create("SignalGroup8", SignalGroup.class));
//
//        ig2.setIntergreenTime(6, Id.create("SignalGroup6", SignalGroup.class), Id.create("SignalGroup5", SignalGroup.class));
//        ig2.setIntergreenTime(6, Id.create("SignalGroup6", SignalGroup.class), Id.create("SignalGroup7", SignalGroup.class));
//        ig2.setIntergreenTime(0, Id.create("SignalGroup6", SignalGroup.class), Id.create("SignalGroup8", SignalGroup.class));
//
//        ig2.setIntergreenTime(0, Id.create("SignalGroup7", SignalGroup.class), Id.create("SignalGroup5", SignalGroup.class));
//        ig2.setIntergreenTime(6, Id.create("SignalGroup7", SignalGroup.class), Id.create("SignalGroup6", SignalGroup.class));
//        ig2.setIntergreenTime(6, Id.create("SignalGroup7", SignalGroup.class), Id.create("SignalGroup8", SignalGroup.class));
//
//        ig2.setIntergreenTime(6, Id.create("SignalGroup8", SignalGroup.class), Id.create("SignalGroup5", SignalGroup.class));
//        ig2.setIntergreenTime(0, Id.create("SignalGroup8", SignalGroup.class), Id.create("SignalGroup6", SignalGroup.class));
//        ig2.setIntergreenTime(6, Id.create("SignalGroup8", SignalGroup.class), Id.create("SignalGroup7", SignalGroup.class));
//
//        // add the data object to the container
//        ig.addIntergreensForSignalSystem(ig2);
    }

    private static void createTransit(Scenario scenario) {
        TransitSchedule ts = scenario.getTransitSchedule();
        TransitScheduleFactory tsFac = ts.getFactory();

        VehiclesFactory vehFac = scenario.getTransitVehicles().getFactory();

        VehicleType busType = vehFac.createVehicleType(Id.create("bus", VehicleType.class));
        VehicleCapacity vehCap = vehFac.createVehicleCapacity();
        vehCap.setSeats(40);
        vehCap.setStandingRoom(80);
        busType.setCapacity(vehCap);
        scenario.getTransitVehicles().addVehicleType(busType);
        scenario.getTransitVehicles().addVehicle(vehFac.createVehicle(Id.createVehicleId(1), busType));

        TransitStopFacility stop1 =tsFac.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord(-1000, 500), true);
        TransitStopFacility stop2 =tsFac.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord(2000, 500), true);
        TransitStopFacility stop3 = tsFac.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord(5000, 500), true);
        stop1.setLinkId(Id.createLinkId("3_2"));
        stop2.setLinkId(Id.createLinkId("10_5"));
        stop3.setLinkId(Id.createLinkId("13_12"));
        ts.addStopFacility(stop1);
        ts.addStopFacility(stop2);
        ts.addStopFacility(stop3);

        TransitLine line1 = tsFac.createTransitLine(Id.create("Line1", TransitLine.class));
        List<Id<Link>> links = new ArrayList<>();
        links.add(Id.createLinkId("12_11"));
        links.add(Id.createLinkId("11_10"));
        links.add(Id.createLinkId("10_5"));
        links.add(Id.createLinkId("5_4"));
        links.add(Id.createLinkId("4_3"));
        links.add(Id.createLinkId("3_2"));


        NetworkRoute networkRoute = new LinkNetworkRouteImpl(Id.createLinkId("13_12"), links, Id.createLinkId("2_1"));
        List<TransitRouteStop> stops = new ArrayList<>();

        stops.add(tsFac.createTransitRouteStop(stop3, 60, 60));
        stops.add(tsFac.createTransitRouteStop(stop2, 60, 60));
        stops.add(tsFac.createTransitRouteStop(stop1, 60, 60));

        TransitRoute route1 = tsFac.createTransitRoute(Id.create("route1", TransitRoute.class), networkRoute, stops, "bus");
        line1.addRoute(route1);
        for(int i = 0; i< 1200; i += 300) {
            Departure dep = tsFac.createDeparture(Id.create(i+1, Departure.class), i);
            dep.setVehicleId(Id.createVehicleId(1));
            route1.addDeparture(dep);
        }
        ts.addTransitLine(line1);
    }
}
