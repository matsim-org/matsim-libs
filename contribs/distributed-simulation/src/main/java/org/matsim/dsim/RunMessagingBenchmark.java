package org.matsim.dsim;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.HdrHistogram.Histogram;
import org.agrona.BitUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.communication.*;
import picocli.CommandLine;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "distributed-benchmark", mixinStandardHelpOptions = true,
        description = "Benchmark for distributed message passing")
public class RunMessagingBenchmark implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(RunMessagingBenchmark.class);

    @CommandLine.Option(names = {"-r", "--rank"}, description = "Rank of this process", required = true)
    private int rank;

    @CommandLine.Option(names = {"-t", "--total"}, description = "Total number of processes", required = true)
    private int total;

    @CommandLine.Option(names = {"-n", "--number"}, description = "Number of thousand messages to send", defaultValue = "100")
    private int n;

    @CommandLine.Option(names = {"-c", "--communicator"}, description = "Type of communicator", defaultValue = "SHM")
    private RunDistributedSim.Type communicator;

    @CommandLine.Option(names = {"--nodes"}, description = "List of all nodes", defaultValue = "")
    private String nodes;

    @CommandLine.Option(names = {"--address"}, description = "Address of this node", defaultValue = "")
    private String address;

    @CommandLine.Option(names = {"-s", "--size"}, description = "Size of the messages in bytes", defaultValue = "16")
    private long size;

    public static void main(String[] args) {
        new CommandLine(new RunMessagingBenchmark()).execute(args);
    }

    @Override
    public Integer call() throws Exception {

		log.info("Communicator: {}", communicator);
        log.info("Starting distributed benchmark as rank {} of {} and {}k messages ({} bytes)", rank, total, n, size);

		Communicator comm = total == 1 ? new NullCommunicator() : switch (communicator) {
			case AERON -> new AeronCommunicator(rank, total, false, address);
			case AERON_IPC -> new AeronCommunicator(rank, total, true, address);
			case HAZELCAST -> new HazelcastCommunicator(rank, total, Communicator.parseNodeList(address, nodes));
			case SHM -> new SharedMemoryCommunicator(rank, total);
			case LOCAL -> throw new IllegalArgumentException("Local communicator is not supported");
		};

        Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);

		if (size < Integer.BYTES) {
			throw new IllegalArgumentException("Size must be at least 4 bytes");
		}

		MemorySegment data = Arena.ofAuto().allocate(size, BitUtil.CACHE_LINE_LENGTH);
		ByteBuffer bb = data.asByteBuffer();

		bb.putInt(-1);
		// Just put some numbers to not send null messages
		for (int i = 1; i < size / Integer.BYTES; i++) {
			bb.putInt(i);
		}

        int N = n * 1000;

        comm.connect();

        log.info("Connected");

		// Timestamps received from other nodes
		IntList backlog = new IntArrayList();

        for (int k = 0; k < N; k++) {
            long start = System.nanoTime();

			// Received messages
			IntList received = new IntArrayList(backlog);

			// Put messages from backlog into received
			backlog.clear();

			bb.putInt(0, k);

			comm.send(Communicator.BROADCAST_TO_ALL, data, 0, size);

//			System.out.println("Sent: " + k);

			int finalK = k;
			comm.recv(() -> received.size() < total - 1, (buf) -> {
				int seq = buf.getInt(); // seq

//				System.out.println("Received: " + seq + " at " + finalK);

				if (seq == finalK) {
					received.add(seq);
				} else
					backlog.add(seq);
			});

            if (received.size() > total - 1) {
                System.out.println("At k=" + k);
                System.out.println(received);
                throw new IllegalStateException("Received more messages than expected");
            }

            // First messages are warmup
            if (k > 10000)
                histogram.recordValue(System.nanoTime() - start);

            if (k % 10000 == 0)
                log.info("Rank {} of {} has sent {}k messages", rank, total, k / 1_000);

        }

        if (rank == 0)
            histogram.outputPercentileDistribution(System.out, 1000.0);

        comm.close();

        return 0;
    }
}
