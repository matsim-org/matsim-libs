package playground.michalm.vrp.run;

import java.io.*;
import java.util.Scanner;


public class ResultsPostProcessor
{
    //    0   PickupT DeliveryT   ServiceT    CruiseT WaitT   OverT   PassengerWaitT  MaxPassengerWaitT
    //    Mean    280159,85   867978,15   261000,00   0,00    7230862,00  0,00    280166,45   1053,90
    //    Min 275199  865726  261000  0   7224940 0   275199  275199
    //    Max 284150  870788  261000  0   7236939 0   284150  284150
    //    StdDev  2266,09 1425,12 0,00    0,00    2915,95 0,00    2272,45 2272,45
    //
    //    1   PickupT DeliveryT   ServiceT    CruiseT WaitT   OverT   PassengerWaitT  MaxPassengerWaitT
    //    Mean    263303,85   867941,05   261000,00   0,00    7247755,10  0,00    263322,25   973,45
    //    Min 258881  865937  261000  0   7239965 0   258881  258881
    //    Max 268763  871056  261000  0   7251583 0   268935  268935
    //    StdDev  2618,62 1299,13 0,00    0,00    3298,15 0,00    2643,21 2643,21

    private static class Experiment
    {
        private final int reqs;
        private final int taxis;


        public Experiment(int reqs, int taxis)
        {
            this.reqs = reqs;
            this.taxis = taxis;
        }
    }


    private static class Stats
    {
        private static final int TIME_WINDOW = 14 * 3600;//for the time being...:-/

        //============

        private Experiment experiment;

        //============

        private double pickupT;
        private double deliveryT;
        private double serviceT;
        private double cruiseT;
        private double waitT;
        private double overT;
        private double passengerWaitT;
        private double maxPassengerWaitT;

        //============

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


    private Experiment[] experiments;
    private Stats[][] allStats;


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

        sc.nextLine();//header
        sc.next();//row header: "Mean"

        stats.pickupT = sc.nextDouble();
        stats.deliveryT = sc.nextDouble();
        stats.serviceT = sc.nextDouble();
        stats.cruiseT = sc.nextDouble();
        stats.waitT = sc.nextDouble();
        stats.overT = sc.nextDouble();
        stats.passengerWaitT = sc.nextDouble();
        stats.maxPassengerWaitT = sc.nextDouble();

        sc.nextLine();//Mean (the rest of the line)
        sc.nextLine();//Min
        sc.nextLine();//Max
        sc.nextLine();//StdDev
        sc.nextLine();//empty line (separator)

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
            double ratio = e.reqs / e.taxis;
            pw.printf("\t%f", ratio);

        }

        pw.println();

        for (int i = 0; i < AlgorithmConfig.ALL.length; i++) {
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


    public void go(boolean destinationKnown, boolean onlineVehicleTracker)
        throws FileNotFoundException
    {
        String dir = "d:\\PP-rad\\taxi\\mielec-2-peaks\\2013_07\\";

        String filename = "stats_destination_" + destinationKnown + "_online_"
                + onlineVehicleTracker + ".out";

        experiments = new Experiment[6];
        experiments[0] = new Experiment(917, 100);
        experiments[1] = new Experiment(1528, 100);
        experiments[2] = new Experiment(2175, 100);
        experiments[3] = new Experiment(917, 50);
        experiments[4] = new Experiment(1528, 50);
        experiments[5] = new Experiment(2175, 50);

        allStats = new Stats[6][];
        allStats[0] = read(dir + "mielec-2-peaks-new-03-100\\" + filename, experiments[0]);
        allStats[1] = read(dir + "mielec-2-peaks-new-05-100\\" + filename, experiments[1]);
        allStats[2] = read(dir + "mielec-2-peaks-new-07-100\\" + filename, experiments[2]);
        allStats[3] = read(dir + "mielec-2-peaks-new-03-50\\" + filename, experiments[3]);
        allStats[4] = read(dir + "mielec-2-peaks-new-05-50\\" + filename, experiments[4]);
        allStats[5] = read(dir + "mielec-2-peaks-new-07-50\\" + filename, experiments[5]);

        writeValues(dir + filename + ".T_W", "T_W");
        writeValues(dir + filename + ".T_P", "T_P");
        writeValues(dir + filename + ".T_W_T_P", "T_W-T_P");
        writeValues(dir + filename + ".R_NI", "R_NI");
    }


    public static void main(String[] args)
        throws FileNotFoundException
    {
        new ResultsPostProcessor().go(false, false);
        new ResultsPostProcessor().go(false, true);
        new ResultsPostProcessor().go(true, false);
        new ResultsPostProcessor().go(true, true);
    }
}
