package org.matsim.contrib.etaxi.run;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.QSimScopeObjectListener;
import org.matsim.contrib.etaxi.util.ETaxiStats;
import org.matsim.contrib.etaxi.util.ETaxiStatsCalculator;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.io.IOUtils;

public class ETaxiBenchmarkStats
		implements ShutdownListener, MobsimBeforeCleanupListener, QSimScopeObjectListener<Fleet> {
	public static final String[] HEADER = { "n", "m", //
			"QueuedTimeRatio_fleetAvg" };

	private final OutputDirectoryHierarchy controlerIO;

	private final SummaryStatistics queuedTimeRatio = new SummaryStatistics();

	private Fleet fleet;

	public ETaxiBenchmarkStats(OutputDirectoryHierarchy controlerIO) {
		this.controlerIO = controlerIO;
	}

	@Override
	public void objectCreated(Fleet fleet) {
		this.fleet = fleet;
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		ETaxiStats singleRunEStats = new ETaxiStatsCalculator(fleet.getVehicles().values()).getDailyEStats();
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
			writer.writeNext(new CSVLineBuilder()//
					.addf("%.3f", queuedTimeRatio.getMean()));
		}
	}
}
