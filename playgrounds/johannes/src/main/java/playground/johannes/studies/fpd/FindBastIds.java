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
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 */
public class FindBastIds {

    private static final Logger logger = Logger.getLogger(FindBastIds.class);

    public static void main(String args[]) throws IOException {
        String countsInFile = "/Volumes/johannes/sge/prj/fpd/data/counts/counts.2014.net20140909.5.24h.xml";
        String countsOutFile = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/countIds.xml";
        String mappingFile = "/Volumes/johannes/sge/prj/fpd/data/counts/counts2014-directions.csv";


        Counts<Link> counts = new Counts();
        MatsimCountsReader countsReader = new MatsimCountsReader(counts);
        countsReader.readFile(countsInFile);

        BufferedReader txtReader = new BufferedReader(new FileReader(mappingFile));
        String line = txtReader.readLine();

        Map<String, Record> records = new HashMap<>();

        while((line = txtReader.readLine()) != null) {
            String[] tokens = line.split("\t");
            String id = tokens[1];
            String name = tokens[2];
            double kfzR1 = parseDouble(tokens[15]);
            double kfzR2 = parseDouble(tokens[16]);
            double svR1 = parseDouble(tokens[18]);
            double svR2 = parseDouble(tokens[19]);

            Record record = new Record();
            record.id = id;
            record.name = name;
            record.pkwR1 = kfzR1 - svR1;
            record.pkwR2 = kfzR2 - svR2;

            if(records.containsKey(record.name)) {
                logger.warn(String.format("Entry for %s already exists - removing both.", record.name));
                records.remove(record.name);
            } else {
                records.put(record.name, record);
            }
        }

        Set<Count> remove = new HashSet<>();

        for(Count<Link> count : counts.getCounts().values()) {
            Record record = records.get(count.getCsId());
            if(record != null) {
                double pkwR1_h = record.pkwR1/24.0;
                double pkwR2_h = record.pkwR2/24.0;

                double diffR1 = Math.abs(pkwR1_h - count.getVolume(1).getValue());
                double diffR2 = Math.abs(pkwR2_h - count.getVolume(1).getValue());

                if(diffR1 < diffR2) {
                    if(diffR1 > 10) {
                        logger.warn(String.format("Difference %s is greater than 10.", diffR1));
                    }
                    count.setCsId(record.id + "_R1");
                } else {
                    if(diffR2 > 10) {
                        logger.warn(String.format("Difference %s is greater than 10.", diffR2));
                    }
                    count.setCsId(record.id + "_R2");
                }
            } else {
                logger.warn(String.format("No count station with name %s found.", count.getCsId()));
                remove.add(count);
            }
        }

        for(Count<Link> count : remove) {
            counts.getCounts().remove(count.getLocId());
        }

        CountsWriter writer = new CountsWriter(counts);
        writer.write(countsOutFile);
    }

    private static double parseDouble(String text) {
        if(text.isEmpty()) return 0.0;
        else return Double.parseDouble(text);
    }

    private static class Record {

        private String id;

        private String name;

        private double pkwR1;

        private double pkwR2;

    }
}
