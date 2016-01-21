/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import org.apache.commons.lang3.StringUtils;


public class ResultsPostProcessor
{
    private static class Experiment
    {
        private final String id;
        private final List<Stats> stats = new ArrayList<>();


        private Experiment(String id)
        {
            this.id = id;
        }
    }


    /**
     * represents a single row in a file created by MultiRunStats all stats in such a file have the
     * same values for 'n' and 'm'
     */
    private static class Stats
    {
        private final String cfg;
        private final int n;
        private final int m;

        private final double[] values;


        private Stats(Scanner sc, int count)
        {
            cfg = sc.next();
            n = sc.nextInt();
            m = sc.nextInt();

            values = new double[count];
            for (int i = 0; i < count; i++) {
                values[i] = sc.nextDouble();
            }
        }
    }


    private static final Experiment EMPTY_COLUMN = new Experiment("empty column");

    private final Experiment[] experiments;
    private final String[] statsColumns;


    public ResultsPostProcessor(String... ids)
    {
        experiments = new Experiment[ids.length];
        for (int i = 0; i < experiments.length; i++) {
            String id = ids[i];
            experiments[i] = id == null ? EMPTY_COLUMN : new Experiment(ids[i]);
        }

        String[] cols = MultiRunStats.HEADER.split("\\s+");
        Arrays.equals(Arrays.copyOf(cols, 3), new String[] { "cfg", "n", "m" });
        statsColumns = Arrays.copyOfRange(cols, 3, cols.length);
    }


    public void process(String dir, String subDirPrefix, String file)
    {
        for (Experiment e : experiments) {
            if (e != EMPTY_COLUMN) {
                readFile(dir + subDirPrefix + e.id + "/" + file, e);
            }
        }

        for (int i = 0; i < statsColumns.length; i++) {
            writeValues(dir + file, i);
        }
    }


    private void readFile(String file, Experiment experiment)
    {
        try (Scanner sc = new Scanner(new File(file))) {
            String header = sc.nextLine();
            if (!header.equals(MultiRunStats.HEADER)) {
                System.err.println("Non-standard header");
            }

            if (!sc.hasNext()) {
                throw new RuntimeException("No stats");
            }

            Stats s0 = new Stats(sc, statsColumns.length);
            experiment.stats.add(s0);

            while (sc.hasNext()) {
                Stats stats = new Stats(sc, statsColumns.length);

                if (stats.m != s0.m || stats.n != s0.n) {
                    throw new RuntimeException(
                            "The file must contain result for the same 'm' and 'n'");
                }

                experiment.stats.add(stats);
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private void writeValues(String file, int column)
    {
        String field = statsColumns[column];
        try (PrintWriter pw = new PrintWriter(file + "_" + field)) {
            StringBuffer lineId = new StringBuffer(StringUtils.leftPad(field, 20));
            StringBuffer lineN = new StringBuffer(StringUtils.leftPad("n", 20));
            StringBuffer lineM = new StringBuffer(StringUtils.leftPad("m", 20));
            StringBuffer lineRatio = new StringBuffer(StringUtils.leftPad("ratio", 20));

            for (Experiment e : experiments) {
                if (e == EMPTY_COLUMN) {
                    lineId.append('\t');
                    lineN.append('\t');
                    lineM.append('\t');
                    lineRatio.append('\t');
                }
                else {
                    Stats s = e.stats.get(0);
                    double ratio = (double)s.n / s.m;
                    lineId.append('\t').append(e.id);
                    lineN.append('\t').append(s.n);
                    lineM.append('\t').append(s.m);
                    lineRatio.append('\t').append(String.format("%.2f", ratio));
                }
            }

            pw.println(lineId.toString());
            pw.println(lineN.toString());
            pw.println(lineM.toString());
            pw.println(lineRatio.toString());

            int statsCount = experiments[0].stats.size();
            DecimalFormat format = new DecimalFormat("#.##");

            for (int i = 0; i < statsCount; i++) {
                String cfg0 = experiments[0].stats.get(i).cfg;
                pw.printf("%20s", cfg0);

                for (Experiment e : experiments) {
                    if (e == EMPTY_COLUMN) {
                        pw.print('\t');//insert one empty column
                    }
                    else {
                        Stats s = e.stats.get(i);

                        if (!cfg0.equals(s.cfg)) {
                            throw new RuntimeException();
                        }

                        pw.print("\t" + format.format(s.values[column]));
                    }
                }

                pw.println();
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }


    public static void processMielec()
    {
        String dir = "d:/PP-rad/mielec/2014_02/";
        String subDirPrefix = "";

        new ResultsPostProcessor(//
                "10-50", //
                "15-50", //
                "20-50", //
                "25-50", //
                "30-50", //
                "35-50", //
                "40-50", //
                null, // empty column
                "10-25", //
                "15-25", //
                "20-25", //
                "25-25", //
                "30-25", //
                "35-25", //
                "40-25"//
        ).process(dir, subDirPrefix, "stats");
    }


    public static void processBerlin()
    {
        String dir = "d:/PP-rad/berlin/Only_Berlin_2015_08/";
        String subDirPrefix = "demand_";

        new ResultsPostProcessor(//
                "1.0", //
                "1.5", //
                "2.0", //
                "2.5", //
                "3.0", //
//                "3.1", //
//                "3.2", //
//                "3.3" //
                "3.5", //
                "4.0", //
                "4.5", //
                "5.0"//
        ).process(dir, subDirPrefix, "stats");
    }


    public static void processBarcelonaVariableDemand()
    {
        String dir = "d:/PP-rad/Barcelona/Barcelona_2015_09/";
        String subDirPrefix = "demand_";

        new ResultsPostProcessor(//
                "0.2", //
                "0.3", //
                "0.4", //
                "0.5", //
                "0.6", //
                "0.7", //
                "0.8", //
                "0.9" //
                //"1.0"//
        ).process(dir, subDirPrefix, "stats");
    }


    public static void processBarcelonaVariableSupply()
    {
        String dir = "d:/PP-rad/Barcelona/Barcelona_2015_09/";
        String subDirPrefix = "supply_from_reqs_";

        new ResultsPostProcessor(//
                //"0.2", //
                //"0.4", //
//                "0.6", //
//                "0.8", //
//                "1.0", //
//                "1.2", //
//                "1.4", //
//                "1.6", //
//                "1.8", //
//                "2.0"//
                "0.45_DSE"//
        ).process(dir, subDirPrefix, "stats");
    }


    public static void processAudiAV_10()
    {
        String dir = "d:/PP-rad/audi_av/audi_av_10pct_2015_10/";
        String subDirPrefix = "taxi_vehicles_";

        new ResultsPostProcessor(//
//                "04000", //
//                "04500", //
//                "05000", //
//                "05500", //
//                "06000", //
//                "06500", //
//                "07000", //
//                "07500", //
//                "08000" //
                "09000", //
                "10000", //
                "11000", //
                "12000", //
                "13000" //
//                "14000", //
//                "15000", //
//                "16000", //
//                "17000", //
//                "18000", //
//                "19000", //
//                "20000", //
//                "21000", //
//                "22000", //
//                "23000", //
//                "24000", //
//                "25000" //
        ).process(dir, subDirPrefix, "stats");
    }


    public static void processAudiAV_100()
    {
        String dir = "d:/PP-rad/audi_av/audi_av_2015_10/";
        String subDirPrefix = "taxi_vehicles_";

        new ResultsPostProcessor(//
//                "050000", //
//                "060000", //
//                "070000", //
                "080000", //
                "090000", //
                "100000", //
                "110000", //
                "120000" //
//                "130000", //
//                "140000", //
//                "150000", //
//                "160000", //
//                "170000", //
//                "180000", //
//                "190000", //
//                "200000", //
//                "210000", //
//                "220000", //
//                "230000", //
//                "240000", //
//                "250000" //
        ).process(dir, subDirPrefix, "stats");
    }


    public static void main(String[] args)
    {
        processMielec();
        //processBerlin();
        //processBarcelonaVariableDemand();
        //processBarcelonaVariableSupply();
        //processAudiAV_10();
        //processAudiAV_100();
    }
}
