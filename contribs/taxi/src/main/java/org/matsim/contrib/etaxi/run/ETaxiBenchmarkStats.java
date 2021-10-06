package org.matsim.contrib.etaxi.run;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.etaxi.util.ETaxiStats;
import org.matsim.contrib.etaxi.util.ETaxiStatsCalculator;
import org.matsim.contrib.ev.charging.ChargingEventSequenceCollector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;

public class ETaxiBenchmarkStats implements ShutdownListener, AfterMobsimListener {
	public static final String[] HEADER = { "QueuedTimeRatio_fleetAvg" };

	private final OutputDirectoryHierarchy controlerIO;
	private final ChargingEventSequenceCollector chargingEventSequenceCollector;
	private final FleetSpecification fleetSpecification;

	private final SummaryStatistics queuedTimeRatio = new SummaryStatistics();

	public ETaxiBenchmarkStats(OutputDirectoryHierarchy controlerIO,
			ChargingEventSequenceCollector chargingEventSequenceCollector, FleetSpecification fleetSpecification) {
		this.controlerIO = controlerIO;
		this.chargingEventSequenceCollector = chargingEventSequenceCollector;
		this.fleetSpecification = fleetSpecification;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		ETaxiStats singleRunEStats = new ETaxiStatsCalculator(chargingEventSequenceCollector.getCompletedSequences(),
				fleetSpecification).getDailyEStats();
		singleRunEStats.getFleetQueuedTimeRatio().ifPresent(queuedTimeRatio::addValue);
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		writeFile("ebenchmark_stats.txt", HEADER);
	}

	private void writeFile(String file, String[] header) {
		try (CompactCSVWriter writer = new CompactCSVWriter(
				IOUtils.getBufferedWriter(controlerIO.getOutputFilename(file)))) {
			writer.writeNext(header);
			writer.writeNext(new CSVLineBuilder().addf("%.3f", queuedTimeRatio.getMean()));
		}
	}
}
