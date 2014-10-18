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
        private final int demand;
        private final int taxis;
        private final List<Stats> stats;


        private Experiment(int demand, int taxis)
        {
            this.demand = demand;
            this.taxis = taxis;
            this.stats = new ArrayList<>();
        }
    }


    private static class Stats
    {
        private static final int TIME_WINDOW = 14 * 3600;// for the time being...:-/

        // ============

        private String name;
        private int n;
        private int m;

        // ============

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


        private void calcStats()
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
            R_NI = (pickupDriveT + pickupT + dropoffDriveT + dropoffT) / (TIME_WINDOW * m);
        }
    }


    private Experiment[] experiments;


    public ResultsPostProcessor()
    {
        experiments = new Experiment[14];
        experiments[0] = new Experiment(10, 50);
        experiments[1] = new Experiment(15, 50);
        experiments[2] = new Experiment(20, 50);
        experiments[3] = new Experiment(25, 50);
        experiments[4] = new Experiment(30, 50);
        experiments[5] = new Experiment(35, 50);
        experiments[6] = new Experiment(40, 50);

        experiments[7] = new Experiment(10, 25);
        experiments[8] = new Experiment(15, 25);
        experiments[9] = new Experiment(20, 25);
        experiments[10] = new Experiment(25, 25);
        experiments[11] = new Experiment(30, 25);
        experiments[12] = new Experiment(35, 25);
        experiments[13] = new Experiment(40, 25);
    }


    private void readFile(String file, Experiment experiment)
        throws FileNotFoundException
    {
        Scanner sc = new Scanner(new File(file));

        // header
        // cfg n   m   PW  PWmax   PD  DD  PS  DS  W   Comp
        sc.nextLine();

        while (sc.hasNext()) {
            experiment.stats.add(readLine(sc));
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

        stats.calcStats();

        return stats;
    }


    private void writeValues(String filename, String field)
        throws FileNotFoundException
    {
        PrintWriter pw = new PrintWriter(filename);

        pw.printf("%s", field);

        {
            int prevTaxis = experiments[0].taxis;

            for (Experiment e : experiments) {
                if (prevTaxis != e.taxis) {
                    pw.print('\t');
                }
                prevTaxis = e.taxis;

                double ratio = (double)e.stats.get(0).n / e.taxis;
                pw.printf("\t%f", ratio);
            }
        }

        pw.println();

        int count = experiments[0].stats.size();

        for (int i = 0; i < count; i++) {
            String name = experiments[0].stats.get(i).name;

            pw.printf("%s", name);

            int prevTaxis = experiments[0].taxis;

            for (Experiment e : experiments) {
                double value;
                Stats s = e.stats.get(i);

                if (!name.equals(s.name)) {
                    pw.close();
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
                    pw.close();
                    throw new RuntimeException();
                }

                if (prevTaxis != e.taxis) {
                    pw.print('\t');
                }
                prevTaxis = e.taxis;

                pw.printf("\t%f", value);
            }

            pw.println();
        }

        pw.close();
    }


    private static final String DIR = "d:\\michalm\\2014_02\\";
    private static final String SUBDIR_PREFIX = "mielec-2-peaks-new-";


    public void go(String filename)
        throws FileNotFoundException
    {
        for (Experiment e : experiments) {
            readFile(DIR + SUBDIR_PREFIX + e.demand + '-' + e.taxis + "\\" + filename, e);
        }

        writeValues(DIR + filename + ".T_W", "T_W");
        writeValues(DIR + filename + ".T_W_95", "T_W_95");
        writeValues(DIR + filename + ".T_W_MAX", "T_W_MAX");
        writeValues(DIR + filename + ".T_P", "T_P");
        writeValues(DIR + filename + ".T_P_95", "T_P_95");
        writeValues(DIR + filename + ".T_P_MAX", "T_P_MAX");
        writeValues(DIR + filename + ".T_W_T_P", "T_W-T_P");
        writeValues(DIR + filename + ".R_NI", "R_NI");
    }


    public static void main(String[] args)
        throws FileNotFoundException
    {
        new ResultsPostProcessor().go("stats");
    }
}
