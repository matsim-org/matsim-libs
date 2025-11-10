package org.matsim.dsim;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import it.unimi.dsi.fastutil.longs.LongList;
import org.HdrHistogram.Histogram;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.LPProvider;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.communication.Communicator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.MobsimListenerManager;
import org.matsim.dsim.executors.LPExecutor;
import org.matsim.dsim.simulation.SimProvider;
import org.matsim.vis.snapshotwriters.SnapshotWriterManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.inject.Key.get;


public final class DSim implements Mobsim {

	private static final Logger log = LogManager.getLogger(DSim.class);

	private final Injector injector;
	private final Communicator comm;
	private final MessageBroker broker;
	private final Set<LPProvider> lps;
	private final MobsimTimer timer;

	@Inject
	public DSim(Injector injector) {
		this.injector = injector;
		this.comm = injector.getInstance(Communicator.class);
		this.broker = injector.getInstance(MessageBroker.class);
		this.lps = injector.getInstance(get(new TypeLiteral<>() {
		}));
		this.timer = new MobsimTimer();
	}

	private static double round(double v) {
		if (Double.isNaN(v) || Double.isInfinite(v))
			return v;

		return new BigDecimal(v).setScale(3, RoundingMode.HALF_UP).doubleValue();
	}

	@Override
	public void run() {

		ComputeNode computeNode = injector.getInstance(ComputeNode.class);
		Topology topology = injector.getInstance(Topology.class);
		Config config = injector.getInstance(Config.class);
		DSimConfigGroup dsimConfig = ConfigUtils.addOrGetModule(config, DSimConfigGroup.class);

		LPExecutor executor = injector.getInstance(LPExecutor.class);
		DistributedEventsManager manager = (DistributedEventsManager) injector.getInstance(EventsManager.class);

		List<LPTask> tasks = new ArrayList<>();
		Set<MobsimListener> listeners = new HashSet<>();

		for (LPProvider lpp : lps) {
			for (int part : computeNode.getParts()) {

				LP lp = lpp.create(part);

				// Skip if no LP is created
				if (lp == null)
					continue;

				log.info("Creating lp {} rank:{} partition:{}", lpp.getClass().getName(), computeNode.getRank(), part);

				LPTask task = executor.register(lp, manager, part);
				broker.register(task, part);

				tasks.add(task);
				injector.injectMembers(lp);
			}

			if (lpp instanceof SimProvider p) {
				listeners.addAll(p.getListeners());
			}
		}

		// Manager for node singletons listeners
		MobsimListenerManager listenerManager = new MobsimListenerManager(this);

		// Incompatible, they do unsupported cast
		listeners.removeIf(l -> l instanceof SnapshotWriterManager);

		listeners.forEach(listenerManager::addQueueSimulationListener);

		// Sync after all components have been added
		manager.syncEventRegistry(comm);

		// Event handler without partition may be executed within the context of the first one
		manager.setContext(computeNode.getParts().getFirst());

		manager.initProcessing();

		timer.setSimStartTime(dsimConfig.getStartTime());
		timer.setTime(timer.getSimStartTime());

		Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(1), 3);

		log.info("Starting simulation");

		listenerManager.fireQueueSimulationInitializedEvent();

		long start = System.currentTimeMillis();

		// Time spent in other stages than mobsim
		long beforeListener = 0;
		long syncStep = 0;
		long afterListener = 0;

		double time = timer.getTimeOfDay();
		while (timer.getTimeOfDay() < dsimConfig.getEndTime()) {

			long t = System.nanoTime();

			if (time % 3600 == 0) {
				var hour = (int) time / 3600;
				var formattedDuration = String.format("%02d:00:00", hour);
				log.info("#{} at sim step: {}", comm.getRank(), formattedDuration);
			}

			long t1 = System.currentTimeMillis();

			listenerManager.fireQueueSimulationBeforeSimStepEvent(time);
			beforeListener += System.currentTimeMillis() - t1;

			manager.beforeSimStep(time);
			broker.beforeSimStep(time);

			try {
				executor.doSimStep(time);
			} catch (Throwable e) {
				log.error("Error in simulation step: %.2fs".formatted(time), e);
				throw e;
			}

			try {
				histogram.recordValue(System.nanoTime() - t);
			} catch (ArrayIndexOutOfBoundsException e) {
				// Overflow in histogram is ignored
			}

			manager.afterSimStep(time);

			long t2 = System.currentTimeMillis();

			broker.syncTimestep(time, false);
			syncStep += System.currentTimeMillis() - t2;

			long t3 = System.currentTimeMillis();
			listenerManager.fireQueueSimulationAfterSimStepEvent(time);
			afterListener += System.currentTimeMillis() - t3;

			time = timer.incrementTime();
		}

		manager.finishProcessing();

		executor.afterSim();

		double mu = histogram.getMean() / 1000;

		// simulated / real
		long runtime = System.currentTimeMillis() - start;
		double rtr = dsimConfig.getEndTime() * 1000 / runtime;

		log.info("Mean time per second: {} μs, Real-time-ratio: {} ",
			round(mu),
			round(rtr)
		);

		listenerManager.fireQueueSimulationBeforeCleanupEvent();

		writeRuntimeStats(topology.getTotalPartitions(), computeNode.getRank(), runtime, rtr);
		writeRuntimes(computeNode, histogram, broker.getRuntime(), broker.getSizes(), executor,
			runtime, beforeListener, afterListener, syncStep);

		// Simulation tasks are deregistered after execution
		for (LPTask task : tasks) {
			broker.deregister(task);
			executor.deregister(task);
		}

		broker.afterSim();
	}

	private void writeRuntimeStats(int size, int rank, long runtimeMillis, double rtr) {
		OutputDirectoryHierarchy io = injector.getInstance(OutputDirectoryHierarchy.class);
		Path out = Path.of(io.getOutputPath(), "runtimes-%d".formatted(rank));
		try {
			Files.createDirectories(out);
			try (BufferedWriter writer = Files.newBufferedWriter(out.resolve("runtime-%d.csv".formatted(rank)))) {
				writer.write("size,rank,runtime,rtr\n");
				writer.write(size + "," + rank + "," + runtimeMillis + "," + rtr + "\n");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeRuntimes(ComputeNode node, Histogram simulation, Histogram broker, Histogram sizes, LPExecutor executor,
							   long overallRuntime, long beforeListener, long afterListener, long syncStep) {

		OutputDirectoryHierarchy io = injector.getInstance(OutputDirectoryHierarchy.class);

		Path out = Path.of(io.getOutputPath(), "runtimes-%d".formatted(node.getRank()));

		try {
			Files.createDirectories(out);

			try (PrintStream writer = new PrintStream(Files.newOutputStream(out.resolve("simulation.hgrm")))) {
				simulation.outputPercentileDistribution(writer, 1000.0);
			}

			try (PrintStream writer = new PrintStream(Files.newOutputStream(out.resolve("broker.hgrm")))) {
				broker.outputPercentileDistribution(writer, 1000.0);
			}

			try (PrintStream writer = new PrintStream(Files.newOutputStream(out.resolve("msgSizes.hgrm")))) {
				sizes.outputPercentileDistribution(writer, 1.0);
			}

			try (BufferedWriter writer = Files.newBufferedWriter(out.resolve("tasks.csv"))) {

				writer.write("name,partition,step,runtime\n");
				writer.write("OverallRuntime,-1,-1," + overallRuntime + "\n");
				writer.write("BeforeSimStepListener,-1,-1," + beforeListener + "\n");
				writer.write("AfterSimStepListener,-1,-1," + afterListener + "\n");
				writer.write("SyncStep,-1,-1," + syncStep + "\n");

				executor.processRuntimes(info -> {
					LongList steps = info.runtime();
					for (int i = 0; i < steps.size(); i++) {
						try {
							long runtime = steps.getLong(i);
							if (runtime == 0)
								continue;

							// Runtimes are collected as 10% samples currently
							writer.write("\"%s\",%d,%d,%d\n".formatted(info.name(), info.partition(), i * 10, runtime));
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}
				});
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
