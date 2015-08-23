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

    private final SummaryStatistics driveWithPassengerTime = new SummaryStatistics();
    private final SummaryStatistics driveEmptyRatio = new SummaryStatistics();

    private final SummaryStatistics computationTime = new SummaryStatistics();


    void updateStats(TaxiStats singleRunStats, long computationTimeInMillis)
    {
        passengerWaitTime.addValue(singleRunStats.passengerWaitTimes.getMean());
        pc95PassengerWaitTime.addValue(singleRunStats.passengerWaitTimes.getPercentile(95));
        maxPassengerWaitTime.addValue(singleRunStats.passengerWaitTimes.getMax());

        driveWithPassengerTime.addValue(singleRunStats.getDriveWithPassengerTimes().getMean());
        driveEmptyRatio.addValue(singleRunStats.getDriveEmptyRatio());

        computationTime.addValue(0.001 * (computationTimeInMillis));
    }


    static final String HEADER = "cfg\tn\tm\t"//
            + "PassWait\t"//
            + "PassWait_p95\t"//
            + "PassWait_max\t"//
            + "PassDrive\t"//
            + "EmptyRatio\t"//
            + "Comp";


    void printStats(PrintWriter pw, String cfg, VrpData data)
    {
        pw.printf(
                "%20s\t%d\t%d\t"//
                        + "%.0f\t"//
                        + "%.0f\t"//
                        + "%.0f\t"//
                        + "%.0f\t"//
                        + "%.2f\t"//
                        + "%.1f\n", //
                cfg, //
                data.getRequests().size(), //
                data.getVehicles().size(), //
                //
                passengerWaitTime.getMean(), //
                pc95PassengerWaitTime.getMean(), //
                maxPassengerWaitTime.getMean(), //
                //
                driveWithPassengerTime.getMean(), //
                driveEmptyRatio.getMean() * 100, //in [%]
                //
                computationTime.getMean());
    }
}
