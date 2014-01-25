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
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.dvrp.util.chart.ScheduleChartUtils;
import org.matsim.contrib.dvrp.util.gis.Schedules2GIS;
import org.matsim.contrib.dvrp.vrpagent.VrpDynLegs;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ricardoFaria2012.EnergyConsumptionModelRicardoFaria2012;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import pl.poznan.put.util.jfreechart.ChartUtils;
import playground.jbischoff.taxi.optimizer.rank.NOSRankTaxiOptimizer;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.taxi.*;
import playground.michalm.taxi.model.*;
import playground.michalm.taxi.model.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.immediaterequest.ImmediateRequestTaxiOptimizer.Params;
import playground.michalm.taxi.run.TaxiLauncherUtils;
import playground.michalm.util.RunningVehicleRegister;


/**
 * @author jbischoff
 */

/*package*/class JbSingleIterOnlineDvrpLauncher
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

    /*package*/MatsimVrpData data;
    /*package*/LegHistogram legHistogram;

    /*package*/TaxiDelaySpeedupStats delaySpeedupStats;
    private String electricStatsDir;
    /*package*/List<String> waitList;


    /*package*/JbSingleIterOnlineDvrpLauncher()
    {
        //    	dirName = "Z:\\WinHome\\Docs\\maciejewski\\jbtest\\";
        //    	dirName = "Z:\\WinHome\\Docs\\svn-checkouts\\jbischoff\\jbmielec\\";
        dirName = "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\";
        netFileName = dirName + "network.xml";

        // michalm - testing config (may be removed...)
        //        dirName = "D:\\PP-rad\\taxi\\mielec-2-peaks\\2013_12\\mielec-2-peaks-new-15-50\\";
        //        plansFileName = dirName + "..\\mielec-2-peaks-new\\output\\ITERS\\it.20\\20.plans.xml.gz";
        //        netFileName = dirName + "..\\mielec-2-peaks-new\\network.xml";
        //        taxiCustomersFileName = dirName + "taxiCustomers_15_pc.txt";
        //        eventsFileName = dirName + "..\\mielec-2-peaks-new\\output\\ITERS\\it.20\\20.events.xml.gz";

        //        electricStatsDir = dirName +"electric_nodepots\\";
        //        electricStatsDir = dirName +"electric_idledepots\\";
        //        electricStatsDir = dirName +"electric_depots\\";
        //        electricStatsDir = dirName +"gas_nodepots\\";
        //      electricStatsDir = dirName +"gas_idledepots\\";
        //      electricStatsDir = dirName +"gas_depots\\";
        //        electricStatsDir = dirName +"modifiedDispatch_SL\\";
        //        electricStatsDir = dirName +"1charger\\";
        electricStatsDir = dirName + "1slow_fifo\\";

        plansFileName = dirName + "20.plans.xml.gz";

        taxiCustomersFileName = dirName + "taxiCustomers_05_pc.txt";

        // taxiCustomersFileName = dirName + "taxiCustomers_10_pc.txt";

        ranksFileName = dirName + "depots-5_taxis-50.xml";
        //         depotsFileName = dirName + "depots-5_taxis-100.xml";

        // reqIdToVehIdFileName = dirName + "reqIdToVehId";

        eventsFileName = dirName + "20.events.xml.gz";

        otfVis = !true;

        vrpOutFiles = !true;
        vrpOutDirName = dirName + "vrp_output";

        outHistogram = true;
        histogramOutDirName = electricStatsDir + "histograms";

        writeSimEvents = true;
        waitList = new ArrayList<String>();

        scenario = VrpLauncherUtils.initScenario(netFileName, plansFileName);

        List<String> taxiCustomerIds;
        try {
            taxiCustomerIds = ODDemandGenerator.readTaxiCustomerIds(taxiCustomersFileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        VrpLauncherUtils.convertLegModes(taxiCustomerIds, TaxiRequestCreator.MODE, scenario);
    }


    /**
     * Can be called several times (1 call == 1 simulation)
     */
    /*package*/void go(int run)
    {
        File f = new File(electricStatsDir);
        f.mkdirs();

        TravelTimeSource ttimeSource = TravelTimeSource.FREE_FLOW_SPEED;
        TravelDisutilitySource tdisSource = TravelDisutilitySource.DISTANCE;

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

        TravelTime travelTime = VrpLauncherUtils.initTravelTime(scenario, null, ttimeSource,
                eventsFileName);

        TravelDisutility travelDisutility = VrpLauncherUtils.initTravelDisutility(tdisSource,
                travelTime);

        VrpPathCalculator calculator = VrpLauncherUtils.initVrpPathCalculator(scenario,
                ttimeSource, travelTime, travelDisutility);

        EnergyConsumptionModel ecm = new EnergyConsumptionModelRicardoFaria2012();

        TaxiData vrpData = TaxiLauncherUtils.initTaxiData(scenario, ranksFileName, ecm);

        Params params = new Params(true, false, 120, 60);

        NOSRankTaxiOptimizer optimizer = NOSRankTaxiOptimizer.createNOSRankTaxiOptimizer(vrpData,
                calculator, params, true);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);

        data = new MatsimVrpData(vrpData, scenario, qSim.getSimTimer());

        ElectroCabLaunchUtils olutils = new ElectroCabLaunchUtils();
        olutils.initVrpSimEngine(qSim, data, optimizer);

        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(
                TaxiRequestCreator.MODE, new TaxiRequestCreator(), optimizer, data, qSim);

        VrpLauncherUtils.initAgentSources(qSim, data, optimizer, new TaxiActionCreator(
                passengerEngine, VrpDynLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR));

        EventsManager events = qSim.getEventsManager();

        EventWriter eventWriter = null;
        if (writeSimEvents) {
            eventWriter = new EventWriterXML(electricStatsDir + "events.xml.gz");
            events.addHandler(eventWriter);
        }

        RunningVehicleRegister rvr = new RunningVehicleRegister();
        events.addHandler(rvr);

        if (otfVis) { // OFTVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, false);
        }

        if (outHistogram) {
            events.addHandler(legHistogram = new LegHistogram(300));
        }
        //        qSim.getScenario().getConfig().simulation().setEndTime(86399);
        qSim.run();

        events.finishProcessing();

        if (writeSimEvents) {
            eventWriter.closeFile();
        }
        waitList.add(run + "\t" + olutils.writeStatisticsToFiles(electricStatsDir) + "\n");

        // check if all reqs have been served
        for (Request r : data.getVrpData().getRequests()) {
            TaxiRequest tr = (TaxiRequest)r;
            if (tr.getStatus() != TaxiRequestStatus.PERFORMED) {
                throw new IllegalStateException();
            }
        }
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
        //        ChartUtils.showFrame(ScheduleChartUtils.chartSchedule(data.getVrpData()));

        try {
            ChartUtils.saveAsPDF(ScheduleChartUtils.chartSchedule(data.getVrpData()),
                    electricStatsDir + "taxiSchedules", 2048, 1546);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
