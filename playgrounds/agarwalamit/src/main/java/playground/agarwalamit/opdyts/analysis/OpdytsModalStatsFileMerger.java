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

package playground.agarwalamit.opdyts.analysis;

import java.io.File;
import playground.agarwalamit.analysis.FileMerger;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 11.06.17.
 */

public class OpdytsModalStatsFileMerger {

    public static void main(String[] args) {
        double [] ascTrialsMotorbike = {-3.0, -2.5, -2.0, -1.5, -1.0, -0.5, 0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0};
        double [] ascTrialsBike = { -1.0, -0.5, 0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0};
        String fileDir = FileUtils.RUNS_SVN+"/opdyts/patna/output_networkModes/ascAnalysis/";
        String outFile = fileDir + "/mergedOpdytsStatsFile.txt";

        FileMerger opdytsModalStatsFileMerger = new FileMerger(OpdytsModalStatsControlerListener.OPDYTS_STATS_LABEL_STARTER);
        opdytsModalStatsFileMerger.mergeTo(outFile);

        for (double ascBike : ascTrialsBike) {
            for (double ascMotorbike : ascTrialsMotorbike) {
                String file = fileDir + "/bikeASC"+ascBike+"_motorbikeASC"+ascMotorbike+"/"+OpdytsModalStatsControlerListener.OPDYTS_STATS_FILE_NAME+".txt";
                if (new File(file).exists()) opdytsModalStatsFileMerger.readAndMerge(file);
            }
        }
        opdytsModalStatsFileMerger.finish();
    }
}
