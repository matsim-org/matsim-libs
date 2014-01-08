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

package playground.michalm.taxi.run;

import java.io.*;
import java.util.*;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.util.chart.ScheduleChartUtils;
import org.matsim.contrib.dvrp.util.gis.Schedules2GIS;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ricardoFaria2012.EnergyConsumptionModelRicardoFaria2012;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.*;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import pl.poznan.put.util.jfreechart.ChartUtils;
import playground.michalm.RunningVehicleRegister;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.taxi.*;
import playground.michalm.taxi.model.*;
import playground.michalm.taxi.model.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.immediaterequest.*;
import playground.michalm.taxi.optimizer.immediaterequest.ImmediateRequestTaxiOptimizer.Params;


/*package*/class SingleIterTaxiLauncher
{
    /*package*/final String dirName;
    /*package*/final String netFileName;
    /*package*/final String plansFileName;
    /*package*/final String taxiCustomersFileName;
    /*package*/final String ranksFileName;

    /*package*/final boolean vrpOutFiles;
    /*package*/final String vrpOutDirName;

    /*package*/final boolean outHistogram;
    /*package*/final String histogramOutDirName;

    /*package*/final boolean otfVis;

    /*package*/final boolean writeSimEvents;
    /*package*/final String eventsFileName;

    /*package*/final Scenario scenario;

    /*package*/AlgorithmConfig algorithmConfig;
    /*package*/boolean destinationKnown;
    /*package*/boolean onlineVehicleTracker;
    /*package*/boolean minimizePickupTripTime;
    /*package*/int pickupDuration;
    /*package*/int dropoffDuration;

    /*package*/LegHistogram legHistogram;
    /*package*/MatsimVrpData data;
    /*package*/TaxiDelaySpeedupStats delaySpeedupStats;

    /*package*/TravelTimeCalculator travelTimeCalculator;


    /*package*/SingleIterTaxiLauncher()
        throws IOException
    {
        dirName = "D:\\PP-rad\\taxi\\mielec-2-peaks\\";
        netFileName = dirName + "network.xml";

        plansFileName = dirName + "output\\ITERS\\it.20\\20.plans.xml.gz";

        taxiCustomersFileName = dirName + "taxiCustomers_05_pc.txt";
        // taxiCustomersFileName = dirName + "taxiCustomers_10_pc.txt";

        ranksFileName = dirName + "depots-5_taxis-50.xml";
        // depotsFileName = dirName + "depots-5_taxis-150.xml";

        // reqIdToVehIdFileName = dirName + "reqIdToVehId";

        eventsFileName = dirName + "output\\ITERS\\it.20\\20.events.xml.gz";

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

        destinationKnown = false;
        onlineVehicleTracker = true;
        minimizePickupTripTime = false;
        pickupDuration = 120;
        dropoffDuration = 60;

        otfVis = !true;

        vrpOutFiles = !true;
        vrpOutDirName = dirName + "vrp_output";

        outHistogram = true;
        histogramOutDirName = dirName + "histograms";

        writeSimEvents = !true;

        scenario = VrpLauncherUtils.initScenario(netFileName, plansFileName);
        List<String> passengerIds = ODDemandGenerator.readTaxiCustomerIds(taxiCustomersFileName);
        VrpLauncherUtils.convertLegModes(passengerIds, TaxiRequestCreator.MODE, scenario);
    }


    /*package*/SingleIterTaxiLauncher(String paramFile)
        throws IOException
    {
        Scanner scanner;
        try {
            scanner = new Scanner(new BufferedReader(new FileReader(paramFile)));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        Map<String, String> params = new HashMap<String, String>();

        while (scanner.hasNext()) {
            String key = scanner.next();
            String value = scanner.next();
            params.put(key, value);
        }
        scanner.close();

//        dirName = params.get("dirName") + '/';
        dirName = new File(paramFile).getParent() + '/';
        netFileName = dirName + params.get("netFileName");

        plansFileName = dirName + params.get("plansFileName");

        taxiCustomersFileName = dirName + params.get("taxiCustomersFileName");

        ranksFileName = dirName + params.get("depotsFileName");

        eventsFileName = dirName + params.get("eventsFileName");

        algorithmConfig = AlgorithmConfig.ALL[Integer.valueOf(params.get("algorithmConfig"))];

        destinationKnown = Boolean.valueOf(params.get("destinationKnown"));
        onlineVehicleTracker = Boolean.valueOf(params.get("onlineVehicleTracker"));
        minimizePickupTripTime = Boolean.valueOf(params.get("minimizePickupTripTime"));
        pickupDuration = Integer.valueOf(params.get("pickupDuration"));
        dropoffDuration = Integer.valueOf(params.get("dropoffDuration"));

        otfVis = Boolean.valueOf(params.get("otfVis"));

        vrpOutFiles = Boolean.valueOf(params.get("vrpOutFiles"));
        vrpOutDirName = dirName + params.get("vrpOutDirName");

        outHistogram = Boolean.valueOf(params.get("outHistogram"));
        histogramOutDirName = dirName + params.get("histogramOutDirName");

        writeSimEvents = Boolean.valueOf(params.get("writeSimEvents"));

        scenario = VrpLauncherUtils.initScenario(netFileName, plansFileName);
        List<String> passengerIds = ODDemandGenerator.readTaxiCustomerIds(taxiCustomersFileName);
        VrpLauncherUtils.convertLegModes(passengerIds, TaxiRequestCreator.MODE, scenario);
    }


    /**
     * Can be called several times (1 call == 1 simulation)
     */
    /*package*/void go(boolean warmup)
    {
        TravelTime travelTime = VrpLauncherUtils.initTravelTime(scenario, travelTimeCalculator,
                algorithmConfig.ttimeSource, eventsFileName);

        TravelDisutility travelDisutility = VrpLauncherUtils.initTravelDisutility(
                algorithmConfig.tdisSource, travelTime);

        VrpPathCalculator calculator = VrpLauncherUtils.initVrpPathFinder(scenario,
                algorithmConfig.ttimeSource, travelTime, travelDisutility);

        EnergyConsumptionModel ecm = new EnergyConsumptionModelRicardoFaria2012();

        TaxiData taxiData = TaxiLauncherUtils.initTaxiData(scenario, ranksFileName, ecm);

        data = new MatsimVrpData(taxiData, scenario);

        Params params = new Params(destinationKnown, minimizePickupTripTime, pickupDuration,
                dropoffDuration);
        ImmediateRequestTaxiOptimizer optimizer = algorithmConfig.createTaxiOptimizer(taxiData,
                calculator, params);

        QSim qSim = VrpLauncherUtils.initQSim(scenario);

        VrpSimEngine vrpSimEngine = VrpLauncherUtils.initVrpSimEngine(qSim, data, optimizer);

        TaxiActionCreator actionCreator = new TaxiActionCreator(vrpSimEngine, onlineVehicleTracker);

        VrpLauncherUtils.initAgentSources(qSim, data, vrpSimEngine, actionCreator);

        VrpLauncherUtils.initDepartureHandler(qSim, data, vrpSimEngine,
                new TaxiRequestCreator(data), TaxiRequestCreator.MODE);

        EventsManager events = qSim.getEventsManager();

        EventWriter eventWriter = null;
        if (writeSimEvents) {
            eventWriter = new EventWriterXML(dirName + "events.xml.gz");
            events.addHandler(eventWriter);
        }

        if (warmup) {
            if (travelTimeCalculator == null) {
                travelTimeCalculator = new TravelTimeCalculator(scenario.getNetwork(), scenario
                        .getConfig().travelTimeCalculator());
            }

            events.addHandler(travelTimeCalculator);
        }
        else {
            optimizer.setDelaySpeedupStats(delaySpeedupStats);
        }

        RunningVehicleRegister rvr = new RunningVehicleRegister();
        events.addHandler(rvr);

        if (otfVis) { // OFTVis visualization
            ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME,
                    OTFVisConfigGroup.class).setColoringScheme(ColoringScheme.taxicab);
            OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(),
                    scenario, qSim.getEventsManager(), qSim);
            OTFClientLive.run(scenario.getConfig(), server);
        }

        if (outHistogram) {
            events.addHandler(legHistogram = new LegHistogram(300));
        }

        qSim.run();

        events.finishProcessing();

        if (writeSimEvents) {
            eventWriter.closeFile();
        }

        // check if all reqs have been served
        for (TaxiRequest r : taxiData.getTaxiRequests()) {
            if (r.getStatus() != TaxiRequestStatus.PERFORMED) {
                throw new IllegalStateException();
            }
        }

        // TravelTime ttCalc = calc.getLinkTravelTimes();
        //
        // Link link = scenario.getNetwork().getLinks().get(scenario.createId("379"));
        // System.err.println("14000 ---> " + ttCalc.getLinkTravelTime(link, 14000, null, null));
        // System.err.println("15000 ---> " + ttCalc.getLinkTravelTime(link, 15000, null, null));
        // System.err.println("16000 ---> " + ttCalc.getLinkTravelTime(link, 16000, null, null));
        //
        // TravelDisutility tcCalc = new TimeAsTravelDisutility(ttCalc);
        //
        // LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), tcCalc, ttCalc);
        // ShortestPathCalculator shortestPathCalculator = new ShortestPathCalculator(router,
        // ttCalc,
        // tcCalc);
        //
        // SummaryStatistics aPrioriTPDiff = new SummaryStatistics();
        // SummaryStatistics aPosterioriTPDiff = new SummaryStatistics();
        // SummaryStatistics aPrioriTDDiff = new SummaryStatistics();
        // SummaryStatistics aPosterioriTDDiff = new SummaryStatistics();
        //
        // for (Vehicle v : data.getVrpData().getVehicles()) {
        // for (Task t : v.getSchedule().getTasks()) {
        // if (t.getType() == TaskType.DRIVE) {
        // DriveTask dt = (DriveTask)t;
        // int beginTime = dt.getBeginTime();
        // MatsimArc arc = (MatsimArc)dt.getArc();
        //
        // int actualDuration = dt.getEndTime() - beginTime;
        //
        // int aPrioriEstimation = arc.getTimeOnDeparture(beginTime);
        //
        // int aPosterioriEstimation = shortestPathCalculator.calculateShortestPath(arc
        // .getFromLink(), arc.getToLink(), beginTime).travelTime;
        //
        // if (aPosterioriEstimation != 0) {
        // if ( (actualDuration - aPosterioriEstimation) / aPosterioriEstimation > 1) {
        // ShortestPath sp = shortestPathCalculator.calculateShortestPath(arc
        // .getFromLink(), arc.getToLink(),
        // beginTime);
        //
        // System.out.println(v.getId() + " : " + (beginTime / 3600) + " : "
        // + Arrays.asList(sp.linkIds));
        // }
        // }
        // else {
        // if (actualDuration > 1) {
        // ShortestPath sp = shortestPathCalculator.calculateShortestPath(arc
        // .getFromLink(), arc.getToLink(),
        // beginTime);
        //
        // System.out.println("0==" + v.getId() + " : " + (beginTime / 3600)
        // + " : " + Arrays.asList(sp.linkIds));
        // }
        // }
        //
        // switch ( ((TaxiDriveTask)dt).getDriveType()) {
        // case PICKUP:
        // aPrioriTPDiff.addValue(actualDuration - aPrioriEstimation);
        // aPosterioriTPDiff.addValue(actualDuration - aPosterioriEstimation);
        // break;
        //
        // case DELIVERY:
        // aPrioriTDDiff.addValue(actualDuration - aPrioriEstimation);
        // aPosterioriTDDiff.addValue(actualDuration - aPosterioriEstimation);
        // break;
        //
        // default:
        // throw new RuntimeException();
        // }
        //
        // }
        // }
        // }
        //
        // System.err.println("aPriori T_P: " + aPrioriTPDiff.getMean());
        // System.err.println("aPosteriori T_P: " + aPosterioriTPDiff.getMean());
        //
        // System.err.println("aPriori T_D: " + aPrioriTDDiff.getMean());
        // System.err.println("aPosteriori T_D: " + aPosterioriTDDiff.getMean());
    }


    /*package*/void generateOutput()
    {
        PrintWriter pw = new PrintWriter(System.out);
        new TaxiStatsCalculator().calculateStats(data.getVrpData()).print(pw);
        pw.flush();

        if (vrpOutFiles) {
            new Schedules2GIS(data.getVrpData().getVehicles(), TransformationFactory.WGS84_UTM33N)
                    .write(vrpOutDirName);
        }

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));

        if (outHistogram) {
            VrpLauncherUtils.writeHistograms(legHistogram, histogramOutDirName);
        }
    }


    public static void main(String... args)
        throws IOException
    {
        SingleIterTaxiLauncher launcher;
        if (args.length == 0) {
            launcher = new SingleIterTaxiLauncher();
        }
        else if (args.length == 1) {
            launcher = new SingleIterTaxiLauncher(args[0]);
        }
        else {
            throw new RuntimeException();
        }

        launcher.go(false);
        launcher.generateOutput();
    }
}
