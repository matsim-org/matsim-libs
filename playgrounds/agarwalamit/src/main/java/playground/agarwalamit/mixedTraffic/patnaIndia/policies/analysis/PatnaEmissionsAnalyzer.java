/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.emission.EmissionUtilsExtended;
import playground.agarwalamit.analysis.emission.filtering.FilteredEmissionPersonEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.MapUtils;

/**
 * Created by amit on 23/12/2016.
 */


public class PatnaEmissionsAnalyzer {

    public static void main(String[] args) {
        String policyCase = "bau";
        String emissionEventsFile = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/"+policyCase+"/output_emissions_events.xml.gz";
        String eventsFile = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/"+policyCase+"/output_events.xml.gz";
        String outEmissionFile = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/analysis/emissions_"+policyCase+".txt";

        new PatnaEmissionsAnalyzer().run(emissionEventsFile, eventsFile, outEmissionFile);
    }

    private void run(final String emissionEventsFile, final String eventsFile, final String outFile){

        FilteredEmissionPersonEventHandler emissionPersonEventHandler = new FilteredEmissionPersonEventHandler(
                PatnaPersonFilter.PatnaUserGroup.urban.toString(), new PatnaPersonFilter());
////        FilteredColdEmissionHandler emissionPersonEventHandler = new FilteredColdEmissionHandler(30*3600.0, 1);

//        EmissionsPerPersonColdEventHandler emissionPersonEventHandler = new EmissionsPerPersonColdEventHandler();

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(emissionPersonEventHandler);
        events.addHandler(new Vehicle2DriverEventHandler());

        EmissionEventsReader emissionEventsReader = new EmissionEventsReader(events);
        MatsimEventsReader eventsReader = new MatsimEventsReader(events);

//        eventsReader.readFile(eventsFile);
        emissionEventsReader.readFile(emissionEventsFile);

        EmissionUtilsExtended emissionUtilsExtended = new EmissionUtilsExtended();

        Map<String, Double> coldEmissions = emissionUtilsExtended.getTotalColdEmissions(emissionPersonEventHandler.getPersonId2ColdEmissions());
        Map<String, Double> warmEmissions = emissionUtilsExtended.getTotalWarmEmissions(emissionPersonEventHandler.getPersonId2WarmEmissions());

        Map<String, Double> totalEmissions = MapUtils.addMaps(coldEmissions, warmEmissions);

        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
        try {
            writer.write("pollutant \t valueInGm \n");
            for (String str : totalEmissions.keySet()) {
                writer.write(str+"\t"+totalEmissions.get(str)+"\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }


    }
}
