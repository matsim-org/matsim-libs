package org.matsim;

import com.google.inject.Inject;
import org.matsim.api.core.v01.messages.ScoringMessage;
import org.matsim.core.communication.Communicator;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.serialization.SerializationProvider;

import java.util.List;

/**
 * Listener to distribute scores across nodes.
 */
public class DistributedScoringListener implements IterationEndsListener {

	@Inject
	private Communicator comm;

	@Inject
	private SerializationProvider serializer;

	@Override
	public double priority() {
		return 100;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		List<ScoringMessage> all = comm.allGather(new ScoringMessage(), 10, serializer);


		System.err.println(all.size());

		// TODO: score assigner


	}
}
