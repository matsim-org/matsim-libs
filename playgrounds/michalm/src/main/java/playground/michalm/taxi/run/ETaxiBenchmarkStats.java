package playground.michalm.taxi.run;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.taxi.benchmark.TaxiBenchmarkStats;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.util.CSVLineBuilder;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.*;

import com.google.common.collect.ObjectArrays;
import com.google.inject.Inject;

import playground.michalm.taxi.utli.stats.*;


public class ETaxiBenchmarkStats
    extends TaxiBenchmarkStats
{
    public static final String[] HEADER = ObjectArrays.concat(TaxiBenchmarkStats.HEADER,
            "QueuedTimeRatio");

    private final SummaryStatistics queuedTimeRatio = new SummaryStatistics();


    @Inject
    public ETaxiBenchmarkStats(TaxiData taxiData, OutputDirectoryHierarchy controlerIO)
    {
        super(taxiData, controlerIO);
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event)
    {
        super.notifyAfterMobsim(event);
        ETaxiStats singleRunEStats = new ETaxiStatsCalculator(taxiData.getVehicles().values())
                .getDailyEStats();
        queuedTimeRatio.addValue(singleRunEStats.getFleetQueuedTimeRatio());
    }


    @Override
    public void notifyShutdown(ShutdownEvent event)
    {
        writeFile("ebenchmark_stats.txt", HEADER);
    }


    protected CSVLineBuilder createAndInitLineBuilder()
    {
        return super.createAndInitLineBuilder().addf("%.3f", queuedTimeRatio.getMean());
    };
}
