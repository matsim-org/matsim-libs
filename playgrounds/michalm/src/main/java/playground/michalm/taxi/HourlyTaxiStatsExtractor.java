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

import java.util.Arrays;

import org.matsim.contrib.taxi.util.stats.HourlyTaxiStatsReader;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.core.utils.io.IOUtils;


public class HourlyTaxiStatsExtractor
{
    public static final String[] FLEETS = { "02.2", "04.4", "06.6", "08.8", "11.0" };
    public static final String[] AVS = { "1.0", "1.5", "2.0" };
    public static final int COUNT = FLEETS.length * AVS.length;


    public static String getId(String fleet, String av)
    {
        return fleet + "k_AV" + av;
    }


    public static void main(String[] args)
    {
        String path = "../../../shared-svn/projects/audi_av/papers/03_transport_special_issue/results_0.15fc/";
        int hours = 25;
        int iter = 50;

        String[] header = new String[COUNT];
        String[][] meanWaitTimes = new String[hours][COUNT];
        String[][] p95WaitTimes = new String[hours][COUNT];
        String[][] meanEmptyRatios = new String[hours][COUNT];

        int i = 0;
        for (String fleet : FLEETS) {
            for (String av : AVS) {
                String file = path + getId(fleet, av) + "." + iter + ".taxi_hourly_stats.txt";
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

        try (CompactCSVWriter writer = new CompactCSVWriter(
                IOUtils.getBufferedWriter(path + "hourly_stats_combined.txt"))) {
            writer.writeNext(header);
            writer.writeAll(Arrays.asList(meanWaitTimes));

            writer.writeNext(header);
            writer.writeAll(Arrays.asList(p95WaitTimes));

            writer.writeNext(header);
            writer.writeAll(Arrays.asList(meanEmptyRatios), false);
        }
    }
}
