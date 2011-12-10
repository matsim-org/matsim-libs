package playground.michalm.vrp.run.online;

import java.io.*;

import org.jfree.chart.*;
import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.*;
import org.matsim.core.events.*;
import org.matsim.core.network.*;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.*;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.*;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.*;
import org.matsim.core.trafficmonitoring.*;
import org.matsim.population.algorithms.*;
import org.matsim.ptproject.qsim.*;
import org.matsim.ptproject.qsim.agents.*;
import org.matsim.ptproject.qsim.qnetsimengine.*;
import org.matsim.run.*;
import org.matsim.vis.otfvis.*;
import org.matsim.vis.otfvis.gui.*;

import pl.poznan.put.util.jfreechart.*;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.vrp.dynamic.chart.*;
import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.optimizer.listener.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import playground.michalm.util.gis.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.file.*;
import playground.michalm.vrp.data.network.router.*;
import playground.michalm.vrp.data.network.shortestpath.sparse.*;
import playground.michalm.vrp.otfvis.*;
import playground.michalm.vrp.taxi.*;
import playground.michalm.vrp.taxi.taxicab.*;


public class SingleIterOnlineDVRPLauncher
{
    private String dirName;
    private String netFileName;
    private String plansFileName;
    private String depotsFileName;
    private boolean vrpOutFiles;
    private String vrpOutDirName;

    private boolean travelTimesFromEvents;
    private String eventsFileName;

    private Scenario scenario;
    private MATSimVRPData data;

    private boolean optimisticOptimizer;
    private TaxiOptimizerFactory optimizerFactory;

    private PersonalizableTravelTime ttimeCalc;
    private PersonalizableTravelCost tcostCalc;

    private boolean otfVis;
    public static OTFQueryControl queryControl;


    private void processArgs()
    {
        dirName = "D:\\PP-rad\\taxi\\mielec\\";
        netFileName = dirName + "network.xml";
        plansFileName = dirName + "plans.xml";
        depotsFileName = dirName + "depots.xml";

        travelTimesFromEvents = true;
        eventsFileName = "d:\\PP-rad\\taxi\\orig-mielec\\output\\std\\ITERS\\it.10\\10.events.xml.gz";

        optimisticOptimizer = !true;

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
        tcostCalc = new OnlyTimeDependentTravelCostCalculator(ttimeCalc);

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
        if (optimisticOptimizer) {
            optimizerFactory = OptimisticTaxiOptimizer.FACTORY;
        }
        else {
            optimizerFactory = PessimisticTaxiOptimizer.FACTORY;
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
        System.out.println(new VRPEvaluator().evaluateVRP(data.getVrpData()).toString());

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
