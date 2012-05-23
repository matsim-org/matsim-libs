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

import java.io.*;
import java.util.*;

import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.gui.OTFQueryControl;

import pl.poznan.put.util.jfreechart.*;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.vrp.dynamic.chart.*;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.ReqStatus;
import pl.poznan.put.vrp.dynamic.data.network.FixedSizeVrpGraph;
import pl.poznan.put.vrp.dynamic.optimizer.listener.ChartFileOptimizerListener;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.util.gis.Schedules2GIS;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.file.DepotReader;
import playground.michalm.vrp.data.network.router.*;
import playground.michalm.vrp.data.network.shortestpath.sparse.*;
import playground.michalm.vrp.otfvis.VrpOTFClientLive;
import playground.michalm.vrp.taxi.*;
import playground.michalm.vrp.taxi.taxicab.TaxiAgentSource;
import playground.michalm.vrp.taxi.wal.WalTaxiSimEngine;


public class SingleIterOnlineDvrpLauncher
{
    private String dirName;
    private String netFileName;
    private String plansFileName;
    private String taxiCustomersFileName;
    private String depotsFileName;
    private String reqIdToVehIdFileName;
    private String dbFileName;
    private boolean vrpOutFiles;
    private String vrpOutDirName;

    private AlgorithmConfig algorithmConfig;
    private String eventsFileName;

    private Scenario scenario;
    private MatsimVrpData data;

    private TaxiOptimizerFactory optimizerFactory;

    private boolean otfVis;
    public static OTFQueryControl queryControl;

    private boolean wal;


    private void processArgs(String... args)
    {
        if (args.length == 1) {// Wal - do not change the following block
            dirName = args[0] + "\\";
            netFileName = dirName + "network.xml";
            plansFileName = dirName + "output\\ITERS\\it.20\\20.plans.xml.gz";

            depotsFileName = dirName + "depots.xml";
            reqIdToVehIdFileName = dirName + "reqIdToVehId";

            taxiCustomersFileName = dirName + "taxiCustomers_5_pc.txt";

            dbFileName = dirName + "system_state.mdb";

            eventsFileName = dirName + "output\\ITERS\\it.20\\20.events.xml.gz";

            algorithmConfig = AlgorithmConfig.RES_DRV_15_MIN;

            otfVis = !true;

            vrpOutFiles = !true;
            vrpOutDirName = dirName + "vrp_output";

            wal = true;
        }
        else if (args.length == 2 && args[1] == "KAI") { // demo version for Kai
            dirName = args[0] + "\\";
            netFileName = dirName + "network.xml";
            plansFileName = dirName + "output\\ITERS\\it.20\\20.plans.xml.gz";

            reqIdToVehIdFileName = dirName + "reqIdToVehId";

            depotsFileName = dirName + "depots-5_taxis-15.xml";
            taxiCustomersFileName = dirName + "taxiCustomers_1_pc.txt";

            eventsFileName = dirName + "output\\ITERS\\it.20\\20.events.xml.gz";

            algorithmConfig = AlgorithmConfig.RES_DRV_15_MIN;

            otfVis = true;

            vrpOutFiles = !true;
            vrpOutDirName = dirName + "vrp_output";

            wal = false;
        }
        else {
            dirName = "D:\\PP-rad\\taxi\\mielec\\";
            netFileName = dirName + "network.xml";

            // plansFileName = dirName + "plans.xml";
            plansFileName = dirName + "output\\ITERS\\it.20\\20.plans.xml.gz";

            depotsFileName = dirName + "depots.xml";

            // depotsFileName = dirName + "depots-5_taxis-10.xml";
            // depotsFileName = dirName + "depots-5_taxis-15.xml";
            // taxiCustomersFileName = dirName + "taxiCustomers_1_pc.txt";

            // taxiCustomersFileName = dirName + "taxiCustomers_10_pc.txt";
            // depotsFileName = dirName + "depots-5_taxis-150.xml";

            // taxiCustomersFileName = dirName + "taxiCustomers_20_pc.txt";
            // depotsFileName = dirName + "depots-5_taxis-500.xml";

            // reqIdToVehIdFileName = dirName + "reqIdToVehId";

            eventsFileName = dirName + "output\\ITERS\\it.20\\20.events.xml.gz";

            // algorithmConfig = AlgorithmConfig.NOS_STRAIGHT_LINE;
            // algorithmConfig = AlgorithmConfig.NOS_TRAVEL_DISTANCE;
            // algorithmConfig = AlgorithmConfig.NOS_FREE_FLOW;
            // algorithmConfig = AlgorithmConfig.NOS_24_H;
            // algorithmConfig = AlgorithmConfig.NOS_15_MIN;
            // algorithmConfig = AlgorithmConfig.OTS_REQ_FREE_FLOW;
            // algorithmConfig = AlgorithmConfig.OTS_REQ_24_H;
            // algorithmConfig = AlgorithmConfig.OTS_REQ_15_MIN;
            // algorithmConfig = AlgorithmConfig.OTS_DRV_FREE_FLOW;
            // algorithmConfig = AlgorithmConfig.OTS_DRV_24_H;
            // algorithmConfig = AlgorithmConfig.OTS_DRV_15_MIN;
            // algorithmConfig = AlgorithmConfig.RES_REQ_FREE_FLOW;
            // algorithmConfig = AlgorithmConfig.RES_REQ_24_H;
            // algorithmConfig = AlgorithmConfig.RES_REQ_15_MIN;
            // algorithmConfig = AlgorithmConfig.RES_DRV_FREE_FLOW;
            // algorithmConfig = AlgorithmConfig.RES_DRV_24_H;
            algorithmConfig = AlgorithmConfig.RES_DRV_15_MIN;

            otfVis = !true;

            vrpOutFiles = !true;
            vrpOutDirName = dirName + "vrp_output";

            wal = false;
        }

        if (vrpOutFiles) {
            new File(vrpOutDirName).mkdir();
        }
    }


    private void prepareMatsimData()
        throws IOException
    {
        Config config = ConfigUtils.createConfig();
        scenario = ScenarioUtils.createScenario(config);

        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);

        List<String> taxiCustomerIds = ODDemandGenerator.readTaxiCustomerIds(taxiCustomersFileName);

        for (String id : taxiCustomerIds) {
            Person person = scenario.getPopulation().getPersons().get(scenario.createId(id));
            Leg leg = (Leg)person.getSelectedPlan().getPlanElements().get(1);
            leg.setMode(TaxiModeDepartureHandler.TAXI_MODE);
        }
    }


    private void initMatsimVrpData()
        throws IOException
    {
        int travelTimeBinSize = algorithmConfig.ttimeSource.travelTimeBinSize;
        int numSlots = algorithmConfig.ttimeSource.numSlots;

        scenario.getConfig().travelTimeCalculator().setTraveltimeBinSize(travelTimeBinSize);

        PersonalizableTravelTime ttimeCalc;
        TravelDisutility tcostCalc;

        switch (algorithmConfig.ttimeSource) {
            case FREE_FLOW_SPEED:
                ttimeCalc = new FreeSpeedTravelTimeCalculator();
                break;

            case EVENTS_15_MIN:
            case EVENTS_24_H:
                ttimeCalc = TravelTimeCalculators.createTravelTimeFromEvents(eventsFileName,
                        scenario);
                break;

            default:
                throw new IllegalArgumentException();
        }

        switch (algorithmConfig.tcostSource) {
            case DISTANCE:
                tcostCalc = new DistanceAsTravelCost();
                break;

            case TIME:
                tcostCalc = new TimeAsTravelCost(ttimeCalc);
                break;

            default:
                throw new IllegalArgumentException();
        }

        data = MatsimVrpDataCreator.create(scenario);
        new DepotReader(scenario, data).readFile(depotsFileName);

        LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), tcostCalc, ttimeCalc);

        SparseShortestPathFinder sspf = new SparseShortestPathFinder(data, travelTimeBinSize,
                numSlots);
        SparseShortestPathArc[][] arcs = sspf.findShortestPaths(ttimeCalc, tcostCalc, router);
        ((FixedSizeVrpGraph)data.getVrpGraph()).setArcs(arcs);
    }


    private void initOptimizerFactory()
        throws IOException
    {
        switch (algorithmConfig.algorithmType) {
            case NO_SCHEDULING:
                optimizerFactory = IdleTaxiDispatcher.createFactory(
                        algorithmConfig == AlgorithmConfig.NOS_STRAIGHT_LINE,
                        algorithmConfig.optimizationPolicy);
                break;

            case ONE_TIME_SCHEDULING:
                optimizerFactory = TaxiOptimizerWithoutReassignment
                        .createFactory(algorithmConfig.optimizationPolicy);
                break;

            case RE_SCHEDULING:
                optimizerFactory = TaxiOptimizerWithReassignment
                        .createFactory(algorithmConfig.optimizationPolicy);
                break;

            case PRE_ASSIGNMENT:
                File reqIdToVehIdFile = new File(reqIdToVehIdFileName);
                Scanner scanner = new Scanner(reqIdToVehIdFile);

                List<Vehicle> vehicles = data.getVrpData().getVehicles();
                Vehicle[] reqIdToVehMapping = new Vehicle[scanner.nextInt()];

                for (int i = 0; i < reqIdToVehMapping.length; i++) {
                    reqIdToVehMapping[i] = vehicles.get(scanner.nextInt());
                }

                optimizerFactory = TaxiOptimizerWithPreassignment.createFactory(reqIdToVehMapping,
                        algorithmConfig.optimizationPolicy);
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
        QSim qSim = new QSim(scenario, events);
        ActivityEngine activityEngine = new ActivityEngine();
        qSim.addMobsimEngine(activityEngine);
        qSim.addActivityHandler(activityEngine);
        QNetsimEngine netsimEngine = new DefaultQSimEngineFactory().createQSimEngine(qSim,
                MatsimRandom.getRandom());
        qSim.addMobsimEngine(netsimEngine);
        qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
        TeleportationEngine teleportationEngine = new TeleportationEngine();
        qSim.addMobsimEngine(teleportationEngine);

        TaxiSimEngine taxiSimEngine = wal ? new WalTaxiSimEngine(qSim, data, optimizerFactory,
                dbFileName) : new TaxiSimEngine(qSim, data, optimizerFactory);
        qSim.addMobsimEngine(taxiSimEngine);
        qSim.addAgentSource(new PopulationAgentSource(scenario.getPopulation(),
                new DefaultAgentFactory(qSim), qSim));
        qSim.addAgentSource(new TaxiAgentSource(data, taxiSimEngine));
        qSim.addDepartureHandler(new TaxiModeDepartureHandler(taxiSimEngine, data));

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
                    scenario, qSim.getEventsManager(), qSim);
            VrpOTFClientLive.run(scenario.getConfig(), server);
        }

        qSim.run();
    }


    private void generateVrpOutput()
    {
        System.out.println(new TaxiEvaluator().evaluateVrp(data.getVrpData()).toString());

        if (vrpOutFiles) {
            new Schedules2GIS(data.getVrpData().getVehicles(), data, vrpOutDirName + "\\route_")
                    .write();
        }

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));
    }


    private void go(String... args)
        throws IOException
    {
        processArgs(args);
        prepareMatsimData();
        initMatsimVrpData();
        initOptimizerFactory();
        runSim();
        generateVrpOutput();

        // check
        for (Request r : data.getVrpData().getRequests()) {
            if (r.getStatus() != ReqStatus.PERFORMED) {
                throw new IllegalStateException();
            }
        }
    }


    public static void main(String... args)
        throws IOException
    {
        new SingleIterOnlineDvrpLauncher().go(args);
    }
}
