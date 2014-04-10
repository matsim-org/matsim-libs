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
import org.matsim.contrib.dvrp.*;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.util.gis.Schedules2GIS;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpDynLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import pl.poznan.put.util.jfreechart.ChartUtils;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.taxi.*;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.scheduler.*;
import playground.michalm.taxi.util.chart.TaxiScheduleChartUtils;
import playground.michalm.taxi.util.stats.*;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;
import playground.michalm.util.RunningVehicleRegister;


/*package*/class TaxiLauncher
{
    /*package*/final String dirName;
    /*package*/final String netFileName;
    /*package*/final String plansFileName;
    /*package*/final String taxiCustomersFileName;
    /*package*/String taxisFileName;
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
    /*package*/Boolean onlineVehicleTracker;
    /*package*/Boolean advanceRequestSubmission;
    /*package*/Double pickupTripTimeLimit;

    /*package*/Boolean destinationKnown;
    /*package*/Double pickupDuration;
    /*package*/Double dropoffDuration;

    /*package*/LegHistogram legHistogram;
    /*package*/MatsimVrpContext context;
    /*package*/TaxiDelaySpeedupStats delaySpeedupStats;
    /*package*/LeastCostPathCalculatorCacheStats cacheStats;

    private TravelTimeCalculator travelTimeCalculator;
    private LeastCostPathCalculatorWithCache routerWithCache;
    private VrpPathCalculator pathCalculator;


    /*package*/TaxiLauncher()
    {
        dirName = "D:\\PP-rad\\taxi\\mielec-2-peaks\\";
        netFileName = dirName + "network.xml";

        plansFileName = dirName + "output\\ITERS\\it.20\\20.plans.xml.gz";

        taxiCustomersFileName = dirName + "taxiCustomers_05_pc.txt";
        // taxiCustomersFileName = dirName + "taxiCustomers_10_pc.txt";

        ranksFileName = null;
        taxisFileName = dirName + "ranks-5_taxis-50.xml";
        // ranksFileName = dirName + "ranks-5_taxis-150.xml";

        // reqIdToVehIdFileName = dirName + "reqIdToVehId";

        eventsFileName = dirName + "output\\ITERS\\it.20\\20.events.xml.gz";

        //optimizer:
        algorithmConfig = AlgorithmConfig.NOS_DSE_SL;
        onlineVehicleTracker = true;
        advanceRequestSubmission = false;
        pickupTripTimeLimit = 10 * 60.;//10 minutes

        //scheduler:
        destinationKnown = false;
        pickupDuration = 120.;
        dropoffDuration = 60.;

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


    /*package*/TaxiLauncher(String paramFile)
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

        ranksFileName = dirName + params.get("ranksFileName");
        taxisFileName = dirName + params.get("taxisFileName");

        eventsFileName = dirName + params.get("eventsFileName");

        algorithmConfig = AlgorithmConfig.valueOf(params.get("algorithmConfig"));
        onlineVehicleTracker = Boolean.valueOf(params.get("onlineVehicleTracker"));
        advanceRequestSubmission = Boolean.valueOf(params.get("advanceRequestSubmission"));

        destinationKnown = Boolean.valueOf(params.get("destinationKnown"));
        pickupDuration = Double.valueOf(params.get("pickupDuration"));
        dropoffDuration = Double.valueOf(params.get("dropoffDuration"));

        otfVis = Boolean.valueOf(params.get("otfVis"));

        vrpOutFiles = Boolean.valueOf(params.get("vrpOutFiles"));
        vrpOutDirName = dirName + params.get("vrpOutDirName");

        outHistogram = Boolean.valueOf(params.get("outHistogram"));
        histogramOutDirName = dirName + params.get("histogramOutDirName");

        writeSimEvents = Boolean.valueOf(params.get("writeSimEvents"));

        scenario = VrpLauncherUtils.initScenario(netFileName, plansFileName);
//        scenario = VrpLauncherUtils.initTimeVariantScenario(netFileName, plansFileName,
//                dirName + "changeevents.xml.gz");

        List<String> passengerIds = ODDemandGenerator.readTaxiCustomerIds(taxiCustomersFileName);
//        VrpLauncherUtils.convertLegModes(passengerIds, TaxiRequestCreator.MODE, scenario);
        VrpLauncherUtils.convertLegModesAndRemoveOtherModes(passengerIds, TaxiRequestCreator.MODE, scenario);
    }


    /*package*/void initVrpPathCalculator()
    {
        TravelTime travelTime = travelTimeCalculator == null ? //
                VrpLauncherUtils.initTravelTime(scenario, algorithmConfig.ttimeSource,
                        eventsFileName) : //
                travelTimeCalculator.getLinkTravelTimes();

        TravelDisutility travelDisutility = VrpLauncherUtils.initTravelDisutility(
                algorithmConfig.tdisSource, travelTime);

        LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), travelDisutility,
                travelTime);

        routerWithCache = new LeastCostPathCalculatorWithCache(router,
                algorithmConfig.ttimeSource.timeDiscretizer);

        pathCalculator = new VrpPathCalculatorImpl(routerWithCache, travelTime, travelDisutility);
    }


    /*package*/void clearVrpPathCalculator()
    {
        travelTimeCalculator = null;
        routerWithCache = null;
        pathCalculator = null;
    }


    /**
     * Can be called several times (1 call == 1 simulation)
     */
    /*package*/void go(boolean warmup)
    {
        MatsimVrpContextImpl contextImpl = new MatsimVrpContextImpl();
        this.context = contextImpl;

        contextImpl.setScenario(scenario);

        TaxiData taxiData = TaxiLauncherUtils.initTaxiData(scenario, taxisFileName, ranksFileName);
        contextImpl.setVrpData(taxiData);

        TaxiOptimizerConfiguration optimizerConfig = createOptimizerConfiguration();
        TaxiOptimizer optimizer = algorithmConfig.createTaxiOptimizer(optimizerConfig);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
        contextImpl.setMobsimTimer(qSim.getSimTimer());

        qSim.addQueueSimulationListeners(optimizer);

        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(
                TaxiRequestCreator.MODE, new TaxiRequestCreator(), optimizer, context, qSim);

        if (advanceRequestSubmission) {
            qSim.addQueueSimulationListeners(new BeforeSimulationTaxiCaller(passengerEngine));
        }

        LegCreator legCreator = onlineVehicleTracker ? VrpDynLegs
                .createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer())
                : VrpDynLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR;

        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                pickupDuration);

        VrpLauncherUtils.initAgentSources(qSim, context, optimizer, actionCreator);

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
            optimizerConfig.scheduler.setDelaySpeedupStats(delaySpeedupStats);
        }

        RunningVehicleRegister rvr = new RunningVehicleRegister();
        events.addHandler(rvr);

        if (otfVis) { // OFTVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, false, ColoringScheme.taxicab);
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

        if (cacheStats != null) {
            cacheStats.updateStats(routerWithCache);
        }
    }


    private TaxiOptimizerConfiguration createOptimizerConfiguration()
    {
        TaxiSchedulerParams params = new TaxiSchedulerParams(destinationKnown, pickupDuration,
                dropoffDuration);
        TaxiScheduler scheduler = new TaxiScheduler(context, pathCalculator, params);

        VehicleRequestPathFinder vrpFinder = new VehicleRequestPathFinder(pathCalculator, scheduler);

        return new TaxiOptimizerConfiguration(context, pathCalculator, scheduler, vrpFinder,
                algorithmConfig.goal, dirName);
    }


    /*package*/void generateOutput()
    {
        PrintWriter pw = new PrintWriter(System.out);
        pw.println("m\t" + context.getVrpData().getVehicles().size());
        pw.println("n\t" + context.getVrpData().getRequests().size());
        pw.println(TaxiStats.HEADER);
        TaxiStats stats = new TaxiStatsCalculator().calculateStats(context.getVrpData().getVehicles());
        pw.println(stats);
        pw.flush();

        if (vrpOutFiles) {
            new Schedules2GIS(context.getVrpData().getVehicles(),
                    TransformationFactory.WGS84_UTM33N).write(vrpOutDirName);
        }

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(TaxiScheduleChartUtils.chartSchedule(context.getVrpData()
                .getVehicles()));

        if (outHistogram) {
            VrpLauncherUtils.writeHistograms(legHistogram, histogramOutDirName);
        }
    }


    public static void main(String... args)
        throws IOException
    {
        TaxiLauncher launcher;
        if (args.length == 0) {
            launcher = new TaxiLauncher();
        }
        else if (args.length == 1) {
            launcher = new TaxiLauncher(args[0]);
        }
        else {
            throw new RuntimeException();
        }

        launcher.initVrpPathCalculator();
        launcher.go(false);
        launcher.generateOutput();
    }
}
