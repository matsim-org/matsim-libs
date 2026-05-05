package org.matsim.core.mobsim.qsim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

/**
 * This module measures the runtime of the QSim during the last iteration and writes it to a CSV file.
 */
@SuppressWarnings({"rawtypes", "unused"})
public final class QSimTimingModule extends AbstractModule {

	@Override
	public void install() {
		// bind the timer, so that it can be injected into CheckForLastIteration
		// use singleton scope, because, we want the same timer everywhere.
		bind(Timer.class).in(Singleton.class);
		addMobsimListenerBinding().to(Timer.class);
		addControllerListenerBinding().to(CheckForLastIteration.class);
	}

	private static final class CheckForLastIteration implements BeforeMobsimListener {

		private final Timer timer;
		private final Config config;

		@Inject
		private CheckForLastIteration(Config config, Timer timer) {
			this.timer = timer;
			this.config = config;
		}

		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent e) {
			// using e.getIsLastIteration, does not yield the correct result somehow
			this.timer.setIsLastIteration(config.controller().getLastIteration() == e.getIteration());
		}
	}

	private static final class Timer implements MobsimInitializedListener, MobsimBeforeCleanupListener {
		private Instant start;
		private boolean isLastIteration;

		private final Config config;
		private final OutputDirectoryHierarchy outDir;

		@Inject
		private Timer(Config config, OutputDirectoryHierarchy outDir) {
			this.config = config;
			this.outDir = outDir;
		}

		void setIsLastIteration(boolean isLastIteration) {
			this.isLastIteration = isLastIteration;
		}

		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			if (isLastIteration) {
				start = Instant.now();
			}
		}

		@Override
		public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
			if (isLastIteration) {
				Instant now = Instant.now();
				Duration duration = Duration.between(start, now);
				int size = config.qsim().getNumberOfThreads();
				Path filename = Paths.get(outDir.getOutputFilename("runtimes.csv"));
				try (BufferedWriter writer = Files.newBufferedWriter(filename); var p = new CSVPrinter(writer, createWriteFormat("size", "rank", "runtime", "rtr"))) {
					double rtr = config.qsim().getEndTime().seconds() / duration.toSeconds();
					p.printRecord(size, 0, duration.toMillis(), rtr);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		}

		private static CSVFormat createWriteFormat(String... header) {
			return CSVFormat.DEFAULT.builder()
				.setHeader(header)
				.setSkipHeaderRecord(false)
				.get();
		}
	}
}
