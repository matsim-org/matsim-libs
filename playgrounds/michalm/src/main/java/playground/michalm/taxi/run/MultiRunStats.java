package playground.michalm.taxi.run;

import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.dvrp.data.VrpData;

import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;


public class MultiRunStats
{
    final SummaryStatistics taxiPickupDriveTime = new SummaryStatistics();
    final SummaryStatistics percentile95TaxiPickupDriveTime = new SummaryStatistics();
    final SummaryStatistics maxTaxiPickupDriveTime = new SummaryStatistics();
    final SummaryStatistics taxiNonPickupDriveTime = new SummaryStatistics();
    final SummaryStatistics taxiDriveWithPassengerTime = new SummaryStatistics();
    final SummaryStatistics taxiPickupTime = new SummaryStatistics();
    final SummaryStatistics passengerWaitTime = new SummaryStatistics();
    final SummaryStatistics percentile95PassengerWaitTime = new SummaryStatistics();
    final SummaryStatistics maxPassengerWaitTime = new SummaryStatistics();
    final SummaryStatistics computationTime = new SummaryStatistics();


    void updateStats(TaxiStats evaluation, long computationTimeInMillis)
    {
        taxiPickupDriveTime.addValue(evaluation.getPickupDriveTime());
        percentile95TaxiPickupDriveTime.addValue(evaluation.getPickupDriveTimeStats()
                .getPercentile(95));
        maxTaxiPickupDriveTime.addValue(evaluation.getMaxPickupDriveTime());
        taxiNonPickupDriveTime.addValue(evaluation.getNonPickupDriveTime());
        taxiDriveWithPassengerTime.addValue(evaluation.getDriveWithPassengerTime());
        taxiPickupTime.addValue(evaluation.getPickupTime());
        passengerWaitTime.addValue(evaluation.getPassengerWaitTime());
        percentile95PassengerWaitTime.addValue(evaluation.getPassengerWaitTimeStats()
                .getPercentile(95));
        maxPassengerWaitTime.addValue(evaluation.getMaxPassengerWaitTime());
        computationTime.addValue(0.001 * (computationTimeInMillis));
    }


    void printStats(PrintWriter pw, String cfg, VrpData data)
    {
        pw.printf(
                "%20s\t%d\t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n",//
                cfg,//
                data.getRequests().size(),//
                data.getVehicles().size(),//
                passengerWaitTime.getMean(),//
                percentile95PassengerWaitTime.getMean(), //
                maxPassengerWaitTime.getMean(),//
                taxiPickupDriveTime.getMean(),//
                percentile95TaxiPickupDriveTime.getMean(), //
                maxTaxiPickupDriveTime.getMean(),//
                taxiNonPickupDriveTime.getMean(),//
                taxiDriveWithPassengerTime.getMean(),//
                taxiPickupTime.getMean(),//
                computationTime.getMean());
    }
}
