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
        private String name;
        private int n;
        private int m;

        private double passengerWaitT;
        private double percentile95PassengerWaitT;
        private double maxPassengerWaitT;
        private double pickupDriveT;
        private double percentile95PickupDriveT;
        private double maxPickupDriveT;
        private double dropoffDriveT;
        private double pickupT;
        private double dropoffT;
        @SuppressWarnings("unused")
        private double waitT;
        @SuppressWarnings("unused")
        private double compT;

        // ============

        private double T_W;
        private double T_W_95;
        private double T_W_MAX;
        @SuppressWarnings("unused")
        private double T_D;
        @SuppressWarnings("unused")
        private double R_W;
        private double T_P;
        private double T_P_95;
        private double T_P_MAX;
        @SuppressWarnings("unused")
        private double R_P;
        private double R_NI;


        private void calcStats(int timeWindow)
        {
            T_W = passengerWaitT / n / 60;
            T_W_95 = percentile95PassengerWaitT / 60;
            T_W_MAX = maxPassengerWaitT / 60;
            T_P = pickupDriveT / n / 60;
            T_P_95 = percentile95PickupDriveT / 60;
            T_P_MAX = maxPickupDriveT / 60;
            T_D = dropoffDriveT / n / 60;
            R_W = passengerWaitT / (passengerWaitT + pickupT + dropoffDriveT + dropoffT);
            R_P = pickupDriveT / (pickupDriveT + dropoffDriveT);
            R_NI = (pickupDriveT + pickupT + dropoffDriveT + dropoffT) / (timeWindow * m);
        }
    }


    private final List<Experiment> experiments;
    private final int timeWindow;


    public ResultsPostProcessor(List<Experiment> experiments, int timeWindow)
    {
        this.experiments = experiments;
        this.timeWindow = timeWindow;
    }


    private void readFile(String file, Experiment experiment)
    {
        try (Scanner sc = new Scanner(new File(file))) {
            // header
            // cfg n   m   PW  PWmax   PD  DD  PS  DS  W   Comp
            sc.nextLine();

            int m0 = -1;
            int n0 = -1;
            while (sc.hasNext()) {
                Stats stats = readLine(sc);

                if (experiment.stats.size() == 0) {
                    m0 = stats.m;
                    n0 = stats.n;
                }
                else {
                    if (stats.m != m0 || stats.n != n0) {
                        throw new RuntimeException(
                                "The file should contain result for the same 'm' and 'n'");
                    }
                }

                experiment.stats.add(stats);
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private Stats readLine(Scanner sc)
    {
        Stats stats = new Stats();

        //        cfg n   m   PW  PWp95   PWmax   PD  PDp95   PDmax   DD  PS  DS  W   Comp
        //        APS_15M_TW    1719    50  276513.60   380.00  763.45  218268.90   NaN 0.00    610428.85   206280.00   103140.00   3001882.25  11.94

        stats.name = sc.next();
        stats.n = sc.nextInt();
        stats.m = sc.nextInt();

        stats.passengerWaitT = sc.nextDouble();
        stats.percentile95PassengerWaitT = sc.nextDouble();
        stats.maxPassengerWaitT = sc.nextDouble();
        stats.pickupDriveT = sc.nextDouble();
        stats.percentile95PickupDriveT = sc.nextDouble();
        stats.maxPickupDriveT = sc.nextDouble();
        stats.dropoffDriveT = sc.nextDouble();
        stats.pickupT = sc.nextDouble();
        stats.dropoffT = sc.nextDouble();
        stats.waitT = sc.nextDouble();
        stats.compT = sc.nextDouble();

        stats.calcStats(timeWindow);

        return stats;
    }


    private void writeValues(String filename, String field)
    {
        try (PrintWriter pw = new PrintWriter(filename)) {
            pw.printf("%s", field);

            {
                int prevTaxis = experiments.get(0).stats.get(0).m;

                for (Experiment e : experiments) {
                    int m = e.stats.get(0).m;

                    if (prevTaxis != m) {
                        pw.print('\t');//insert one empty column
                    }
                    prevTaxis = m;

                    double ratio = (double)e.stats.get(0).n / m;
                    pw.printf("\t%f", ratio);
                }
            }

            pw.println();

            int count = experiments.get(0).stats.size();

            for (int i = 0; i < count; i++) {
                Stats s0 = experiments.get(0).stats.get(i);
                String name = s0.name;
                pw.printf("%s", name);
                int prevTaxis = s0.m;

                for (Experiment e : experiments) {
                    double value;
                    Stats s = e.stats.get(i);

                    if (!name.equals(s.name)) {
                        throw new RuntimeException();
                    }

                    if ("T_W".equals(field)) {
                        value = s.T_W;
                    }
                    else if ("T_W_95".equals(field)) {
                        value = s.T_W_95;
                    }
                    else if ("T_W_MAX".equals(field)) {
                        value = s.T_W_MAX;
                    }
                    else if ("T_P".equals(field)) {
                        value = s.T_P;
                    }
                    else if ("T_P_95".equals(field)) {
                        value = s.T_P_95;
                    }
                    else if ("T_P_MAX".equals(field)) {
                        value = s.T_P_MAX;
                    }
                    else if ("T_W-T_P".equals(field)) {
                        value = s.T_W - s.T_P;
                    }
                    else if ("R_NI".equals(field)) {
                        value = s.R_NI;
                    }
                    else {
                        throw new RuntimeException();
                    }

                    if (prevTaxis != s.m) {
                        pw.print('\t');
                    }
                    prevTaxis = s.m;

                    pw.printf("\t%f", value);
                }

                pw.println();
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }


    public void process(String dir, String subDirPrefix, String filename)
    {
        for (Experiment e : experiments) {
            readFile(dir + subDirPrefix + e.id + "/" + filename, e);
        }

        writeValues(dir + filename + ".T_W", "T_W");
        writeValues(dir + filename + ".T_W_95", "T_W_95");
        writeValues(dir + filename + ".T_W_MAX", "T_W_MAX");
        writeValues(dir + filename + ".T_P", "T_P");
        writeValues(dir + filename + ".T_P_95", "T_P_95");
        writeValues(dir + filename + ".T_P_MAX", "T_P_MAX");
        writeValues(dir + filename + ".T_W_T_P", "T_W-T_P");
        writeValues(dir + filename + ".R_NI", "R_NI");
    }


    public static void processMielec()
    {
        List<Experiment> experiments = new ArrayList<>();
        experiments.add(new Experiment("10-50"));
        experiments.add(new Experiment("15-50"));
        experiments.add(new Experiment("20-50"));
        experiments.add(new Experiment("25-50"));
        experiments.add(new Experiment("30-50"));
        experiments.add(new Experiment("35-50"));
        experiments.add(new Experiment("40-50"));

        experiments.add(new Experiment("10-25"));
        experiments.add(new Experiment("15-25"));
        experiments.add(new Experiment("20-25"));
        experiments.add(new Experiment("25-25"));
        experiments.add(new Experiment("30-25"));
        experiments.add(new Experiment("35-25"));
        experiments.add(new Experiment("40-25"));

        int timeWindow = 14 * 3600;//approx.

        String dir = "d:/michalm/2014_02/";
        String subDirPrefix = "mielec-2-peaks-new-";

        new ResultsPostProcessor(experiments, timeWindow).process(dir, subDirPrefix, "stats");
    }


    public static void processBerlin()
    {
        List<Experiment> experiments = new ArrayList<>();
        experiments.add(new Experiment("1.0"));
        experiments.add(new Experiment("1.5"));
        experiments.add(new Experiment("2.0"));
        experiments.add(new Experiment("2.5"));
        experiments.add(new Experiment("3.0"));
        experiments.add(new Experiment("3.5"));
        experiments.add(new Experiment("4.0"));
        experiments.add(new Experiment("4.5"));
        experiments.add(new Experiment("5.0"));

        int timeWindow = 14 * 3600;//very approx.

        String dir = "d:/michalm/Berlin_2014_11/";
        String subDirPrefix = "demand_";

        new ResultsPostProcessor(experiments, timeWindow).process(dir, subDirPrefix, "stats");
    }


    public static void main(String[] args)
    {
        //processMielec();
        processBerlin();
    }
}
