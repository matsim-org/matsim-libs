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

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.*;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.extensions.taxi.TaxiUtils;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.*;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import playground.jbischoff.taxi.optimizer.rank.NOSRankTaxiOptimizer;
import playground.michalm.taxi.*;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.data.file.*;
import playground.michalm.taxi.scheduler.*;
import playground.michalm.taxi.util.stats.*;
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


    /*package*/final boolean otfVis;

    /*package*/final boolean writeSimEvents;
    /*package*/final String eventsFileName;

    /*package*/final Scenario scenario;

    /*package*/MatsimVrpContext context;

    private String electricStatsDir;
    /*package*/List<String> waitList;


    /*package*/JbSingleIterOnlineDvrpLauncher()
    {


        dirName = "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/subfleets_v7/";

        plansFileName = dirName + "plans/plans4to2_1.0.xml.gz";

        taxisFileName = dirName + "taxis4to4_EV0.0.xml";

        changeEventsFilename = dirName + "changeevents_min1.xml";
        eventsFileName = dirName + "../2kW.15.1000/2kW.15.1000.events.xml.gz";
        netFileName = dirName + "berlin_brb.xml";

        ////////////////////////////////////////////////////////         

        electricStatsDir = dirName + "/test/";

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

        writeSimEvents = true;
        waitList = new ArrayList<String>();

        scenario = VrpLauncherUtils.initScenario(netFileName, plansFileName, changeEventsFilename, 15 * 60, 30 * 15);

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

        if (scenario == null)
            System.out.println("scen");
        if (eventsFileName == null)
            System.out.println("eventsFileName");
        if (ranksFileName == null)
            System.out.println("ranksFileName");

        TravelTime travelTime = VrpLauncherUtils.initTravelTimeCalculatorFromEvents(scenario,
                eventsFileName, 900).getLinkTravelTimes();

        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);

        ETaxiData vrpData = new ETaxiData();
        new ETaxiReader(scenario, vrpData).parse(taxisFileName);
        new TaxiRankReader(scenario, vrpData).parse(ranksFileName);

        contextImpl.setVrpData(vrpData);

        double pickupDuration = 120;
        double dropoffDuration = 60;
        TaxiSchedulerParams params = new TaxiSchedulerParams(false, false, pickupDuration, dropoffDuration, 1);

        NOSRankTaxiOptimizer optimizer = NOSRankTaxiOptimizer.createNOSRankTaxiOptimizer(context,
                params,travelTime , travelDisutility, dirName);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
        contextImpl.setMobsimTimer(qSim.getSimTimer());

        qSim.addQueueSimulationListeners(optimizer);

        ElectroCabLaunchUtils olutils = new ElectroCabLaunchUtils();
        olutils.initVrpSimEngine(qSim, context, optimizer);

        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(
                TaxiUtils.TAXI_MODE, new TaxiRequestCreator(), optimizer, context, qSim);

        VrpLauncherUtils.initAgentSources(qSim, context, optimizer, new TaxiActionCreator(
                passengerEngine, VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer()),
                pickupDuration));

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
        for (Request r : context.getVrpData().getRequests().values()) {
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
        TaxiStats stats = new TaxiStatsCalculator(context.getVrpData().getVehicles().values()).getStats();
        pw.println(stats);
        pw.flush();
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
