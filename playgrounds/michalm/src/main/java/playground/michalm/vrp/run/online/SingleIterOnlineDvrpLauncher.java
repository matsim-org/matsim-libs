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
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.gui.OTFQueryControl;

import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.vrp.dynamic.chart.*;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
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
        if (args.length == 1) {
            dirName = args[0];
        }
        else {
            dirName = "D:\\PP-rad\\taxi\\mielec\\";
            // dirName = "D:\\PP-rad\\taxi\\poznan\\";
        }

        if (!dirName.endsWith("\\")) {
            dirName += "\\";
        }

        if (args.length == 1) {// Wal - do not change the following block
            netFileName = dirName + "network.xml";
            plansFileName = dirName + "plans.xml";
            depotsFileName = dirName + "depots.xml";
            reqIdToVehIdFileName = dirName + "reqIdToVehId";
            dbFileName = dirName + "system_state.mdb";

            // eventsFileName = dirName + "output\\std\\ITERS\\it.10\\10.events.xml.gz";
            eventsFileName = dirName + "output\\ITERS\\it.20\\20.events.xml.gz";

            algorithmConfig = AlgorithmConfig.RES_DRV_15_MIN;

            otfVis = !true;

            vrpOutFiles = !true;
            vrpOutDirName = dirName + "vrp_output";

            wal = true;

            taxiCustomersFileName = null;
        }
        else {
            netFileName = dirName + "network.xml";

            // plansFileName = dirName + "plans.xml";
            plansFileName = dirName + "output\\std\\ITERS\\it.20\\20.plans.xml.gz";

            // depotsFileName = dirName + "depots-5_taxis-10.xml";
            depotsFileName = dirName + "depots-5_taxis-15.xml";
            reqIdToVehIdFileName = dirName + "reqIdToVehId";
            dbFileName = dirName + "system_state.mdb";

            eventsFileName = dirName + "output\\std\\ITERS\\it.20\\20.events.xml.gz";

            algorithmConfig = AlgorithmConfig.NOS_STRAIGHT_LINE;
            algorithmConfig = AlgorithmConfig.NOS_TRAVEL_DISTANCE;
            algorithmConfig = AlgorithmConfig.NOS_FREE_FLOW;
            algorithmConfig = AlgorithmConfig.NOS_24_H;
            algorithmConfig = AlgorithmConfig.NOS_15_MIN;
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
            // algorithmConfig = AlgorithmConfig.RES_DRV_15_MIN;

            otfVis = !true;

            vrpOutFiles = !true;
            vrpOutDirName = dirName + "vrp_output";

            wal = false;

            taxiCustomersFileName = dirName + "taxiCustomers_1_pc.txt";
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
        scenario.getConfig().travelTimeCalculator()
                .setTraveltimeBinSize(algorithmConfig.ttimeSource.travelTimeBinSize);

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

        SparseShortestPathFinder sspf = new SparseShortestPathFinder(data);
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

        QSim sim = QSim.createQSimWithDefaultEngines(scenario, events,
                new DefaultQSimEngineFactory());

        TaxiSimEngine taxiSimEngine = wal ? new WalTaxiSimEngine(sim, data, optimizerFactory,
                dbFileName) : new TaxiSimEngine(sim, data, optimizerFactory);
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

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        // ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));
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
    }


    public static void main(String... args)
        throws IOException
    {
        new SingleIterOnlineDvrpLauncher().go(args);
    }
}
