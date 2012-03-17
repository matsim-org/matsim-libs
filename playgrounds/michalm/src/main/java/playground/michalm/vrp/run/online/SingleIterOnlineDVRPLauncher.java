package playground.michalm.vrp.run.online;

import java.io.*;
import java.util.*;

import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityCalculator;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.*;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.population.algorithms.*;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.*;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.gui.OTFQueryControl;

import pl.poznan.put.util.jfreechart.*;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.vrp.dynamic.chart.*;
import pl.poznan.put.vrp.dynamic.data.VRPData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.optimizer.listener.ChartFileOptimizerListener;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizer.AlgorithmType;
import playground.michalm.util.gis.Schedules2GIS;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.file.DepotReader;
import playground.michalm.vrp.data.network.router.TravelTimeCalculators;
import playground.michalm.vrp.data.network.shortestpath.sparse.SparseShortestPathFinder;
import playground.michalm.vrp.otfvis.VRPOTFClientLive;
import playground.michalm.vrp.taxi.*;
import playground.michalm.vrp.taxi.taxicab.TaxiAgentSource;


public class SingleIterOnlineDVRPLauncher
{
    private String dirName;
    private String netFileName;
    private String plansFileName;
    private String depotsFileName;
    private String reqIdToVehIdFileName;
    private boolean vrpOutFiles;
    private String vrpOutDirName;

    private boolean travelTimesFromEvents;
    private String eventsFileName;

    private Scenario scenario;
    private MATSimVRPData data;

    private AlgorithmType algorithmType;
    private TaxiOptimizerFactory optimizerFactory;

    private PersonalizableTravelTime ttimeCalc;
    private PersonalizableTravelDisutility tcostCalc;

    private boolean otfVis;
    public static OTFQueryControl queryControl;


    private void processArgs()
    {
        dirName = "D:\\PP-rad\\taxi\\mielec-nowe-OD\\";
        netFileName = dirName + "network.xml";
        plansFileName = dirName + "plans.xml";
        depotsFileName = dirName + "depots.xml";
        reqIdToVehIdFileName = dirName + "reqIdToVehId";

        travelTimesFromEvents = true;
        eventsFileName = "d:\\PP-rad\\taxi\\orig-mielec-nowe-OD\\output\\std\\ITERS\\it.10\\10.events.xml.gz";

        algorithmType = AlgorithmType.PRE_ASSIGNMENT;

        otfVis = !true;

        vrpOutFiles = !true;
        vrpOutDirName = dirName + "\\vrp_output";
        new File(vrpOutDirName).mkdir();
    }


    private void prepareMATSimData()
    {
        Config config = ConfigUtils.createConfig();
        scenario = ScenarioUtils.createScenario(config);

        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);

        ttimeCalc = travelTimesFromEvents ? TravelTimeCalculators.createTravelTimeFromEvents(
                eventsFileName, scenario) : new FreeSpeedTravelTimeCalculator();
        tcostCalc = new OnlyTimeDependentTravelDisutilityCalculator(ttimeCalc);

        DijkstraFactory leastCostPathCalculatorFactory = new DijkstraFactory();

        ModeRouteFactory routeFactory = ((PopulationFactoryImpl)scenario.getPopulation()
                .getFactory()).getModeRouteFactory();

        final PlansCalcRoute routingAlgorithm = new PlansCalcRoute(config.plansCalcRoute(),
                scenario.getNetwork(), tcostCalc, ttimeCalc, leastCostPathCalculatorFactory,
                routeFactory);

        routingAlgorithm.addLegHandler("taxi", new NetworkLegRouter(scenario.getNetwork(),
                routingAlgorithm.getLeastCostPathCalculator(), routingAlgorithm.getRouteFactory()));

        ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 1,
                new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
                    @Override
                    public AbstractPersonAlgorithm getPersonAlgorithm()
                    {
                        return new PersonPrepareForSim(routingAlgorithm, (ScenarioImpl)scenario);
                    }
                });
    }


    private void initMATSimVRPData()
        throws IOException
    {
        data = MATSimVRPDataCreator.create(scenario);
        new DepotReader(scenario, data).readFile(depotsFileName);

        LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), tcostCalc, ttimeCalc);

        SparseShortestPathFinder sspf = new SparseShortestPathFinder(data);
        sspf.findShortestPaths(ttimeCalc, tcostCalc, router);
        sspf.upadateVRPArcTimesAndCosts();
    }


    private void initOptimizerFactory()
        throws IOException
    {
        switch (algorithmType) {
            case NO_RE_ASSIGNMENT:
                optimizerFactory = TaxiOptimizerWithoutReassignment.FACTORY;
                break;

            case RE_ASSIGNMENT:
                optimizerFactory = TaxiOptimizerWithReassignment.FACTORY;
                break;

            case PRE_ASSIGNMENT:
                File reqIdToVehIdFile = new File(reqIdToVehIdFileName);
                Scanner scanner = new Scanner(reqIdToVehIdFile);

                List<Vehicle> vehicles = data.getVrpData().getVehicles();
                Vehicle[] reqIdToVehMapping = new Vehicle[scanner.nextInt()];
                
                for (int i = 0; i < reqIdToVehMapping.length; i++) {
                    reqIdToVehMapping[i] = vehicles.get(scanner.nextInt());
                }

                optimizerFactory = TaxiOptimizerWithPreassignment.createFactory(reqIdToVehMapping);
                break;

            default:
                throw new IllegalStateException();
        }
    }


    private void runSim()
    {
        QSimConfigGroup qSimConfig = new QSimConfigGroup();
        qSimConfig.setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE);
        scenario.getConfig().addQSimConfigGroup(qSimConfig);

        EventsManager events = EventsUtils.createEventsManager();

        QSim sim = new QSim(scenario, events, new DefaultQSimEngineFactory());

        TaxiSimEngine taxiSimEngine = new TaxiSimEngine(sim, data.getVrpData(), optimizerFactory);
        sim.addMobsimEngine(taxiSimEngine);
        sim.addAgentSource(new PopulationAgentSource(scenario.getPopulation(),
                new DefaultAgentFactory(sim), sim));
        sim.addAgentSource(new TaxiAgentSource(data, taxiSimEngine));
        sim.addDepartureHandler(new TaxiModeDepartureHandler(taxiSimEngine, data));

        if (vrpOutFiles) {
            taxiSimEngine.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                public JFreeChart createChart(VRPData data)
                {
                    return RouteChartUtils.chartRoutesByStatus(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\routes_", 800, 800));

            taxiSimEngine.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                public JFreeChart createChart(VRPData data)
                {
                    return ScheduleChartUtils.chartSchedule(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\schedules_", 1200, 800));
        }

        if (otfVis) { // OFTVis visualization
            OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(),
                    scenario, sim.getEventsManager(), sim);
            VRPOTFClientLive.run(scenario.getConfig(), server);
        }

        sim.run();
    }


    private void generateVrpOutput()
    {
        System.out.println(new TaxiEvaluator().evaluateVRP(data.getVrpData()).toString());

        if (vrpOutFiles) {
            new Schedules2GIS(data.getVrpData().getVehicles(), data, vrpOutDirName + "\\route_")
                    .write();
        }

        ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));
    }


    private void go()
        throws IOException
    {
        processArgs();
        prepareMATSimData();
        initMATSimVRPData();
        initOptimizerFactory();
        runSim();
        generateVrpOutput();
    }


    public static void main(String... args)
        throws IOException
    {
        new SingleIterOnlineDVRPLauncher().go();
    }
}
