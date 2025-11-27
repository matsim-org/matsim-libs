package org.matsim.core.communication;

import io.aeron.*;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import io.aeron.samples.SampleConfiguration;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AeronCommunicator implements Communicator {

	private static final String IPC = "aeron:ipc?tags=%d";
	private static final String CHANNEL = "aeron:udp?endpoint=224.0.%d.1:40123|interface=localhost";
	private static final String UDP_CHANNEL = "aeron:udp?endpoint=localhost:%d";


	private final Aeron.Context ctx;
	private final MediaDriver driver;
	private final int rank;
	private final int size;
	private final String address;
	private final ControlledFragmentAssembler fragmentAssembler;
	private final FragmentHandler fragmentHandler;

	/**
	 * Latch to wait for the startup of the communicator.
	 */
	private final CountDownLatch startup;
	private final List<Subscription> subscriptions;

	private final IdleStrategy idle = new BusySpinIdleStrategy();

	private Aeron aeron;
	private Publication publication;

	public AeronCommunicator(int rank, int size, boolean ipc, String address) {
		this.rank = rank;
		this.size = size;
		this.driver = ipc ? null : MediaDriver.launchEmbedded();
		this.ctx = new Aeron.Context()
			.aeronDirectoryName(ipc ? CommonContext.getAeronDirectoryName() : driver.aeronDirectoryName())
			.availableImageHandler(this::imageHandler);
		this.address = address;
		this.startup = new CountDownLatch(size - 1);
		this.subscriptions = new ArrayList<>(size - 1);
		this.fragmentHandler = new FragmentHandler();
		this.fragmentAssembler = new ControlledFragmentAssembler(fragmentHandler);
	}

	public void connect() throws Exception {
		aeron = Aeron.connect(ctx);

		// With 2 ranks, direct udp connection can be used
		boolean unicast = false;
		String channel = driver == null ? IPC : unicast ? UDP_CHANNEL : CHANNEL;
		int port = unicast ? 40100 : 1;

		if (!address.isBlank())
			channel = channel.replace("localhost", address);

		publication = aeron.addExclusivePublication(String.format(channel, port + rank), rank);
		for (int i = 0; i < size; i++) {

			if (i == rank) {
				continue;
			}

			Subscription subs = aeron.addSubscription(String.format(channel, port + i), i);
			subscriptions.add(subs);
		}

		startup.await();
	}

	@Override
	public void close() throws Exception {

		// At the moment it is not ensured that all messages have been received, just wait and hope in the prototype
		Thread.sleep(2000);
		subscriptions.forEach(Subscription::close);
		publication.close();
		aeron.close();
		CloseHelper.close(driver);
	}

	private void imageHandler(Image image) {
		final Subscription subscription = image.subscription();

		System.out.printf(
			"Available image on %s streamId=%d sessionId=%d mtu=%d term-length=%d from %s%n",
			subscription.channel(), subscription.streamId(), image.sessionId(), image.mtuLength(),
			image.termBufferLength(), image.sourceIdentity());

		startup.countDown();
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
	public void send(int receiver, MemorySegment data, long offset, long length) {

		int len = (int) length;
		UnsafeBuffer buffer = new UnsafeBuffer(data.address(), len);

		while (true) {
			long ret = publication.offer(buffer, 0, len, null);
			if (ret >= 0)
				break;

			if (ret == Publication.NOT_CONNECTED || ret == Publication.BACK_PRESSURED || ret == Publication.ADMIN_ACTION) {
				idle.idle();
				continue;
			}

			if (ret == Publication.CLOSED)
				throw new IllegalStateException("Publication is closed");

			if (ret == Publication.MAX_POSITION_EXCEEDED)
				throw new IllegalStateException("Publication max position exceeded");
		}

		idle.reset();
	}

	@Override
	public void recv(MessageReceiver expectsNext, MessageConsumer handleMsg) {

		fragmentHandler.delegate = (buffer, offset, length, header) -> {
			ByteBuffer bb = buffer.byteBuffer();

			if (bb == null)
				bb = ByteBuffer.wrap(buffer.byteArray(), offset, length);
			else
				bb = bb.slice(offset, length);

			try {
				handleMsg.consume(bb);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			return ControlledFragmentHandler.Action.COMMIT;
		};

		while (expectsNext.expectsMoreMessages()) {
			int fragments = 0;
			for (Subscription subscription : subscriptions) {
				fragments += subscription.controlledPoll(fragmentAssembler, SampleConfiguration.FRAGMENT_COUNT_LIMIT);
			}

			idle.idle(fragments);
		}

		idle.reset();
	}


	private static final class FragmentHandler implements ControlledFragmentHandler {

		private ControlledFragmentHandler delegate;

		@Override
		public Action onFragment(DirectBuffer buffer, int offset, int length, Header header) {
			return delegate.onFragment(buffer, offset, length, header);
		}
	}
}
