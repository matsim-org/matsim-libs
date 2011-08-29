package playground.michalm.vrp.online;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.ptproject.qsim.QSim;

import pl.poznan.put.vrp.cvrp.data.AlgorithmParams;
import pl.poznan.put.vrp.dynamic.customer.CustomerAction;
import pl.poznan.put.vrp.dynamic.data.VRPData;
import pl.poznan.put.vrp.dynamic.data.file.LacknerReader;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import playground.michalm.vrp.data.MATSimVRPData;
import playground.michalm.vrp.data.network.MATSimVertex;
import playground.michalm.vrp.data.network.MATSimVertexImpl;
import playground.michalm.vrp.data.network.ShortestPathsFinder;
import playground.michalm.vrp.sim.VRPAgentFactory;
import playground.michalm.vrp.sim.VRPDriverPerson;
import playground.michalm.vrp.sim.VRPSimEngine;


public class SingleIterOnlineDVRPLauncher
{
    // means: all requests are known a priori (in advance/static)
    private static boolean STATIC_MODE = false;// default: false

    // schedules/routes PNG files, routes SHP files
    private static boolean VRP_OUT_FILES = true;// default: true


    public static void main(String... args)
        throws IOException
    {
        String dirName;
        String cfgFileName;
        String vrpDirName;
        String vrpStaticFileName;
        String vrpArcTimesFileName;
        String vrpArcCostsFileName;
        String vrpArcPathsFileName;
        String vrpDynamicFileName;
        String algParamsFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            STATIC_MODE = false;
            VRP_OUT_FILES = true;

            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            cfgFileName = dirName + "config-verB.xml";
            vrpDirName = dirName + "dvrp\\";
            vrpStaticFileName = "A101.txt";
            vrpDynamicFileName = "A101_scen.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "A102.txt";
            // vrpDynamicFileName = "A102_scen.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "C101.txt";
            // vrpDynamicFileName = "C101_scen.txt";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            // cfgFileName = dirName + "config-verB.xml";
            // vrpDirName = dirName + "dvrp\\";
            // vrpStaticFileName = "C102.txt";
            // vrpDynamicFileName = "C102_scen.txt";

            vrpArcTimesFileName = vrpDirName + "arc_times.txt.gz";
            vrpArcCostsFileName = vrpDirName + "arc_costs.txt.gz";
            vrpArcPathsFileName = vrpDirName + "arc_paths.txt.gz";
            algParamsFileName = "algorithm.txt";
        }
        else if (args.length == 9) {
            dirName = args[0];
            cfgFileName = dirName + args[1];
            vrpDirName = dirName + args[2];
            vrpStaticFileName = args[3];
            vrpArcTimesFileName = vrpDirName + args[4];
            vrpArcCostsFileName = vrpDirName + args[5];
            vrpArcPathsFileName = vrpDirName + args[6];
            vrpDynamicFileName = args[7];
            algParamsFileName = args[8];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        // read MATSim data
        Config config = ConfigUtils.loadConfig(cfgFileName);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        preparePlansForPersons(scenario);
        
        // init DVRP data
        AlgorithmParams algParams = new AlgorithmParams(new File(vrpDirName + algParamsFileName));

        VRPData vrpData = LacknerReader.parseStaticFile(vrpDirName, vrpStaticFileName,
                new MATSimVertexImpl.Builder(scenario));

        Queue<CustomerAction> caQueue = null;

        if (STATIC_MODE) {
            caQueue = new PriorityQueue<CustomerAction>(1, CustomerAction.TIME_COMPARATOR);
        }
        else {
            caQueue = LacknerReader.parseDynamicFile(vrpDirName, vrpDynamicFileName, vrpData);
        }

        final MATSimVRPData data = new MATSimVRPData(vrpData, scenario);

        //create VRPDriverPersons and add them to the population
        createDriverPersons(scenario, vrpData);
        
        //read ShortestPaths from file
        ShortestPathsFinder spf = new ShortestPathsFinder(data);
        spf.readShortestPaths(vrpArcTimesFileName, vrpArcCostsFileName, vrpArcPathsFileName);
        spf.upadateVRPArcTimesAndCosts();

        // to have TravelTimeCalculatorWithBuffer instead of TravelTimeCalculator use:
        // controler.setTravelTimeCalculatorFactory(new TravelTimeCalculatorWithBufferFactory());

        final String vrpOutDirName = vrpDirName + "\\output";
        new File(vrpOutDirName).mkdir();

        EventsManager events = EventsUtils.createEventsManager();
        QSim sim = createMobsim(scenario, events, data, algParams, vrpOutDirName);

        //OFTVis visualization
        // ControlerIO controlerIO = new ControlerIO(scenario.getConfig().controler()
        // .getOutputDirectory());
        // OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(sim);
        // sim.addFeature(queueSimulationFeature);
        // queueSimulationFeature.setVisualizeTeleportedAgents(scenario.getConfig().otfVis()
        // .isShowTeleportedAgents());
        // sim.setControlerIO(controlerIO);
        // sim.setIterationNumber(scenario.getConfig().controler().getLastIteration());

        sim.run();

//        if (VRP_OUT_FILES) {
//            new Routes2QGIS(data.getVrpData().routes, data, vrpOutDirName + "\\route_").write();
//        }

//        ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
//        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));
    }


    private static QSim createMobsim(Scenario sc, EventsManager eventsManager, MATSimVRPData data,
            AlgorithmParams algParams, String vrpOutDirName)
    {
        QSim sim = new QSim(sc, eventsManager);
        sim.setAgentFactory(new VRPAgentFactory(sim, data));
        
        VRPSimEngine vrpSimEngine = new VRPSimEngine(sim, data, algParams);
        data.setVrpSimEngine(vrpSimEngine);
        sim.addMobsimEngine(vrpSimEngine);
        
        // The above is slighly confusing: 
        // (1) The VRPSimEngine adds "VRP" persons to the population (in onPrepareSim) ...
        // (2) ... which are then converted into VRP agents by the agent factory.
        // One wonders if they really need to be added to the population, and if so, if this is the best way to do this.
        // kai, jun'11

//        if (VRP_OUT_FILES) {
//            vrpSimEngine.addListener(new ChartFileSimulationListener(new ChartCreator() {
//                public JFreeChart createChart(VRPData data)
//                {
//                    return RouteChartUtils.chartRoutesByStatus(data);
//                }
//            }, OutputType.PNG, vrpOutDirName + "\\routes_", 800, 800));
//
//            vrpSimEngine.addListener(new ChartFileSimulationListener(new ChartCreator() {
//                public JFreeChart createChart(VRPData data)
//                {
//                    return ScheduleChartUtils.chartSchedule(data);
//                }
//            }, OutputType.PNG, vrpOutDirName + "\\schedules_", 1200, 800));
//        }

        return sim;
    }


    private static void preparePlansForPersons(Scenario scenario)
    {
        Config config = scenario.getConfig();
        final NetworkImpl network = (NetworkImpl)scenario.getNetwork();

        DijkstraFactory leastCostPathCalculatorFactory = new DijkstraFactory();
        TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
        TravelCostCalculatorFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
        TravelTimeCalculator travelTimeCalculator = travelTimeCalculatorFactory
                .createTravelTimeCalculator(scenario.getNetwork(), config.travelTimeCalculator());

        final PlansCalcRoute routingAlgorithm = new PlansCalcRoute(config.plansCalcRoute(),
                scenario.getNetwork(), travelCostCalculatorFactory.createTravelCostCalculator(
                        travelTimeCalculator, config.planCalcScore()), travelTimeCalculator,
                leastCostPathCalculatorFactory);

        ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 1,
                new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
                    public AbstractPersonAlgorithm getPersonAlgorithm()
                    {
                        return new PersonPrepareForSim(routingAlgorithm, network);
                    }
                });
    }
    
    
    private static void createDriverPersons(Scenario scenario, VRPData vrpData)
    {
        Population population = scenario.getPopulation();
        Vehicle[] vrpVehicles = vrpData.vehicles;

        for (Vehicle vrpVeh : vrpVehicles) {
            Id personId = scenario.createId("vrpDriver_" + vrpVeh.id);
            VRPDriverPerson vrpDriver = new VRPDriverPerson(personId, vrpVeh);

            Plan dummyPlan = new PlanImpl(vrpDriver);
            MATSimVertex vertex = (MATSimVertex)vrpVeh.depot.vertex;
            Activity dummyAct = new ActivityImpl("w", vertex.getCoord(), vertex.getLink().getId());
            dummyPlan.addActivity(dummyAct);
            vrpDriver.addPlan(dummyPlan);

            population.addPerson(vrpDriver);
        }
    }

}
