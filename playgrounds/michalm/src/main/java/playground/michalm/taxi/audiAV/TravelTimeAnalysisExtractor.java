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

package playground.michalm.taxi.audiAV;

import java.util.*;

import org.matsim.contrib.util.*;
import org.matsim.core.utils.io.IOUtils;


public class TravelTimeAnalysisExtractor
{
    @SuppressWarnings("unused")
    private static final int COL_hour = 0;
    private static final int COL_carTTInside = 1;
    private static final int COL_carRidesInside = 2;
    private static final int COL_carTTOutside = 3;
    private static final int COL_carRidesOutside = 4;
    @SuppressWarnings("unused")
    private static final int COL_taxiWait = 5;
    private static final int COL_taxiIVTT = 6;
    @SuppressWarnings("unused")
    private static final int COL_taxiTT = 7;
    private static final int COL_taxiRides = 8;

    private static final String path = "../../../runs-svn/avsim/flowpaper/";
    private static final int hours = 24;

    private final String[] header = new String[TaxiStatsExtractor.COUNT + 2];
    private final String[][] carTTInside = new String[hours + 1][TaxiStatsExtractor.COUNT + 2];
    private final String[][] carRidesInside = new String[hours + 1][TaxiStatsExtractor.COUNT + 2];
    private final String[][] carTTOutside = new String[hours + 1][TaxiStatsExtractor.COUNT + 2];
    private final String[][] carRidesOutside = new String[hours + 1][TaxiStatsExtractor.COUNT + 2];
    private final String[][] taxiIVTT = new String[hours + 1][TaxiStatsExtractor.COUNT + 2];
    private final String[][] taxiRides = new String[hours + 1][TaxiStatsExtractor.COUNT + 2];


    private void go()
    {
        header[0] = "hour";
        for (int h = 0; h < hours; h++) {
            carTTInside[h][0] = h + "";
            carRidesInside[h][0] = h + "";
            carTTOutside[h][0] = h + "";
            carRidesOutside[h][0] = h + "";
            taxiIVTT[h][0] = h + "";
            taxiRides[h][0] = h + "";
        }
        carTTInside[hours][0] = "daily";
        carRidesInside[hours][0] = "daily";
        carTTOutside[hours][0] = "daily";
        carRidesOutside[hours][0] = "daily";
        taxiIVTT[hours][0] = "daily";
        taxiRides[hours][0] = "daily";

        readData("00.0", "1.0", 1);

        int col = 2;
        for (String fleet : TaxiStatsExtractor.FLEETS) {
            for (String av : TaxiStatsExtractor.AVS) {
                readData(fleet, av, col++);
            }
        }

        try (CompactCSVWriter writer = new CompactCSVWriter(
                IOUtils.getBufferedWriter(path + "travelTimeStats_combined.txt"))) {
            writer.writeNext("carTTInside [s]");
            writer.writeNext(header);
            writer.writeAll(Arrays.asList(carTTInside));
            writer.writeNextEmpty();

            writer.writeNext("carRidesInside");
            writer.writeNext(header);
            writer.writeAll(Arrays.asList(carRidesInside));
            writer.writeNextEmpty();

            writer.writeNext("carTTOutside [s]");
            writer.writeNext(header);
            writer.writeAll(Arrays.asList(carTTOutside));
            writer.writeNextEmpty();

            writer.writeNext("carRidesOutside");
            writer.writeNext(header);
            writer.writeAll(Arrays.asList(carRidesOutside));
            writer.writeNextEmpty();

            writer.writeNext("taxiIVTT [s]");
            writer.writeNext(header);
            writer.writeAll(Arrays.asList(taxiIVTT));
            writer.writeNextEmpty();

            writer.writeNext("taxiRides");
            writer.writeNext(header);
            writer.writeAll(Arrays.asList(taxiRides));
            writer.writeNextEmpty();
        }
    }


    private void readData(String fleet, String av, int col)
    {
        String file = path + TaxiStatsExtractor.getId(fleet, av) + "/travelTimeStats.csv";
        List<String[]> data = CSVReaders.readSemicolonSV(file);

        header[col] = fleet + "_" + av;
        for (int h = 0; h <= hours; h++) {
            String[] line = data.get(h + 1);
            carTTInside[h][col] = line[COL_carTTInside];
            carRidesInside[h][col] = line[COL_carRidesInside];
            carTTOutside[h][col] = line[COL_carTTOutside];
            carRidesOutside[h][col] = line[COL_carRidesOutside];
            taxiIVTT[h][col] = line[COL_taxiIVTT];
            taxiRides[h][col] = line[COL_taxiRides];
        }
    }


    public static void main(String[] args)
    {
        new TravelTimeAnalysisExtractor().go();
    }
}
