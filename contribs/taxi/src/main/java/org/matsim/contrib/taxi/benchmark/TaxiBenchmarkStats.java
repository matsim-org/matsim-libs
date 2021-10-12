package org.matsim.contrib.taxi.benchmark;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.contrib.dvrp.analysis.ExecutedScheduleCollector;
import org.matsim.contrib.taxi.analysis.TaxiEventSequenceCollector;
import org.matsim.contrib.taxi.util.stats.TaxiStats;
import org.matsim.contrib.taxi.util.stats.TaxiStatsCalculator;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;

public class TaxiBenchmarkStats implements ShutdownListener, AfterMobsimListener {
	public static final String[] HEADER = { //
			"PassWaitTime_avg", //
			"PassWaitTime_95%ile", //
			"PassWaitTime_max", //
			"EmptyDriveRatio_fleetAvg", //
			"StayRatio_fleetAvg" };

	private final OutputDirectoryHierarchy controlerIO;

	private final SummaryStatistics passengerWaitTime = new SummaryStatistics();
	private final SummaryStatistics pc95PassengerWaitTime = new SummaryStatistics();
	private final SummaryStatistics maxPassengerWaitTime = new SummaryStatistics();

	private final SummaryStatistics emptyDriveRatio = new SummaryStatistics();
	private final SummaryStatistics stayRatio = new SummaryStatistics();

	private final ExecutedScheduleCollector executedScheduleCollector;
	private final TaxiEventSequenceCollector taxiEventSequenceCollector;

	public TaxiBenchmarkStats(OutputDirectoryHierarchy controlerIO, ExecutedScheduleCollector executedScheduleCollector,
			TaxiEventSequenceCollector taxiEventSequenceCollector) {
		this.controlerIO = controlerIO;
		this.executedScheduleCollector = executedScheduleCollector;
		this.taxiEventSequenceCollector = taxiEventSequenceCollector;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		TaxiStats singleRunStats = new TaxiStatsCalculator(executedScheduleCollector.getExecutedSchedules(),
				taxiEventSequenceCollector.getRequestSequences().values()).getDailyStats();

		passengerWaitTime.addValue(singleRunStats.passengerWaitTime.getMean());
		pc95PassengerWaitTime.addValue(singleRunStats.passengerWaitTime.getPercentile(95));
		maxPassengerWaitTime.addValue(singleRunStats.passengerWaitTime.getMax());

		singleRunStats.calculateFleetEmptyDriveRatio().ifPresent(emptyDriveRatio::addValue);
		singleRunStats.calculateFleetStayRatio().ifPresent(stayRatio::addValue);
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		writeFile("benchmark_stats.txt", HEADER);
	}

	private void writeFile(String file, String[] header) {
		try (CompactCSVWriter writer = new CompactCSVWriter(
				IOUtils.getBufferedWriter(controlerIO.getOutputFilename(file)))) {
			writer.writeNext(header);
			writer.writeNext(new CSVLineBuilder()//
					.addf("%.1f", passengerWaitTime.getMean())
					.addf("%.0f", pc95PassengerWaitTime.getMean())
					.addf("%.0f", maxPassengerWaitTime.getMean())
					.addf("%.3f", emptyDriveRatio.getMean())
					.addf("%.3f", stayRatio.getMean()));
		}
	}
}
