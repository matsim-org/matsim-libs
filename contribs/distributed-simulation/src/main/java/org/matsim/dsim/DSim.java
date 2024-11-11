package org.matsim.dsim;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.HdrHistogram.Histogram;
import org.matsim.api.LP;
import org.matsim.api.LPProvider;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.SimulationNode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.communication.Communicator;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.dsim.executors.LPExecutor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.inject.Key.get;


@Log4j2
public final class DSim implements Mobsim {

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
        this.lps = injector.getInstance(get(new TypeLiteral<>() {}));
		this.timer = new MobsimTimer();
    }

    private static double round(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v))
            return v;

        return new BigDecimal(v).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    @SneakyThrows
    @Override
    public void run() {

        SimulationNode node = injector.getInstance(SimulationNode.class);
        Topology topology = injector.getInstance(Topology.class);
        Config config = injector.getInstance(Config.class);

        LPExecutor executor = injector.getInstance(LPExecutor.class);
		DistributedEventsManager manager = (DistributedEventsManager) injector.getInstance(EventsManager.class);

        List<LPTask> tasks = new ArrayList<>();

        for (LPProvider lpp : lps) {
            for (int part : node.getParts()) {

                LP lp = lpp.create(part);

                // Skip if no LP is created
                if (lp == null)
                    continue;

                log.info("Creating lp {} rank:{} partition:{}", lpp.getClass().getName(), node.getRank(), part);

                LPTask task = executor.register(lp, manager, part);
                broker.register(task, part);

                tasks.add(task);
                injector.injectMembers(lp);
            }
        }

        manager.initProcessing();

		timer.setSimStartTime(config.qsim().getStartTime().orElse(0));
		timer.setTime(timer.getSimStartTime());

        Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(1), 3);

        log.info("Starting simulation");

        long start = System.currentTimeMillis();

		double time = timer.getTimeOfDay();
        while (timer.getTimeOfDay() < config.qsim().getEndTime().orElse(86400)) {

            long t = System.nanoTime();

            if (time % 3600 == 0) {
                var hour = (int) time / 3600;
                var formattedDuration = String.format("%02d:00:00", hour);
                log.info("#{} at sim step: {}", comm.getRank(), formattedDuration);
            }

            manager.beforeSimStep(time);
            broker.beforeSimStep(time);

            try {
                executor.doSimStep(time);
            } catch (Throwable e) {
                log.error("Error in simulation step: %.2fs".formatted(time), e);
                break;
            }

            try {
                histogram.recordValue(System.nanoTime() - t);
            } catch (ArrayIndexOutOfBoundsException e) {
                // Overflow in histogram is ignored
            }

            manager.afterSimStep(time);
            broker.syncTimestep(time, false);

			time = timer.incrementTime();
        }

		manager.finishProcessing();

        double mu = histogram.getMean() / 1000;

        // simulated / real
        long runtime = System.currentTimeMillis() - start;
        double rtr = config.qsim().getEndTime().seconds() * 1000 / runtime;

        log.info("Mean time per second: {} μs, Real-time-ratio: {} ",
                round(mu),
                round(rtr)
        );

        writeRuntimeStats(topology.getTotalPartitions(), node.getRank(), runtime, rtr);
        writeRuntimes(node, histogram, broker.getRuntime(), executor, runtime);

        // Simulation tasks are deregistered after execution
        for (LPTask task : tasks) {
            broker.deregister(task);
            executor.deregister(task);
        }
    }

    @SneakyThrows
    private void writeRuntimeStats(int size, int rank, long runtimeMillis, double rtr) {
        OutputDirectoryHierarchy io = injector.getInstance(OutputDirectoryHierarchy.class);
        Path out = Path.of(io.getOutputPath(), "runtimes-%d".formatted(rank));
        Files.createDirectories(out);
        try (BufferedWriter writer = Files.newBufferedWriter(out.resolve("runtime-%d.csv".formatted(rank)))) {
            writer.write("size,rank,runtime,rtr\n");
            writer.write(size + "," + rank + "," + runtimeMillis + "," + rtr + "\n");
        }
    }

    @SneakyThrows
    private void writeRuntimes(SimulationNode node, Histogram simulation, Histogram broker, LPExecutor executor, long overallRuntime) {

        OutputDirectoryHierarchy io = injector.getInstance(OutputDirectoryHierarchy.class);

        Path out = Path.of(io.getOutputPath(), "runtimes-%d".formatted(node.getRank()));

        Files.createDirectories(out);

        try (PrintStream writer = new PrintStream(Files.newOutputStream(out.resolve("simulation.hgrm")))) {
            simulation.outputPercentileDistribution(writer, 1000.0);
        }

        try (PrintStream writer = new PrintStream(Files.newOutputStream(out.resolve("broker.hgrm")))) {
            broker.outputPercentileDistribution(writer, 1000.0);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(out.resolve("tasks.csv"))) {

            writer.write("name,partition,step,runtime\n");
            writer.write("OverallRuntime,-1,-1," + overallRuntime + "\n");
            executor.processRuntimes(info -> {
                LongList steps = info.runtime();
                for (int i = 0; i < steps.size(); i++) {
                    try {
                        long runtime = steps.getLong(i);
                        if (runtime == 0)
                            continue;

                        writer.write("\"%s\",%d,%d,%d\n".formatted(info.name(), info.partition(), i, runtime));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }
    }
}
