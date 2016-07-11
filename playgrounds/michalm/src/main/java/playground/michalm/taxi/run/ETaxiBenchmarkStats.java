package playground.michalm.taxi.run;

import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.util.stats.*;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

import playground.michalm.taxi.utli.stats.*;


public class ETaxiBenchmarkStats
    implements AfterMobsimListener, ShutdownListener

{
    static final String HEADER = "n\tm\t"//
            + "PassWait\t"//
            + "PassWait_p95\t"//
            + "PassWait_max\t"//
            + "EmptyDriveRatio\t"//
            + "StayRatio\t"//
            + "QueuedTimeRatio\t";

    private final TaxiData taxiData;
    private final OutputDirectoryHierarchy controlerIO;

    private final SummaryStatistics passengerWaitTime = new SummaryStatistics();
    private final SummaryStatistics pc95PassengerWaitTime = new SummaryStatistics();
    private final SummaryStatistics maxPassengerWaitTime = new SummaryStatistics();

    private final SummaryStatistics emptyDriveRatio = new SummaryStatistics();
    private final SummaryStatistics stayRatio = new SummaryStatistics();
    
    private final SummaryStatistics queuedTimeRatio = new SummaryStatistics();


    @Inject
    public ETaxiBenchmarkStats(TaxiData taxiData, OutputDirectoryHierarchy controlerIO)
    {
        this.taxiData = taxiData;
        this.controlerIO = controlerIO;
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event)
    {
        TaxiStats singleRunStats = new TaxiStatsCalculator(taxiData.getVehicles().values())
                .getDailyStats();
        ETaxiStats singleRunEStats = new ETaxiStatsCalculator(taxiData.getVehicles().values())
                .getDailyEStats();

        passengerWaitTime.addValue(singleRunStats.passengerWaitTime.getMean());
        pc95PassengerWaitTime.addValue(singleRunStats.passengerWaitTime.getPercentile(95));
        maxPassengerWaitTime.addValue(singleRunStats.passengerWaitTime.getMax());

        emptyDriveRatio.addValue(singleRunStats.getFleetEmptyDriveRatio());
        stayRatio.addValue(singleRunStats.getFleetStayRatio());
        
        queuedTimeRatio.addValue(singleRunEStats.getFleetQueuedTimeRatio());
    }


    @Override
    public void notifyShutdown(ShutdownEvent event)
    {
        PrintWriter pw = new PrintWriter(
                IOUtils.getBufferedWriter(controlerIO.getOutputFilename("ebenchmark_stats.txt")));
        pw.println(HEADER);
        pw.printf(
                "%d\t%d\t"//
                        + "%.1f\t%.0f\t%.0f\t"//
                        + "%.3f\t%.3f\t%.3f\t", //
                taxiData.getRequests().size(), //
                taxiData.getVehicles().size(), //
                //
                passengerWaitTime.getMean(), //
                pc95PassengerWaitTime.getMean(), //
                maxPassengerWaitTime.getMean(), //
                //
                emptyDriveRatio.getMean(), //
                stayRatio.getMean(), //
                //
                queuedTimeRatio.getMean());

        pw.close();
    }
}
