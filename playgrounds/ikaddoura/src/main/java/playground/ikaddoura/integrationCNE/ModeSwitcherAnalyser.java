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
        String dir = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/";

        // munich
        String [] cases = {
                "output_run0_muc_bc","output_run0b_muc_bc"
//                ,"output_run1_muc_c_QBPV3","output_run1b_muc_c_QBPV3"
//                ,"output_run2_muc_c_QBPV9","output_run2b_muc_c_QBPV9"
//                ,"output_run3_muc_c_DecongestionPID","output_run3b_muc_c_DecongestionPID"
//                ,"output_run3_muc_c_DecongestionPID","output_run3b_muc_c_DecongestionPID"
//                ,"output_run3-BB_muc_c_DecongestionBangBang","output_run3b-BB_muc_c_DecongestionBangBang"
                ,"output_run4_muc_cne_DecongestionPID","output_run4b_muc_cne_DecongestionPID"
//                ,"output_run4-BB_muc_cne_DecongestionBangBang","output_run4b-BB_muc_cne_DecongestionBangBang"
//                ,"output_run5_muc_cne_QBPV3","output_run5b_muc_cne_QBPV3"
//                ,"output_run6_muc_cne_QBPV9","output_run6b_muc_cne_QBPV9"
//                ,"output_run7_muc_n","output_run7b_muc_n"
//                ,"output_run8_muc_e","output_run8b_muc_e"
        };

        int [] its = {0, 100};

        for (String str : cases) {

            String firstIterationFile = dir + "/" + str + "/ITERS/it." + its[0]+"/"+its[0]+".events.xml.gz";
            String lastIterationFile = dir + "/" + str + "/ITERS/it." + its[1]+"/"+its[1]+".events.xml.gz";

            ModeSwitchersTripTime modeSwitchersTripTime = new ModeSwitchersTripTime();
            modeSwitchersTripTime.processEventsFile(firstIterationFile, lastIterationFile);
            Map<Id<Person>, List<Tuple<String, String>>> personId2ModeSwitches =  modeSwitchersTripTime.getPersonId2ModeSwitcherRetainerTripInfo();

            try(BufferedWriter writer = IOUtils.getBufferedWriter( dir + "/" + str + "/modeSwitchesInfo.txt")) {
                writer.write("personId\tmodeInFirstItr\tmodeInLastIt\n");
                for(Id<Person> personId : personId2ModeSwitches.keySet()) {
                    for (Tuple<String, String> modeSwitch : personId2ModeSwitches.get(personId)) {
                        writer.write(personId+"\t"+modeSwitch.getFirst()+"\t"+modeSwitch.getSecond()+"\n");
                    }
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Data is not read/written. Reason " + e);
            }
        }
    }
}
