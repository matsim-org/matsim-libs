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

package playground.jbischoff.taxi.launch;

import java.io.*;
import java.util.*;

import org.jfree.chart.JFreeChart;
import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vis.otfvis.*;

import pl.poznan.put.util.jfreechart.*;
import pl.poznan.put.util.jfreechart.ChartUtils.OutputType;
import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.chart.*;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.ReqStatus;
import pl.poznan.put.vrp.dynamic.data.network.ArcFactory;
import pl.poznan.put.vrp.dynamic.optimizer.listener.ChartFileOptimizerListener;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.immediaterequest.TaxiOptimizationPolicy;
import playground.jbischoff.taxi.optimizer.NOSRankTaxiOptimizer;
import playground.jbischoff.taxi.optimizer.RankTaxiOptimizer;
import playground.jbischoff.taxi.optimizer.RankTaxiOptimizerFactory;
import playground.jbischoff.taxi.taxicab.JBTaxiSimEngine;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.util.gis.Schedules2GIS;
import playground.michalm.vrp.RunningVehicleRegister;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.file.DepotReader;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.router.*;
import playground.michalm.vrp.data.network.shortestpath.MatsimArcFactories;
import playground.michalm.vrp.otfvis.OTFLiveUtils;
import playground.michalm.vrp.run.VrpConfigUtils;
import playground.jbischoff.taxi.launch.AlgorithmConfig;
import playground.michalm.vrp.taxi.*;


public class JbSingleIterOnlineDvrpLauncher
{
    String dirName;
    String netFileName;
    String plansFileName;
    String taxiCustomersFileName;
    String depotsFileName;
    String reqIdToVehIdFileName;

    boolean vrpOutFiles;
    String vrpOutDirName;

    boolean outputHistogram;
    String histogramOutDirName = "outhist";
    LegHistogram legHistogram;

    AlgorithmConfig algorithmConfig;
    String eventsFileName;

    Scenario scenario;
    MatsimVrpData data;

    RankTaxiOptimizerFactory optimizerFactory;

    boolean otfVis;


    // public static OTFQueryControl queryControl;

    void defaultArgs()
    {
        dirName = "Z:\\WinHome\\Docs\\maciejewski\\jbtest\\";
        netFileName = dirName + "network.xml";

        plansFileName = dirName + "20.plans.xml.gz";

        taxiCustomersFileName = dirName + "taxiCustomers_05_pc.txt";
        // taxiCustomersFileName = dirName + "taxiCustomers_10_pc.txt";

        depotsFileName = dirName + "depots-5_taxis-50.xml";
        // depotsFileName = dirName + "depots-5_taxis-150.xml";

        // reqIdToVehIdFileName = dirName + "reqIdToVehId";

        eventsFileName = dirName + "20.events.xml.gz";

        // algorithmConfig = AlgorithmConfig.NOS_STRAIGHT_LINE;
        // algorithmConfig = AlgorithmConfig.NOS_TRAVEL_DISTANCE;
        algorithmConfig = AlgorithmConfig.NOS_FREE_FLOW;
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
        // algorithmConfig = AlgorithmConfig.RES_DRV_15_MIN;

        otfVis = !true;

        vrpOutFiles = !true;
        vrpOutDirName = dirName + "vrp_output";

        outputHistogram = true;
        vrpOutDirName = dirName + "histograms";
    }


    void readArgs(String paramFile)
        throws FileNotFoundException
    {
        Scanner scanner = new Scanner(new BufferedReader(new FileReader(paramFile)));
        Map<String, String> params = new HashMap<String, String>();

        while (scanner.hasNext()) {
            String key = scanner.next();
            String value = scanner.next();
            params.put(key, value);
        }

        dirName = params.get("dirName") + '\\';
        netFileName = dirName + params.get("netFileName");

        plansFileName = dirName + params.get("plansFileName");

        taxiCustomersFileName = dirName + params.get("taxiCustomersFileName");

        depotsFileName = dirName + params.get("depotsFileName");

        eventsFileName = dirName + params.get("eventsFileName");

        algorithmConfig = AlgorithmConfig.ALL[Integer.valueOf(params.get("algorithmConfig"))];

        otfVis = Boolean.valueOf(params.get("otfVis"));

        vrpOutFiles = Boolean.valueOf(params.get("vrpOutFiles"));

        vrpOutDirName = dirName + params.get("vrpOutDirName");
    }


    void prepareMatsimData()
        throws IOException
    {
        scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());

        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);

        List<String> taxiCustomerIds = ODDemandGenerator.readTaxiCustomerIds(taxiCustomersFileName);

        for (String id : taxiCustomerIds) {
            Person person = scenario.getPopulation().getPersons().get(scenario.createId(id));
            Leg leg = (Leg)person.getSelectedPlan().getPlanElements().get(1);
            leg.setMode(TaxiModeDepartureHandler.TAXI_MODE);
        }
    }


    void initMatsimVrpData()
        throws IOException
    {
        int travelTimeBinSize = algorithmConfig.ttimeSource.travelTimeBinSize;
        int numSlots = algorithmConfig.ttimeSource.numSlots;

        scenario.getConfig().travelTimeCalculator().setTraveltimeBinSize(travelTimeBinSize);

        TravelTime ttimeCalc;
        TravelDisutility tcostCalc;

        switch (algorithmConfig.ttimeSource) {
            case FREE_FLOW_SPEED:
                ttimeCalc = new FreeSpeedTravelTime();
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
                tcostCalc = new DistanceAsTravelDisutility();
                break;

            case TIME:
                tcostCalc = new TimeAsTravelDisutility(ttimeCalc);
                break;

            default:
                throw new IllegalArgumentException();
        }

        Network network = scenario.getNetwork();
        TimeDiscretizer timeDiscretizer = new TimeDiscretizer(travelTimeBinSize, numSlots);
        ArcFactory arcFactory = MatsimArcFactories.createArcFactory(network, ttimeCalc, tcostCalc,
                timeDiscretizer, false);
        MatsimVrpGraph graph = MatsimVrpGraphCreator.create(network, arcFactory, false);

        VrpData vrpData = new VrpData();
        vrpData.setVrpGraph(graph);
        vrpData.setCustomers(new ArrayList<Customer>());
        vrpData.setRequests(new ArrayList<Request>());
        new DepotReader(scenario, vrpData).readFile(depotsFileName);

        data = new MatsimVrpData(vrpData, scenario);
    }


    void initOptimizerFactory()
        throws IOException
    {
        switch (algorithmConfig.algorithmType) {
            case NO_SCHEDULING:
                optimizerFactory = NOSRankTaxiOptimizer.createFactory(
                        algorithmConfig == AlgorithmConfig.NOS_STRAIGHT_LINE,
                        TaxiOptimizationPolicy.ALWAYS);
                break;

//            case ONE_TIME_SCHEDULING:
//                optimizerFactory = OTSTaxiOptimizer
//                        .createFactory(algorithmConfig.optimizationPolicy);
//                break;
//
//            case RE_SCHEDULING:
//                optimizerFactory = RESTaxiOptimizer
//                        .createFactory(algorithmConfig.optimizationPolicy);
//                break;
//
//            case PRE_ASSIGNMENT:
//                File reqIdToVehIdFile = new File(reqIdToVehIdFileName);
//                Scanner scanner = new Scanner(reqIdToVehIdFile);
//
//                List<Vehicle> vehicles = data.getVrpData().getVehicles();
//                Vehicle[] reqIdToVehMapping = new Vehicle[scanner.nextInt()];
//
//                for (int i = 0; i < reqIdToVehMapping.length; i++) {
//                    reqIdToVehMapping[i] = vehicles.get(scanner.nextInt());
//                }
//
//                optimizerFactory = TaxiOptimizerWithPreassignment.createFactory(reqIdToVehMapping,
//                        algorithmConfig.optimizationPolicy);
//                break;

            default:
                throw new IllegalStateException();
        }
    }


    // just for debugging
    private RunningVehicleRegister rvr;


    @SuppressWarnings("unused")
    void runSim()
    {
        if (scenario.getConfig().getQSimConfigGroup() == null) {
            QSimConfigGroup qSimConfig = new QSimConfigGroup();
            qSimConfig.setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE);
            qSimConfig.setRemoveStuckVehicles(false);
            scenario.getConfig().addQSimConfigGroup(qSimConfig);
        }

        EventsManager events = EventsUtils.createEventsManager();
         EventWriter writer = new EventWriterXML(dirName + "events.xml.gz");
         events.addHandler(writer);

        rvr = new RunningVehicleRegister();
        events.addHandler(rvr);

        QSim qSim = new QSim(scenario, events);
        ActivityEngine activityEngine = new ActivityEngine();
        qSim.addMobsimEngine(activityEngine);
        qSim.addActivityHandler(activityEngine);
        QNetsimEngine netsimEngine = new DefaultQSimEngineFactory().createQSimEngine(qSim);
        qSim.addMobsimEngine(netsimEngine);
        qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
        TeleportationEngine teleportationEngine = new TeleportationEngine();
        qSim.addMobsimEngine(teleportationEngine);

        // // taken from SimEngine

        final VrpData vrpData = data.getVrpData();
        RankTaxiOptimizer optimizer = optimizerFactory.create(vrpData);
        TaxiOptimizationPolicy optimizationPolicy = optimizerFactory.getOptimizationPolicy();

        //
        //
        //
        TaxiOptimizer dummy = null;
        TaxiSimEngine taxiSimEngine = new JBTaxiSimEngine(qSim, data, dummy, optimizationPolicy,optimizer);

        //
        //
        //

        qSim.addMobsimEngine(taxiSimEngine);
        qSim.addAgentSource(new PopulationAgentSource(scenario.getPopulation(),
                new DefaultAgentFactory(qSim), qSim));
        qSim.addAgentSource(new TaxiAgentSource(data, taxiSimEngine));
        qSim.addDepartureHandler(new TaxiModeDepartureHandler(taxiSimEngine, data));

        OTFLiveUtils.initQueryHandler(qSim, vrpData);

        if (otfVis) { // OFTVis visualization
            OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(),
                    scenario, qSim.getEventsManager(), qSim);
            OTFClientLive.run(scenario.getConfig(), server);
        }

        // events.addHandler(runningVehicleRegister = new RunningVehicleRegister());

        if (outputHistogram) {
            legHistogram = new LegHistogram(300);
            events.addHandler(legHistogram);
        }

        qSim.run();

        events.finishProcessing();
        // writer.closeFile();

    }


    // RunningVehicleRegister runningVehicleRegister;

    void generateOutput()
    {
        // DVRP-based evaluation (using schedules)
//        TaxiEvaluation taxiEval = new TaxiEvaluator().evaluateVrp(data.getVrpData());
//        System.out.println(TaxiEvaluation.HEADER);
//        System.out.println(taxiEval.toString());

        // MATSim-based evaluation (using events)

        if (vrpOutFiles) {
            new File(vrpOutDirName).mkdir();
            new Schedules2GIS(data.getVrpData().getVehicles(), data)
                    .write(vrpOutDirName);
        }

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));

        if (outputHistogram) {
            new File(histogramOutDirName).mkdir();
            legHistogram.write(histogramOutDirName + "legHistogram.txt");
            legHistogram.writeGraphic(histogramOutDirName + "legHistogram_all.png");
            for (String legMode : legHistogram.getLegModes()) {
                legHistogram.writeGraphic(histogramOutDirName + "legHistogram_" + legMode + ".png",
                        legMode);
            }
        }
    }


    void go()
        throws IOException
    {
        initMatsimVrpData();
        initOptimizerFactory();
        runSim();

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
        String paramFile;
        if (args.length == 0) {
            paramFile = null;
        }
        else if (args.length == 1) {
            paramFile = args[0];
        }
        else {
            throw new RuntimeException();
        }

        JbSingleIterOnlineDvrpLauncher launcher = new JbSingleIterOnlineDvrpLauncher();

        if (paramFile == null) {
            launcher.defaultArgs();
        }
        else {
            launcher.readArgs(paramFile);
        }

        launcher.prepareMatsimData();
        launcher.go();
        launcher.generateOutput();
    }
}
