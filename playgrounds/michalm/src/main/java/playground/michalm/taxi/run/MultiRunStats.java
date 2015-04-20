package playground.michalm.taxi.run;

import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.dvrp.data.VrpData;

import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;


public class MultiRunStats
{
    private final SummaryStatistics passengerWaitTime = new SummaryStatistics();
    private final SummaryStatistics pc95PassengerWaitTime = new SummaryStatistics();
    private final SummaryStatistics maxPassengerWaitTime = new SummaryStatistics();

    private final SummaryStatistics pickupDriveTime = new SummaryStatistics();
    private final SummaryStatistics pc95PickupDriveTime = new SummaryStatistics();
    private final SummaryStatistics maxPickupDriveTime = new SummaryStatistics();

    private final SummaryStatistics otherDriveTime = new SummaryStatistics();
    private final SummaryStatistics driveWithPassengerTime = new SummaryStatistics();
    private final SummaryStatistics pickupTime = new SummaryStatistics();

    private final SummaryStatistics computationTime = new SummaryStatistics();


    void updateStats(TaxiStats singleRunStats, long computationTimeInMillis)
    {
        passengerWaitTime.addValue(singleRunStats.passengerWaitTimes.getMean());
        pc95PassengerWaitTime.addValue(singleRunStats.passengerWaitTimes.getPercentile(95));
        maxPassengerWaitTime.addValue(singleRunStats.passengerWaitTimes.getMax());

        pickupDriveTime.addValue(singleRunStats.pickupDriveTimes.getMean());
        pc95PickupDriveTime.addValue(singleRunStats.pickupDriveTimes.getPercentile(95));
        maxPickupDriveTime.addValue(singleRunStats.pickupDriveTimes.getMax());

        otherDriveTime.addValue(singleRunStats.otherDriveTimes.getMean());
        driveWithPassengerTime.addValue(singleRunStats.driveWithPassengerTimes.getMean());
        pickupTime.addValue(singleRunStats.pickupTimes.getMean());

        computationTime.addValue(0.001 * (computationTimeInMillis));
    }


    static final String HEADER = "cfg\tn\tm\t"//
            + "PW\tPWp95\tPWmax\t"//
            + "PD\tPDp95\tPDmax\t"//
            + "otherD\tDwP\tP\t"//
            + "Comp";


    void printStats(PrintWriter pw, String cfg, VrpData data)
    {
        pw.printf("%20s\t%d\t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n",//
                cfg,//
                data.getRequests().size(),//
                data.getVehicles().size(),//
                //
                passengerWaitTime.getMean(),//
                pc95PassengerWaitTime.getMean(), //
                maxPassengerWaitTime.getMean(),//
                //
                pickupDriveTime.getMean(),//
                pc95PickupDriveTime.getMean(), //
                maxPickupDriveTime.getMean(),//
                //
                otherDriveTime.getMean(),//
                driveWithPassengerTime.getMean(),//
                pickupTime.getMean(),//
                //
                computationTime.getMean());
    }
}
