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

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.*;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.extensions.taxi.TaxiUtils;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.dvrp.util.gis.Schedules2GIS;
import org.matsim.contrib.dvrp.util.time.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import playground.jbischoff.taxi.optimizer.rank.NOSRankTaxiOptimizer;
import playground.michalm.taxi.*;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.run.TaxiLauncherUtils;
import playground.michalm.taxi.scheduler.*;
import playground.michalm.taxi.util.stats.*;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;
import playground.michalm.util.MovingAgentsRegister;


/**
 * @author jbischoff
 */

/*package*/class JbSingleIterOnlineDvrpLauncher
{
    /*package*/final String dirName;
    /*package*/final String netFileName;
    /*package*/final String changeEventsFilename;

    /*package*/final String plansFileName;
    /*package*/final String taxiCustomersFileName;
    /*package*/final String taxisFileName;
    /*package*/final String ranksFileName;

    /*package*/final boolean vrpOutFiles;
    /*package*/final String vrpOutDirName;

    /*package*/final boolean outHistogram;
    /*package*/final String histogramOutDirName;

    /*package*/final boolean otfVis;

    /*package*/final boolean writeSimEvents;
    /*package*/final String eventsFileName;

    /*package*/final Scenario scenario;

    /*package*/MatsimVrpContext context;
    /*package*/LegHistogram legHistogram;

    private String electricStatsDir;
    /*package*/List<String> waitList;


    /*package*/JbSingleIterOnlineDvrpLauncher()
    {

        // michalm - testing config (may be removed...)////////////////////////////////////

        //        dirName = "D:\\PP-rad\\taxi\\mielec-2-peaks\\joschka\\mielec-2-peaks-new-15-50\\";
        //        plansFileName = dirName + "..\\mielec-2-peaks-new\\output\\ITERS\\it.20\\20.plans.xml.gz";
        //        netFileName = dirName + "..\\mielec-2-peaks-new\\network.xml";
        //        eventsFileName = dirName + "..\\mielec-2-peaks-new\\output\\ITERS\\it.20\\20.events.xml.gz";

        //    	   dirName = "/Users/jb/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/2014_02_basic_scenario_v1/";
        //    	   dirName = "C:\\local_jb\\data\\scenarios\\2014_02_basic_scenario_v1\\";
        //           plansFileName = dirName + "OD_20130417_SCALE_2.0_plans.xml.gz";
        //           taxisFileName = dirName + "taxis-3000.xml";

        //    	   dirName = "C:\\local_jb\\data\\scenarios\\2014_02_basic_scenario_v2\\";
        //    	   dirName = "C:\\local_jb\\data\\scenarios\\2014_05_basic_scenario_v3\\";
        dirName = "C:\\local_jb\\data\\scenarios\\2014_10_basic_scenario_v4\\";

        //           plansFileName = dirName + "1.0plans4to3.xml";
        //           plansFileName = dirName + "plans4to3.xml";
        plansFileName = dirName + "plans/plans4to3_1.0.xml.gz";
        //           plansFileName = dirName + "1.5plans4to3.xml.gz";
        //    	   	plansFileName = dirName + "1.5plans4to3.xml.gz";

        //           plansFileName = dirName + "2.0plans4to4.xml.gz";

        //           taxisFileName = dirName + "taxis4to4_EV0.5.xml";
        //           taxisFileName = dirName + "taxis4to4_EV0.6.xml";
        //           taxisFileName = dirName + "taxis4to4_EV0.7.xml";
        //           taxisFileName = dirName + "taxis4to4_EV0.8.xml";
        //           taxisFileName = dirName + "taxis4to4_EV0.9.xml";
        //             taxisFileName = dirName + "taxis4to4_EV1.0.xml";
        //             taxisFileName = dirName + "taxis4to4_EVonezone.xml";
        taxisFileName = dirName + "taxis4to4_EV0.0.xml";
        //           taxisFileName = dirName + "oldtaxis4to4.xml";

        changeEventsFilename = dirName + "changeevents_min.xml";
        eventsFileName = dirName + "2kW.15.1000.events.xml.gz";
        netFileName = dirName + "berlin_brb.xml";

        ////////////////////////////////////////////////////////         

        electricStatsDir = dirName + "test/";

        //        plansFileName = dirName + "20.plans.xml.gz";
        //
        //        taxiCustomersFileName = dirName + "taxiCustomers_05_pc.txt";
        // taxiCustomersFileName = dirName + "taxiCustomers_10_pc.txt";
        taxiCustomersFileName = dirName + "taxiCustomers_15_pc.txt";

        ranksFileName = dirName + "berlin_ranks.xml";

        // reqIdToVehIdFileName = dirName + "reqIdToVehId";

        //        eventsFileName = dirName + "20.events.xml.gz";

        otfVis = !true;

        vrpOutFiles = !true;
        vrpOutDirName = dirName + "vrp_output";

        outHistogram = true;
        histogramOutDirName = electricStatsDir + "histograms";

        writeSimEvents = true;
        waitList = new ArrayList<String>();

        scenario = VrpLauncherUtils.initScenario(netFileName, plansFileName, changeEventsFilename);

        //        List<String> taxiCustomerIds;
        //        taxiCustomerIds = ODDemandGenerator.readTaxiCustomerIds(taxiCustomersFileName);

        //        VrpLauncherUtils.convertLegModes(taxiCustomerIds, TaxiRequestCreator.MODE, scenario);
    }


    /**
     * Can be called several times (1 call == 1 simulation)
     */
    /*package*/void go(int run)
    {
        MatsimVrpContextImpl contextImpl = new MatsimVrpContextImpl();
        this.context = contextImpl;

        contextImpl.setScenario(scenario);

        File f = new File(electricStatsDir);
        f.mkdirs();

        //        TravelTimeSource ttimeSource = TravelTimeSource.FREE_FLOW_SPEED;
        TravelTimeSource ttimeSource = TravelTimeSource.EVENTS;

        TravelDisutilitySource tdisSource = TravelDisutilitySource.TIME;

        if (scenario == null)
            System.out.println("scen");
        if (ttimeSource == null)
            System.out.println("ttsource");
        if (tdisSource == null)
            System.out.println("tcostSource");
        if (eventsFileName == null)
            System.out.println("eventsFileName");
        if (ranksFileName == null)
            System.out.println("ranksFileName");

        TravelTime travelTime = VrpLauncherUtils.initTravelTimeCalculatorFromEvents(scenario,
                eventsFileName, 900).getLinkTravelTimes();

        TravelDisutility travelDisutility = VrpLauncherUtils.initTravelDisutility(tdisSource,
                travelTime);

        LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), travelDisutility,
                travelTime);

        LeastCostPathCalculatorWithCache routerWithCache = new LeastCostPathCalculatorWithCache(
                router, new TimeDiscretizer(31 * 4, 15 * 60, false));

        VrpPathCalculator calculator = new VrpPathCalculatorImpl(routerWithCache, travelTime,
                travelDisutility);

        TaxiData vrpData = TaxiLauncherUtils.initTaxiData(scenario, taxisFileName, ranksFileName);
        contextImpl.setVrpData(vrpData);

        double pickupDuration = 120;
        double dropoffDuration = 60;
        TaxiSchedulerParams params = new TaxiSchedulerParams(false, pickupDuration, dropoffDuration);

        NOSRankTaxiOptimizer optimizer = NOSRankTaxiOptimizer.createNOSRankTaxiOptimizer(context,
                calculator, params, tdisSource, dirName);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
        contextImpl.setMobsimTimer(qSim.getSimTimer());

        qSim.addQueueSimulationListeners(optimizer);

        ElectroCabLaunchUtils olutils = new ElectroCabLaunchUtils();
        olutils.initVrpSimEngine(qSim, context, optimizer);

        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(
                TaxiUtils.TAXI_MODE, new TaxiRequestCreator(), optimizer, context, qSim);

        VrpLauncherUtils.initAgentSources(qSim, context, optimizer, new TaxiActionCreator(
                passengerEngine, VrpLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR, pickupDuration));

        EventsManager events = qSim.getEventsManager();

        EventWriter eventWriter = null;
        if (writeSimEvents) {
            eventWriter = new EventWriterXML(electricStatsDir + "events.xml.gz");
            events.addHandler(eventWriter);
        }

        MovingAgentsRegister rvr = new MovingAgentsRegister();
        events.addHandler(rvr);

        if (otfVis) { // OFTVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, false, ColoringScheme.taxicab);
        }

        if (outHistogram) {
            events.addHandler(legHistogram = new LegHistogram(300));
        }
        //        qSim.getScenario().getConfig().simulation().setEndTime(86399);
        qSim.run();
        System.out.println("taxiless agents: ");
        for (Id<?> id : rvr.getMovingAgentIds()) {
            System.out.println(id.toString());

        }
        events.finishProcessing();

        if (writeSimEvents) {
            eventWriter.closeFile();
        }
        waitList.add(run + "\t" + olutils.writeStatisticsToFiles(electricStatsDir) + "\n");

        // check if all reqs have been served
        for (Request r : context.getVrpData().getRequests()) {
            TaxiRequest tr = (TaxiRequest)r;
            if (tr.getStatus() != TaxiRequestStatus.PERFORMED) {
                //                throw new IllegalStateException();
            }
        }
    }


    /*package*/void generateOutput()
    {
        PrintWriter pw = new PrintWriter(System.out);
        pw.println(TaxiStats.HEADER);
        TaxiStats stats = new TaxiStatsCalculator(context.getVrpData().getVehicles()).getStats();
        pw.println(stats);
        pw.flush();

        if (vrpOutFiles) {
            new Schedules2GIS(context.getVrpData().getVehicles(),
                    TransformationFactory.WGS84_UTM33N).write(vrpOutDirName);
        }

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        //        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));

        //        try {
        ////            ChartUtils.saveAsPDF(
        ////                    ScheduleChartUtils.chartSchedule(context.getVrpData().getVehicles()),
        ////                    electricStatsDir + "taxiSchedules", 2048, 1546);
        //        }
        //        catch (IOException e) {
        //            // TODO Auto-generated catch block
        //            e.printStackTrace();
        //        }
        if (outHistogram) {
            VrpLauncherUtils.writeHistograms(legHistogram, histogramOutDirName);
        }
    }


    public static void main(String... args)
    {

        JbSingleIterOnlineDvrpLauncher launcher;
        launcher = new JbSingleIterOnlineDvrpLauncher();
        //        launcher.goIncreasedDemand(11);

        launcher.go(0);
        launcher.generateOutput();
    }
}
