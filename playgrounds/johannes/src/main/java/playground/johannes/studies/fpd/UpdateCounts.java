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

package playground.johannes.studies.fpd;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class UpdateCounts {

    private static final Logger logger = Logger.getLogger(UpdateCounts.class);

    public static void main(String args[]) throws IOException {
        String valuesFile = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/counts.aug-sep.txt";
        String inCountsFile = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/countIds.xml";
        String outCountsFile = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/counts.2015.aug-sep.sv.xml";

        Counts<Link> counts = new Counts<>();
        CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
        reader.readFile(inCountsFile);

        Map<String, Double> pkwCounts = new HashMap<>();
        Map<String, Double> svCounts = new HashMap<>();

        BufferedReader txtReader = new BufferedReader(new FileReader(valuesFile));
        String line = txtReader.readLine();
        while((line = txtReader.readLine()) != null) {
            String tokens[] = line.split(";");
            String id = tokens[0];
            Double pkwR1 = Double.parseDouble(tokens[5]);
            Double pkwR2 = Double.parseDouble(tokens[6]);
            Double svR1 = Double.parseDouble(tokens[3]);
            Double svR2 = Double.parseDouble(tokens[4]);

            pkwCounts.put(id + "_R1", pkwR1 + (svR1/1.5));
            pkwCounts.put(id + "_R2", pkwR2 + (svR2/1.5));

            svCounts.put(id + "_R1", svR1);
            svCounts.put(id + "_R2", svR2);
        }

        int notFound = 0;
        for(Count<Link> count : counts.getCounts().values()) {
            Double val = pkwCounts.get(count.getCsLabel());
            if(val != null && val > 0) {
                for(int h = 1 ; h < 25; h++) {
                    count.getVolumes().get(h).setValue(val/24.0);
                }
            } else {
                notFound++;
//                logger.info(String.format("No values found for %s.", count.getCsId()));
            }

//            for(int h = 1 ; h < 25; h++) {
//                val = count.getVolume(h).getValue();
//                count.getVolumes().get(h).setValue(val/24.0);
//            }
        }

        logger.info(String.format("Cannot update values for %s of %s stations.", notFound, counts.getCounts().size()));

        CountsWriter writer = new CountsWriter(counts);
        writer.write(outCountsFile);
    }
}
