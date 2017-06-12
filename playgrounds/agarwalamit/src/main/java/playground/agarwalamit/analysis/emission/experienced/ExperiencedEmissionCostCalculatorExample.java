/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.emission.experienced;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.MapUtils;
import playground.kai.usecases.combinedEventsReader.CombinedMatsimEventsReader;
import playground.vsp.airPollution.exposure.EmissionResponsibilityCostModule;
import playground.vsp.airPollution.exposure.GridTools;
import playground.vsp.airPollution.exposure.IntervalHandler;
import playground.vsp.airPollution.exposure.ResponsibilityGridTools;

/**
 * Created by amit on 15.05.17.
 */


public class ExperiencedEmissionCostCalculatorExample {

    public static void main (String args []) {

        // munich CNE specific data

        final Integer noOfXCells = 270;
        final Integer noOfYCells = 208;
        final double xMin = 4452550.;
        final double xMax = 4479550.;
        final double yMin = 5324955.;
        final double yMax = 5345755.;

//		final Integer noOfXCells = 160;
//		final Integer noOfYCells = 120;
//		final double xMin = 4452550.25;
//		final double xMax = 4479483.33;
//		final double yMin = 5324955.00;
//		final double yMax = 5345696.81;

        final Double timeBinSize = 3600.;
        final int noOfTimeBins = 30;

        // munich
        String dir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output/";
        String outFile = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output/airPolluationExposureCosts_cne.txt";

        // munich
        String [] cases = {
                "output_run0_muc_bc","output_run0b_muc_bc"
                ,"output_run1_muc_c_QBPV3","output_run1b_muc_c_QBPV3"
                ,"output_run2_muc_c_QBPV9","output_run2b_muc_c_QBPV9"
                ,"output_run3_muc_c_DecongestionPID","output_run3b_muc_c_DecongestionPID"
                ,"output_run3_muc_c_DecongestionPID","output_run3b_muc_c_DecongestionPID"
                ,"output_run3-BB_muc_c_DecongestionBangBang","output_run3b-BB_muc_c_DecongestionBangBang"
                ,"output_run4_muc_cne_DecongestionPID","output_run4b_muc_cne_DecongestionPID"
                ,"output_run4-BB_muc_cne_DecongestionBangBang","output_run4b-BB_muc_cne_DecongestionBangBang"
                ,"output_run5_muc_cne_QBPV3","output_run5b_muc_cne_QBPV3"
                ,"output_run6_muc_cne_QBPV9","output_run6b_muc_cne_QBPV9"
                ,"output_run7_muc_n","output_run7b_muc_n"
                ,"output_run8_muc_e","output_run8b_muc_e"
        };
        int [] its = {1500};

        try(BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
            writer.write("case \t itNr \t costsInEur \t tollValuesEUR \n");

            for(String str : cases) {
                for(int itr : its) {
                    String networkFile = dir+str+"/output_network.xml.gz";
                    String configFile = dir+str+"/output_config.xml.gz";
                    String eventsFile = dir + str + "/ITERS/it." + itr + "/" + itr + ".events.xml.gz";

                    if(! new File(eventsFile).exists() || ! new File(networkFile).exists() || ! new File(configFile).exists() ) {
                        continue;
                    }

                    double simulationEndtime = LoadMyScenarios.getSimulationEndTime(configFile);

                    GridTools gt = new GridTools(LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork().getLinks(), xMin, xMax, yMin, yMax, noOfXCells, noOfYCells);
                    IntervalHandler intervalHandler = new IntervalHandler(timeBinSize, simulationEndtime, gt);

                    final Map<Id<Person>, Double> person2toll = new HashMap<>();
                    EventsManager eventsManager = EventsUtils.createEventsManager();
                    eventsManager.addHandler(intervalHandler);
                    eventsManager.addHandler(new PersonMoneyEventHandler() {
                        @Override
                        public void handleEvent(PersonMoneyEvent event) {
                            if(person2toll.containsKey(event.getPersonId())) {
                                person2toll.put(event.getPersonId(), person2toll.get(event.getPersonId()) + event.getAmount());
                            } else {
                                person2toll.put(event.getPersonId(), event.getAmount());
                            }
                        }
                        @Override
                        public void reset(int iteration) {

                        }
                    });
                    new MatsimEventsReader(eventsManager).readFile(eventsFile);

                    ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, gt);
                    rgt.resetAndcaluculateRelativeDurationFactors(intervalHandler.getDuration());

                    EmissionsConfigGroup emissionsConfigGroup  = new EmissionsConfigGroup();
                    emissionsConfigGroup.setConsideringCO2Costs(true);
                    emissionsConfigGroup.setEmissionCostMultiplicationFactor(1.);

                    EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule(emissionsConfigGroup, rgt);
                    ExperiencedEmissionCostHandler handler = new ExperiencedEmissionCostHandler(emissionCostModule, new MunichPersonFilter(),simulationEndtime, 1);

                    EventsManager events = EventsUtils.createEventsManager();
                    events.addHandler(handler);
                    CombinedMatsimEventsReader reader = new CombinedMatsimEventsReader(events);
                    reader.readFile(eventsFile);

                    handler.getUserGroup2TotalEmissionCosts().entrySet().forEach(e -> System.out.println(e.getKey()+"\t"+e.getValue()));
                    writer.write(str+"\t"+itr+"\t"+ MapUtils.doubleValueSum(handler.getUserGroup2TotalEmissionCosts())+"\t");

                    writer.write(MapUtils.doubleValueSum(person2toll)+"\n");

                    // writing time bin 2 costs
                    BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(dir+"timeBin2AirPollutionExposureCosts_"+str+".txt");

                    Map<Double, Double> time2costs = handler.getTimeBin2TotalCosts();
                    bufferedWriter.write("timeBin\tairPollutionExposureCostsEUR\n");
                    for(Double d : time2costs.keySet()) {
                        bufferedWriter.write(d+"\t"+time2costs.get(d)+"\n");
                    }
                    bufferedWriter.close();
                }
            }
            writer.close();
        } catch(IOException e) {
            throw new RuntimeException("Data is not written.");
        }
    }

}
