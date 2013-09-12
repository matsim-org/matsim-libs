package playground.michalm.vrp.run;

import java.io.*;
import java.util.*;


public class ResultsPostProcessor
{
    // 0 PickupT DeliveryT ServiceT CruiseT WaitT OverT PassengerWaitT MaxPassengerWaitT
    // Mean 280159,85 867978,15 261000,00 0,00 7230862,00 0,00 280166,45 1053,90
    // Min 275199 865726 261000 0 7224940 0 275199 275199
    // Max 284150 870788 261000 0 7236939 0 284150 284150
    // StdDev 2266,09 1425,12 0,00 0,00 2915,95 0,00 2272,45 2272,45
    //
    // 1 PickupT DeliveryT ServiceT CruiseT WaitT OverT PassengerWaitT MaxPassengerWaitT
    // Mean 263303,85 867941,05 261000,00 0,00 7247755,10 0,00 263322,25 973,45
    // Min 258881 865937 261000 0 7239965 0 258881 258881
    // Max 268763 871056 261000 0 7251583 0 268935 268935
    // StdDev 2618,62 1299,13 0,00 0,00 3298,15 0,00 2643,21 2643,21

    private static class Experiment
    {
        private final int demand;
        private final int reqs;
        private final int taxis;


        public Experiment(int demand, int reqs, int taxis)
        {
            this.demand = demand;
            this.reqs = reqs;
            this.taxis = taxis;
        }
    }


    private static class Stats
    {
        private static final int TIME_WINDOW = 14 * 3600;// for the time being...:-/

        // ============

        private Experiment experiment;

        // ============

        private double pickupT;
        private double deliveryT;
        private double serviceT;
        private double cruiseT;
        private double waitT;
        private double overT;
        private double passengerWaitT;
        private double maxPassengerWaitT;

        // ============

        private double T_W;
        private double T_W_MAX;
        private double T_D;
        private double R_W;
        private double T_P;
        private double R_P;
        private double R_NI;


        private void calcStats()
        {
            T_W = passengerWaitT / experiment.reqs / 60;
            T_W_MAX = maxPassengerWaitT / 60;
            T_D = deliveryT / experiment.reqs / 60;
            R_W = passengerWaitT / (passengerWaitT + serviceT + deliveryT);
            T_P = pickupT / experiment.reqs / 60;
            R_P = pickupT / (pickupT + deliveryT);
            R_NI = (pickupT + serviceT + deliveryT) / (TIME_WINDOW * experiment.taxis);
        }
    }


    private List<Experiment> experiments;
    private List<Stats[]> allStats;


    private Stats[] read(String file, Experiment experiment)
        throws FileNotFoundException
    {
        Scanner sc = new Scanner(new File(file));
        Stats[] stats = new Stats[AlgorithmConfig.ALL.length];

        for (int i = 0; i < stats.length; i++) {
            stats[i] = readSection(sc, experiment);
        }

        return stats;
    }


    private Stats readSection(Scanner sc, Experiment experiment)
    {
        Stats stats = new Stats();
        stats.experiment = experiment;

        sc.nextLine();// header
        sc.next();// row header: "Mean"

        stats.pickupT = sc.nextDouble();
        stats.deliveryT = sc.nextDouble();
        stats.serviceT = sc.nextDouble();
        stats.cruiseT = sc.nextDouble();
        stats.waitT = sc.nextDouble();
        stats.overT = sc.nextDouble();
        stats.passengerWaitT = sc.nextDouble();
        stats.maxPassengerWaitT = sc.nextDouble();

        sc.nextLine();// Mean (the rest of the line)
        sc.nextLine();// Min
        sc.nextLine();// Max
        sc.nextLine();// StdDev
        sc.nextLine();// empty line (separator)

        if (Double.isNaN(stats.pickupT)) {
            return null;
        }

        stats.calcStats();

        return stats;
    }


    private void writeValues(String filename, String field)
        throws FileNotFoundException
    {
        PrintWriter pw = new PrintWriter(filename);

        pw.printf("%s\t", field);

        for (Experiment e : experiments) {
            double ratio = (double)e.reqs / e.taxis;
            pw.printf("\t%f", ratio);

        }

        pw.println();

        for (int i = 0; i < AlgorithmConfig.ALL.length; i++) {

            if (i == 2 || (i >= 5 && i <= 8) || (i >= 11 && i <= 14)) {
                continue;
            }

            AlgorithmConfig ac = AlgorithmConfig.ALL[i];
            pw.printf("%d\t%s", i, ac.algorithmType.shortcut);

            for (Stats[] stats : allStats) {
                if (stats[i] == null) {
                    pw.print("\t");
                    continue;
                }

                double value;

                if ("T_W".equals(field)) {
                    value = stats[i].T_W;
                }
                else if ("T_W_MAX".equals(field)) {
                    value = stats[i].T_W_MAX;
                }
                else if ("T_P".equals(field)) {
                    value = stats[i].T_P;
                }
                else if ("T_W-T_P".equals(field)) {
                    value = stats[i].T_W - stats[i].T_P;
                }
                else if ("R_NI".equals(field)) {
                    value = stats[i].R_NI;
                }
                else {
                    throw new RuntimeException();
                }

                pw.printf("\t%f", value);
            }

            pw.println();
        }

        pw.close();
    }


    public void go(boolean destinationKnown, boolean onlineVehicleTracker,
            boolean minimizePickupTripTime)
        throws FileNotFoundException
    {
        String dir = "d:\\michalm\\2013_07\\";
        String subdirPrefix = "mielec-2-peaks-new-";
        String filename = "stats_DK_" + destinationKnown + "_VT_" + onlineVehicleTracker + "_TP_"
                + minimizePickupTripTime + ".out";

        experiments = new ArrayList<Experiment>();
        experiments.add(new Experiment(10, 406, 50));
        experiments.add(new Experiment(15, 636, 50));
        experiments.add(new Experiment(20, 840, 50));
        experiments.add(new Experiment(25, 1069, 50));
        experiments.add(new Experiment(30, 1297, 50));
        experiments.add(new Experiment(35, 1506, 50));
        experiments.add(new Experiment(40, 1719, 50));

        experiments.add(new Experiment(10, 406, 25));
        experiments.add(new Experiment(15, 636, 25));
        experiments.add(new Experiment(20, 840, 25));
        experiments.add(new Experiment(25, 1069, 25));
        experiments.add(new Experiment(30, 1297, 25));
        experiments.add(new Experiment(35, 1506, 25));
        experiments.add(new Experiment(40, 1719, 25));

        allStats = new ArrayList<Stats[]>();
        for (Experiment e : experiments) {
            allStats.add(read(dir + subdirPrefix + e.demand + '-' + e.taxis + "\\" + filename, e));
        }

        writeValues(dir + filename + ".T_W", "T_W");
        writeValues(dir + filename + ".T_W_MAX", "T_W_MAX");
        writeValues(dir + filename + ".T_P", "T_P");
        writeValues(dir + filename + ".T_W_T_P", "T_W-T_P");
        writeValues(dir + filename + ".R_NI", "R_NI");
    }


    public static void main(String[] args)
        throws FileNotFoundException
    {
        new ResultsPostProcessor().go(false, false, false);
        new ResultsPostProcessor().go(false, true, false);
        new ResultsPostProcessor().go(true, false, false);
        new ResultsPostProcessor().go(true, true, false);

        new ResultsPostProcessor().go(false, false, true);
        new ResultsPostProcessor().go(false, true, true);
        new ResultsPostProcessor().go(true, false, true);
        new ResultsPostProcessor().go(true, true, true);
    }
}
