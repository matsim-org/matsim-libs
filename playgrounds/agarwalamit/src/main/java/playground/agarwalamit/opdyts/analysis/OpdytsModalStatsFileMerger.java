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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 11.06.17.
 */

public class OpdytsModalStatsFileMerger {

    // BEGIN_EXAMPLE
    public static void main(String[] args) {

//        double [] ascTrials = {-3.0, -2.5, -2.0, 1.0, 1.5, 2.0, 2.5, 3.0};
        double [] ascTrials = {-3.0, -2.5, -2.0, -1.5, -1.0, -0.5, 0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0};
        String fileDir = FileUtils.RUNS_SVN+"/opdyts/patna/output_networkModes/ascAnalysis/";
        String outFile = fileDir + "/mergedOpdytsStatsFile.txt";

        OpdytsModalStatsFileMerger opdytsModalStatsFileMerger = new OpdytsModalStatsFileMerger();
        opdytsModalStatsFileMerger.mergeTo(outFile);

        for (double ascBike : ascTrials) {
            for (double ascMotorbike : ascTrials) {
                String file = fileDir + "/bikeASC"+ascBike+"_motorbikeASC"+ascMotorbike+"/"+OpdytsModalStatsControlerListener.OPDYTS_STATS_FILE_NAME+".txt";
                opdytsModalStatsFileMerger.readAndMerge(file);
            }
        }
        opdytsModalStatsFileMerger.finish();
    }
    // END_EXAMPLE

    private boolean isHeaderAdded = false;
    private BufferedWriter writer;

    public void mergeTo(final String outFile) {
        writer = IOUtils.getBufferedWriter(outFile);
    }

    public void finish () {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    public void readAndMerge(final String inputFile){
        readAndMerge (IOUtils.getBufferedReader(inputFile));
    }

    public void readAndMerge (final BufferedReader reader) {
        try {
            String line = reader.readLine();
            while (line!= null) {
                if (isHeaderFile(line)) {
                    if (! isHeaderAdded ) {
                        writer.write(line);
                        writer.newLine();
                        isHeaderAdded = true;
                    }
                } else {
                    writer.write(line);
                    writer.newLine();
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    private boolean isHeaderFile ( final String line) {
        return line.startsWith(OpdytsModalStatsControlerListener.OPDYTS_STATS_LABEL_STARTER);
    }

}
