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

package playground.agarwalamit.nemo.demand;

import playground.agarwalamit.utils.FileUtils;
import playground.vsp.demandde.cemdapMATSimCadyts.DemandGeneratorCensus;

/**
 * Created by amit on 22.06.17.
 */

public class NRWDemandGenerator {

    public static void main(String[] args) {

        String sharedSVNDir = FileUtils.SHARED_SVN; // "../../../shared-svn";

        String commuterFileOutgoing1 = sharedSVNDir+"/projects/nemo_mercator/30_Scenario/cemdap_input/pendlerstatistik/051NRW2009Ga.txt";
        String commuterFileOutgoing2 = sharedSVNDir+"/projects/nemo_mercator/30_Scenario/cemdap_input/pendlerstatistik/053NRW2009Ga.txt";
        String commuterFileOutgoing3 = sharedSVNDir+"/projects/nemo_mercator/30_Scenario/cemdap_input/pendlerstatistik/055NRW2009Ga.txt";
        String commuterFileOutgoing4 = sharedSVNDir+"/projects/nemo_mercator/30_Scenario/cemdap_input/pendlerstatistik/057NRW2009Ga.txt";
        String commuterFileOutgoing5 = sharedSVNDir+"/projects/nemo_mercator/30_Scenario/cemdap_input/pendlerstatistik/059NRW2009Ga.txt";

        String censusFile = sharedSVNDir+"/projects/nemo_mercator/30_Scenario/cemdap_input/zensus_2011/Zensus11_Datensatz_Bevoelkerung_NRW.csv";
        String shapeFileLors = sharedSVNDir+"/projects/nemo_mercator/30_Scenario/cemdap_input/shapeFiles/shapeFile_Ruhrgebiet/dvg2gem_ruhrgebiet.shp";
        String outputBase = sharedSVNDir+"/projects/nemo_mercator/30_Scenario/cemdap_output/";

        // Parameters
        int numberOfPlansPerPerson = 5;
        String planningAreaId = ""; // no specific planning Area Id
        double defaultAdultsToEmployeesRatio = 1.23;  // Calibrated based on sum value from Zensus 2011.
        double defaultEmployeesToCommutersRatio = 2.5;  // This is an assumption, oriented on observed values, deliberately chosen slightly too high.
        boolean writeMatsimPlanFiles = true;
        boolean includeChildren = false;

        String[] commuterFilesOutgoing = {commuterFileOutgoing1, commuterFileOutgoing2, commuterFileOutgoing3, commuterFileOutgoing4, commuterFileOutgoing5};

        new DemandGeneratorCensus(commuterFilesOutgoing, censusFile, shapeFileLors, outputBase,	numberOfPlansPerPerson, planningAreaId,
                defaultAdultsToEmployeesRatio, defaultEmployeesToCommutersRatio, writeMatsimPlanFiles, includeChildren);

    }

}
