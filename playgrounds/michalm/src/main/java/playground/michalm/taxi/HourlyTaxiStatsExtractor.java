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

package playground.michalm.taxi;

import java.io.*;
import java.util.Arrays;

import org.matsim.contrib.taxi.util.stats.HourlyTaxiStatsReader;

import com.opencsv.CSVWriter;


public class HourlyTaxiStatsExtractor
{
    public static void main(String[] args)
    {
        String path = "../../../shared-svn/projects/audi_av/papers/03_transport_special_issue/results/";
        String[] fleets = { "02.2", "04.4", "06.6", "08.8", "11.0" };
        String[] avs = { "1.0", "1.5", "2.0" };
        int count = fleets.length * avs.length;
        int hours = 25;

        String[] header = new String[count];
        String[][] meanWaitTimes = new String[hours][count];
        String[][] p95WaitTimes = new String[hours][count];
        String[][] meanEmptyRatios = new String[hours][count];

        int i = 0;
        for (String fleet : fleets) {
            for (String av : avs) {
                String file = path + fleet + "k_AV" + av + ".30.taxi_hourly_stats.txt";
                HourlyTaxiStatsReader r = new HourlyTaxiStatsReader(file);

                header[i] = fleet + "_" + av;
                for (int h = 0; h < hours; h++) {
                    meanWaitTimes[h][i] = r.getMeanWaitTime(h) + "";
                    p95WaitTimes[h][i] = r.getP95WaitTime(h) + "";
                    meanEmptyRatios[h][i] = r.getMeanEmptyRatio(h) + "";
                }

                i++;
            }
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(path + "hourly_stats_combined.txt"),
                '\t')) {
            writer.writeNext(header);
            writer.writeAll(Arrays.asList(meanWaitTimes), false);

            writer.writeNext(header);
            writer.writeAll(Arrays.asList(p95WaitTimes), false);

            writer.writeNext(header);
            writer.writeAll(Arrays.asList(meanEmptyRatios), false);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
