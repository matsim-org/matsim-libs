/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package playground.vsp.analysis.modules.pt.stop2stop;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.MatsimVehicleReader;

/**
 * Example runner for offline (after simulation has finished) usage of {@link PtStop2StopAnalysis}.
 */
public class RunPtStop2StopOffline {
    private static final Logger log = Logger.getLogger(RunPtStop2StopOffline.class);

    public static void main(String[] args) {
        String eventsFile = "/home/gregor/tmp/i364/i364.output_events.xml.gz";
        String transitVehiclesFile = "/home/gregor/tmp/i364/i364.output_transitVehicles.xml.gz";
        String outputCsvByDeparture = "/home/gregor/tmp/i364/i364.pt_stop2stop_departures.csv.gz";
        String outputCsvByLine = "/home/gregor/tmp/i364/i364.pt_stop2stop_lines.csv.gz";
//        String eventsFile = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/output-Vu-BC/Vu-BC.output_events.xml.gz";
//        String transitVehiclesFile = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/output-Vu-BC/Vu-BC.output_transitVehicles.xml.gz";
//        String outputCsvByDeparture = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/output-Vu-BC/Vu-BC.pt_stop2stop_departures.csv.gz";
//        String outputCsvByLine = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/output-Vu-BC/Vu-BC.pt_stop2stop_lines.csv.gz";
//        String eventsFile = "/home/gregor/git/runs-svn/capetown-minibuses/output-minibus-wo-transit/100pct/2019-02-05_100pct_rest0/output_events.xml.gz";
//        String transitVehiclesFile = "/home/gregor/git/runs-svn/capetown-minibuses/output-minibus-wo-transit/100pct/2019-02-05_100pct_rest0/output_transitVehicles.xml.gz";
//        String outputCsvByDeparture = "/home/gregor/git/runs-svn/capetown-minibuses/output-minibus-wo-transit/100pct/2019-02-05_100pct_rest0/pt_stop2stop_departures.csv.gz";
//        String outputCsvByLine = "/home/gregor/git/runs-svn/capetown-minibuses/output-minibus-wo-transit/100pct/2019-02-05_100pct_rest0/pt_stop2stop_lines.csv.gz";
        String sep = ",";
        String sep2 = ";";

        Scenario dummyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimVehicleReader transitVehicleReader = new MatsimVehicleReader(dummyScenario.getTransitVehicles());
        transitVehicleReader.readFile(transitVehiclesFile);

        EventsManager eventsManager = EventsUtils.createEventsManager();
        PtStop2StopAnalysis ptStop2StopAnalysis = new PtStop2StopAnalysis(dummyScenario.getTransitVehicles());
        eventsManager.addHandler(ptStop2StopAnalysis);
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsManager.initProcessing();
        eventsReader.readFile(eventsFile);
        eventsManager.finishProcessing();

        ptStop2StopAnalysis.writeStop2StopEntriesByDepartureCsv(outputCsvByDeparture, sep, sep2);
        log.info("ptStop2StopAnalysis.writeStop2StopEntriesByDepartureCsv done");
        //PtStop2StopAnalysis2Shp.writePtStop2StopAnalysisByTransitLine2ShpFile(ptStop2StopAnalysis.getStop2StopEntriesByDeparture(), "", outputCsvByLine, sep);
    }
}
