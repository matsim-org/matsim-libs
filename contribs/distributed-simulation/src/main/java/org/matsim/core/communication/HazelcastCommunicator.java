package org.matsim.core.communication;

import com.hazelcast.config.Config;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.ICountDownLatch;
import com.hazelcast.topic.ITopic;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.ManyToOneConcurrentLinkedQueue;
import org.agrona.concurrent.OneToOneConcurrentArrayQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Communicator implementation using Hazelcast.
 */
public class HazelcastCommunicator implements Communicator {

	private static final Logger log = LogManager.getLogger(HazelcastCommunicator.class);

	private final int rank;
	private final int size;

	private final HazelcastInstance hz;
	private final IdleStrategy idle = new BusySpinIdleStrategy();

	private final List<ITopic<Object>> topics;

	private final Queue<byte[]> received = new OneToOneConcurrentArrayQueue<>(1024);
	private final Queue<CompletableFuture<Void>> sent = new ManyToOneConcurrentLinkedQueue<>();

	public HazelcastCommunicator(int rank, int size, List<String> nodeList) {
		this.rank = rank;
		this.size = size;

		Config config = Config.load();
		NetworkConfig network = config.getNetworkConfig();
		MulticastConfig multicast = network.getJoin().getMulticastConfig();
		multicast.setEnabled(true);

		if (System.getenv("HZ_MC_PORT") != null) {
			multicast.setMulticastPort(Integer.parseInt(System.getenv("HZ_MC_PORT")));
		}

		if (nodeList != null && !nodeList.isEmpty()) {
			log.info("Using node list: {}", nodeList);

			TcpIpConfig tcp = network.getJoin().getTcpIpConfig();
			tcp.setEnabled(true);
			tcp.setMembers(nodeList);

			multicast.setEnabled(false);
		}

		this.hz = Hazelcast.newHazelcastInstance(config);
		this.topics = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			topics.add(hz.getReliableTopic("rank-" + i));
		}
	}

	@Override
	public int getRank() {
		return rank;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public void connect() throws Exception {
		ICountDownLatch startup = hz.getCPSubsystem().getCountDownLatch("startup");
		startup.trySetCount(size);

		topics.get(rank).addMessageListener(this::onMessage);

		startup.countDown();
		startup.await(10, TimeUnit.MINUTES);
	}

	@Override
	public void close() throws Exception {
		hz.shutdown();
	}

	@Override
	public void send(int receiver, MemorySegment data, long offset, long length) {

		byte[] bytes = data.asSlice(offset, length).toArray(ValueLayout.JAVA_BYTE);

		if (receiver == Communicator.BROADCAST_TO_ALL) {
			for (int i = 0; i < size; i++) {
				if (i != rank) {
//                    topics.get(i).publishAsync(data);
					sent.offer(topics.get(i).publishAsync(bytes).toCompletableFuture());
				}
			}
		} else
//            topics.get(receiver).publish(data);
			sent.offer(topics.get(receiver).publishAsync(bytes).toCompletableFuture());
	}

	@Override
	public void recv(MessageReceiver expectsNext, MessageConsumer handleMsg) {

		if (!expectsNext.expectsMoreMessages())
			return;

		// Wait for all async sends to complete
		CompletableFuture<Void> f;
		while ((f = sent.poll()) != null) {
			sneakyGet(f);
		}

		while (received.isEmpty()) {
			idle.idle();
		}

		idle.reset();

		while (true) {
			byte[] recv = received.poll();
			if (recv != null) {
				sneakyExtract(handleMsg, recv);
			} else {
				if (expectsNext.expectsMoreMessages())
					idle.idle();
				else
					break;
			}
		}
	}

	private static void sneakyExtract(MessageConsumer handleMsg, byte[] recv) {
		try {
			handleMsg.consume(ByteBuffer.wrap(recv));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void sneakyGet(CompletableFuture<Void> f) {
		try {
			f.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private void onMessage(com.hazelcast.topic.Message<Object> msg) {
		byte[] data = (byte[]) msg.getMessageObject();
		received.add(data);
	}
}
