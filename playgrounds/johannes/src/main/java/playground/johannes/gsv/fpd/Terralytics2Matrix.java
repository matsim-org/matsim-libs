/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.fpd;

import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class Terralytics2Matrix {

    public static void main(String[] args) throws IOException {
        String outdir = "/home/johannes/gsv/fpd/telefonica/matrixv2";

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dayformatter = DateTimeFormat.forPattern("dd");
        DateTimeFormatter hourFormatter = DateTimeFormat.forPattern("HH");

        Map<String, KeyMatrix> map = new HashMap<String, KeyMatrix>();
        BufferedReader reader = new BufferedReader(new FileReader("/mnt/cifs/B-drive/C_Vertrieb/2013_01_01_Floating Phone Data/04 Telefonica/Matrizen/od_start_daily.csv"));
//        BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/fpd/telefonica/Telefonica_Pilotmatrix_Frankfurt-Berlin/od_201503_hourly_start.csv"));

        String line = reader.readLine();

        TDoubleDoubleHashMap hourHist = new TDoubleDoubleHashMap();

        while((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");

            String i = tokens[0].substring(1, tokens[0].length() - 1);
            String j = tokens[2].substring(1, tokens[2].length() - 1);
            String mode = tokens[4].substring(1, tokens[4].length() - 1);

            if(mode.equalsIgnoreCase("IV")) {
                LocalDateTime date = formatter.parseLocalDateTime(tokens[5].substring(1, tokens[5].length() - 1));
                String day = date.toString(dayformatter);
                int hour = date.getHourOfDay();

                double volume = Double.parseDouble(tokens[6].substring(1, tokens[6].length() - 1));

                hourHist.adjustOrPutValue(hour, volume, volume);

                KeyMatrix m = map.get(day);
                if (m == null) {
                    m = new KeyMatrix();
                    map.put(day, m);
                }

                m.add(i, j, volume);
            }
        }

        KeyMatrixXMLWriter writer = new KeyMatrixXMLWriter();
        for(Map.Entry<String, KeyMatrix> entry : map.entrySet()) {
            writer.write(entry.getValue(), String.format("%s/%s.xml", outdir, entry.getKey()));
        }

        StatsWriter.writeHistogram(hourHist, "hour", "trips", String.format("%s/startTime.hist.txt", outdir));
    }
}
