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
import java.util.*;


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
            StringBuffer lineId = new StringBuffer(field);
            StringBuffer lineN = new StringBuffer("n");
            StringBuffer lineM = new StringBuffer("m");
            StringBuffer lineRatio = new StringBuffer("ratio");

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
                    lineRatio.append('\t').append(ratio);
                }
            }

            pw.println(lineId.toString());
            pw.println(lineN.toString());
            pw.println(lineM.toString());
            pw.println(lineRatio.toString());

            int statsCount = experiments[0].stats.size();

            for (int i = 0; i < statsCount; i++) {
                String cfg0 = experiments[0].stats.get(i).cfg;
                pw.printf("%s", cfg0);

                for (Experiment e : experiments) {
                    if (e == EMPTY_COLUMN) {
                        pw.print('\t');//insert one empty column
                    }
                    else {
                        Stats s = e.stats.get(i);

                        if (!cfg0.equals(s.cfg)) {
                            throw new RuntimeException();
                        }

                        pw.printf("\t%f", s.values[column]);
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
        String subDirPrefix = "mielec-2-peaks-new-";

        new ResultsPostProcessor(//
                "10-50",//
                "15-50",//
                "20-50",//
                "25-50",//
                "30-50",//
                "35-50",//
                "40-50",//
                null,// empty column
                "10-25",//
                "15-25",//
                "20-25",//
                "25-25",//
                "30-25",//
                "35-25",//
                "40-25"//
        ).process(dir, subDirPrefix, "stats");
    }


    public static void processBerlin()
    {
        String dir = "d:/michalm/Berlin_2014_11/";
        String subDirPrefix = "demand_";

        new ResultsPostProcessor(//
                "1.0",//
                "1.5",//
                "2.0",//
                "2.5",//
                "3.0",//
                "3.5",//
                "4.0",//
                "4.5",//
                "5.0"//
        ).process(dir, subDirPrefix, "stats");
    }


    public static void main(String[] args)
    {
        processMielec();
        //processBerlin();
    }
}
