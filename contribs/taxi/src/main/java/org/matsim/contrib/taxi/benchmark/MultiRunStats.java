package org.matsim.contrib.taxi.benchmark;

import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.util.stats.*;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.utils.io.IOUtils;


public class MultiRunStats
    implements AfterMobsimListener, ShutdownListener

{
    static final String HEADER = "cfg\tn\tm\t"//
            + "PassWait\t"//
            + "PassWait_p95\t"//
            + "PassWait_max\t"//
            + "PassDrive\t"//
            + "EmptyRatio\t";

    private final TaxiData taxiData;
    private final String outputDir;
    private final String id;

    private final SummaryStatistics passengerWaitTime = new SummaryStatistics();
    private final SummaryStatistics pc95PassengerWaitTime = new SummaryStatistics();
    private final SummaryStatistics maxPassengerWaitTime = new SummaryStatistics();

    private final SummaryStatistics driveOccupiedTime = new SummaryStatistics();
    private final SummaryStatistics driveEmptyRatio = new SummaryStatistics();


    public MultiRunStats(TaxiData taxiData, String outputDir, String id)
    {
        this.taxiData = taxiData;
        this.outputDir = outputDir;
        this.id = id;
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event)
    {
        TaxiStats singleRunStats = new TaxiStatsCalculator(taxiData.getVehicles().values())
                .getStats();

        passengerWaitTime.addValue(singleRunStats.passengerWaitTimes.getMean());
        pc95PassengerWaitTime.addValue(singleRunStats.passengerWaitTimes.getPercentile(95));
        maxPassengerWaitTime.addValue(singleRunStats.passengerWaitTimes.getMax());

        driveOccupiedTime.addValue(singleRunStats.getDriveOccupiedTimes().getMean());
        driveEmptyRatio.addValue(singleRunStats.getDriveEmptyRatio());
    }


    @Override
    public void notifyShutdown(ShutdownEvent event)
    {
        PrintWriter pw = new PrintWriter(
                IOUtils.getBufferedWriter(outputDir + "/multiStats_" + id + ".txt"));
        pw.println(HEADER);
        pw.printf(
                "%20s\t%d\t%d\t"//
                        + "%.0f\t%.0f\t%.0f\t"//
                        + "%.0f\t%.2f\t", //
                id, //
                taxiData.getRequests().size(), //
                taxiData.getVehicles().size(), //
                //
                passengerWaitTime.getMean(), //
                pc95PassengerWaitTime.getMean(), //
                maxPassengerWaitTime.getMean(), //
                //
                driveOccupiedTime.getMean(), //
                driveEmptyRatio.getMean() * 100); //in [%]
        
        pw.close();
    }
}
