package org.matsim.contrib.taxi.benchmark;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.util.stats.*;
import org.matsim.contrib.util.*;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;


public class TaxiBenchmarkStats
    implements AfterMobsimListener, ShutdownListener
{
    public static final String[] HEADER = { "n", "m", //
            "PassWait", //
            "PassWait_p95", //
            "PassWait_max", //
            "EmptyDriveRatio", //
            "StayRatio" };

    protected final TaxiData taxiData;
    private final OutputDirectoryHierarchy controlerIO;

    private final SummaryStatistics passengerWaitTime = new SummaryStatistics();
    private final SummaryStatistics pc95PassengerWaitTime = new SummaryStatistics();
    private final SummaryStatistics maxPassengerWaitTime = new SummaryStatistics();

    private final SummaryStatistics emptyDriveRatio = new SummaryStatistics();
    private final SummaryStatistics stayRatio = new SummaryStatistics();


    @Inject
    public TaxiBenchmarkStats(TaxiData taxiData, OutputDirectoryHierarchy controlerIO)
    {
        this.taxiData = taxiData;
        this.controlerIO = controlerIO;
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event)
    {
        TaxiStats singleRunStats = new TaxiStatsCalculator(taxiData.getVehicles().values())
                .getDailyStats();

        passengerWaitTime.addValue(singleRunStats.passengerWaitTime.getMean());
        pc95PassengerWaitTime.addValue(singleRunStats.passengerWaitTime.getPercentile(95));
        maxPassengerWaitTime.addValue(singleRunStats.passengerWaitTime.getMax());

        emptyDriveRatio.addValue(singleRunStats.getFleetEmptyDriveRatio());
        stayRatio.addValue(singleRunStats.getFleetStayRatio());
    }


    @Override
    public void notifyShutdown(ShutdownEvent event)
    {
        writeFile("benchmark_stats.txt", HEADER);
    }


    protected void writeFile(String file, String[] header)
    {
        try (CompactCSVWriter writer = new CompactCSVWriter(
                IOUtils.getBufferedWriter(controlerIO.getOutputFilename(file)))) {
            writer.writeNext(header);
            writer.writeNext(createAndInitLineBuilder());
        }
    }


    protected CSVLineBuilder createAndInitLineBuilder()
    {
        return new CSVLineBuilder()//
                .addf("%d", taxiData.getRequests().size())//
                .addf("%d", taxiData.getVehicles().size())//
                .addf("%.1f", passengerWaitTime.getMean())//
                .addf("%.0f", pc95PassengerWaitTime.getMean())//
                .addf("%.0f", maxPassengerWaitTime.getMean())//
                .addf("%.3f", emptyDriveRatio.getMean())//
                .addf("%.3f", stayRatio.getMean());
    }
}
