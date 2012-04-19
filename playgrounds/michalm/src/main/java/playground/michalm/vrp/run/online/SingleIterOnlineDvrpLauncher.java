/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.run.online;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.gui.OTFQueryControl;

import pl.poznan.put.util.jfreechart.ChartUtils;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.vrp.dynamic.chart.ChartCreator;
import pl.poznan.put.vrp.dynamic.chart.RouteChartUtils;
import pl.poznan.put.vrp.dynamic.chart.ScheduleChartUtils;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.network.FixedSizeVrpGraph;
import pl.poznan.put.vrp.dynamic.optimizer.listener.ChartFileOptimizerListener;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiEvaluator;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizer.AlgorithmType;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizerFactory;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizerWithPreassignment;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizerWithReassignment;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizerWithoutReassignment;
import playground.michalm.util.gis.Schedules2GIS;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.MatsimVrpDataCreator;
import playground.michalm.vrp.data.file.DepotReader;
import playground.michalm.vrp.data.network.router.TravelTimeCalculators;
import playground.michalm.vrp.data.network.shortestpath.sparse.SparseShortestPathArc;
import playground.michalm.vrp.data.network.shortestpath.sparse.SparseShortestPathFinder;
import playground.michalm.vrp.otfvis.VrpOTFClientLive;
import playground.michalm.vrp.taxi.TaxiModeDepartureHandler;
import playground.michalm.vrp.taxi.TaxiSimEngine;
import playground.michalm.vrp.taxi.taxicab.TaxiAgentSource;


public class SingleIterOnlineDvrpLauncher
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
    private MatsimVrpData data;

    private AlgorithmType algorithmType;
    private TaxiOptimizerFactory optimizerFactory;

    private PersonalizableTravelTime ttimeCalc;
    private TravelDisutility tcostCalc;

    private boolean otfVis;
    public static OTFQueryControl queryControl;


    private void processArgs()
    {
        // dirName = "D:\\PP-rad\\taxi\\mielec-nowe-OD\\";
        dirName = "D:\\PP-rad\\taxi\\poznan\\";

        netFileName = dirName + "network.xml";
        plansFileName = dirName + "plans.xml";
        depotsFileName = dirName + "depots.xml";
        reqIdToVehIdFileName = dirName + "reqIdToVehId";

        travelTimesFromEvents = true;
        // eventsFileName =
        // "d:\\PP-rad\\taxi\\orig-mielec-nowe-OD\\output\\std\\ITERS\\it.10\\10.events.xml.gz";
        eventsFileName = "d:\\PP-rad\\taxi\\poznan\\output\\ITERS\\it.20\\20.events.xml.gz";

        algorithmType = AlgorithmType.RE_ASSIGNMENT;

        otfVis = true;

        vrpOutFiles = !true;
        vrpOutDirName = dirName + "\\vrp_output";
        new File(vrpOutDirName).mkdir();
    }


    private void prepareMatsimData()
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


    private void initMatsimVrpData()
        throws IOException
    {
        data = MatsimVrpDataCreator.create(scenario);
        new DepotReader(scenario, data).readFile(depotsFileName);

        LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), tcostCalc, ttimeCalc);

        SparseShortestPathFinder sspf = new SparseShortestPathFinder(data);
        SparseShortestPathArc[][] arcs = sspf.findShortestPaths(ttimeCalc, tcostCalc, router);
        ((FixedSizeVrpGraph)data.getVrpGraph()).setArcs(arcs);
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

        QSim sim = QSim.createQSimWithDefaultEngines(scenario, events,
                new DefaultQSimEngineFactory());

        TaxiSimEngine taxiSimEngine = new TaxiSimEngine(sim, data, optimizerFactory);
        sim.addMobsimEngine(taxiSimEngine);
        sim.addAgentSource(new PopulationAgentSource(scenario.getPopulation(),
                new DefaultAgentFactory(sim), sim));
        sim.addAgentSource(new TaxiAgentSource(data, taxiSimEngine));
        sim.addDepartureHandler(new TaxiModeDepartureHandler(taxiSimEngine, data));

        if (vrpOutFiles) {
            taxiSimEngine.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                @Override
								public JFreeChart createChart(VrpData data)
                {
                    return RouteChartUtils.chartRoutesByStatus(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\routes_", 800, 800));

            taxiSimEngine.addListener(new ChartFileOptimizerListener(new ChartCreator() {
                @Override
								public JFreeChart createChart(VrpData data)
                {
                    return ScheduleChartUtils.chartSchedule(data);
                }
            }, OutputType.PNG, vrpOutDirName + "\\schedules_", 1200, 800));
        }

        if (otfVis) { // OFTVis visualization
            OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(),
                    scenario, sim.getEventsManager(), sim);
            VrpOTFClientLive.run(scenario.getConfig(), server);
        }

        sim.run();
    }


    private void generateVrpOutput()
    {
        System.out.println(new TaxiEvaluator().evaluateVrp(data.getVrpData()).toString());

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
        prepareMatsimData();
        initMatsimVrpData();
        initOptimizerFactory();
        runSim();
        generateVrpOutput();
    }


    public static void main(String... args)
        throws IOException
    {
        new SingleIterOnlineDvrpLauncher().go();
    }
}
