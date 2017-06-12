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

package playground.ikaddoura.integrationCNE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.modeSwitcherRetainer.ModeSwitchersTripTime;

/**
 * Created by amit on 12.06.17.
 */


public class ModeSwitcherAnalyser {

    public static void main(String[] args) {
        ModeSwitcherAnalyser modeSwitcherAnalyser = new ModeSwitcherAnalyser();
        modeSwitcherAnalyser.analyseForBerlin();
        modeSwitcherAnalyser.analyseForMunich();
    }

    public void analyseForMunich() {
        String dir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/";
//        String dir = FileUtils.RUNS_SVN+"/cne/munich/output-final/";

        String [] cases = {
                "output_run0_muc_bc","output_run0b_muc_bc"
                ,"output_run4_muc_cne_DecongestionPID","output_run4b_muc_cne_DecongestionPID"
        };

        int [] its = {1000, 1500};

        for (String str : cases) {

            String firstIterationFile = dir + "/" + str + "/ITERS/it." + its[0]+"/"+its[0]+".events.xml.gz";
            String lastIterationFile = dir + "/" + str + "/ITERS/it." + its[1]+"/"+its[1]+".events.xml.gz";

            ModeSwitchersTripTime modeSwitchersTripTime = new ModeSwitchersTripTime();
            modeSwitchersTripTime.processEventsFile(firstIterationFile, lastIterationFile);
            Map<Id<Person>, List<Tuple<String, String>>> personId2ModeSwitches =  modeSwitchersTripTime.getPersonId2ModeSwitcherRetainerTripInfo();

            try(BufferedWriter writer = IOUtils.getBufferedWriter( dir + "/" + str + "/modeSwitchesInfo.txt")) {
                writer.write("personId\tmodeInFirstItr\tmodeInLastIt\ttripNumber\n");
                for(Id<Person> personId : personId2ModeSwitches.keySet()) {
                    int tripIndex = 1;
                    for (Tuple<String, String> modeSwitch : personId2ModeSwitches.get(personId)) {
                        writer.write(personId+"\t"+modeSwitch.getFirst()+"\t"+modeSwitch.getSecond()+"\t"+tripIndex+"\n");
                        tripIndex++;
                    }
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Data is not read/written. Reason " + e);
            }
        }
    }

    public void analyseForBerlin() {
        String dir = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct-simpleNetwork/output_selectedPlans_flowCapFactor0.015_randomization/";

        String [] cases = {
                "m_r_output_run0_bln_bc","r_output_run0_bln_bc"
                ,"m_r_output_run4_bln_cne_DecongestionPID","r_output_run4_bln_cne_DecongestionPID"
        };

        int [] its = {0, 100};

        for (String str : cases) {

            String firstIterationFile = dir + "/" + str + "/ITERS/it." + its[0]+"/"+its[0]+".events.xml.gz";
            String lastIterationFile = dir + "/" + str + "/ITERS/it." + its[1]+"/"+its[1]+".events.xml.gz";

            ModeSwitchersTripTime modeSwitchersTripTime = new ModeSwitchersTripTime();
            modeSwitchersTripTime.processEventsFile(firstIterationFile, lastIterationFile);
            Map<Id<Person>, List<Tuple<String, String>>> personId2ModeSwitches =  modeSwitchersTripTime.getPersonId2ModeSwitcherRetainerTripInfo();

            try(BufferedWriter writer = IOUtils.getBufferedWriter( dir + "/" + str + "/modeSwitchesInfo.txt")) {
                writer.write("personId\tmodeInFirstItr\tmodeInLastIt\ttripNumber\n");
                for(Id<Person> personId : personId2ModeSwitches.keySet()) {
                    for (int index = 1; index <= personId2ModeSwitches.get(personId).size() ; index++) {
                        Tuple<String, String> modeSwitch = personId2ModeSwitches.get(personId).get(index);
                        writer.write(personId+"\t"+modeSwitch.getFirst()+"\t"+modeSwitch.getSecond()+"\t"+index+"\n");
                    }
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Data is not read/written. Reason " + e);
            }
        }
    }
}
