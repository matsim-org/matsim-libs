package org.matsim.contrib.drt.optimizer.abort;

import com.google.inject.Inject;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.util.HashMap;
import java.util.Map;

public class DrtRejectionEventHandler implements PassengerRequestRejectedEventHandler,
	PassengerRequestSubmittedEventHandler, IterationEndsListener {
	private static final Logger log = LogManager.getLogger(DrtRejectionEventHandler.class );
	private final Map<Integer, MutableInt> numberOfRejectionsPerTimeBin = new HashMap<>();
	private final Map<Integer, MutableInt> numberOfSubmissionsPerTimeBin = new HashMap<>();
	private final Map<Integer, Double> probabilityOfRejectionPerTimeBin = new HashMap<>();

	// Key parameters
	// TODO: consider make them configurable
	private final double timeBinSize = 900;
	// Time bin to analyze the probability of being rejected
	private final double rejectionCost = 6;
	// 6 -> 1 hour of default performing score
	private final double learningRate = 0.25;
	// (1 - alpha) * old probability + alpha * new probability (0 < alpha <= 1)

	@Inject
	private EventsManager events;

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		// Currently, we assume there is only 1 DRT operator with standard DRT mode ("drt")
		// Because it is a little tricky to get DRT Config Group here (which can only be acquired via DvrpQSimModule),
		// we just use the simple way. For multi-operator, a map can be introduced to store the data for different DRT modes
		if (event.getMode().equals(TransportMode.drt)) {
			int timeBin = getTimeBin(event.getTime());
			numberOfRejectionsPerTimeBin.computeIfAbsent(timeBin, c -> new MutableInt()).increment();
		}
	}

	@Override
	public void reset(int iteration) {
		PassengerRequestRejectedEventHandler.super.reset(iteration);
		if (iteration != 0) {
			log.debug(probabilityOfRejectionPerTimeBin.values());
			numberOfSubmissionsPerTimeBin.clear();
			numberOfRejectionsPerTimeBin.clear();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// Calculate the probability of being rejected at each time bin
		for (Integer timeBin : numberOfSubmissionsPerTimeBin.keySet()) {
			double probability = numberOfRejectionsPerTimeBin.getOrDefault(timeBin, new MutableInt()).doubleValue() /
				numberOfSubmissionsPerTimeBin.get(timeBin).doubleValue();
			// Apply exponential discount
			probability = learningRate * probability + (1 - learningRate) * probabilityOfRejectionPerTimeBin.getOrDefault(timeBin, 0.);
			probabilityOfRejectionPerTimeBin.put(timeBin, probability);
		}
	}

	@Override
	public void handleEvent(PassengerRequestSubmittedEvent event) {
		if (event.getMode().equals(TransportMode.drt)) {
			int timeBin = getTimeBin(event.getTime());
			numberOfSubmissionsPerTimeBin.computeIfAbsent(timeBin, c -> new MutableInt()).increment();

			// Add a cost for potential rejection
			double extraScore = (-1) * rejectionCost * probabilityOfRejectionPerTimeBin.getOrDefault(getTimeBin(event.getTime()), 0.);
			events.processEvent(new PersonScoreEvent(event.getTime(), event.getPersonId(), extraScore, "Potential_of_being_rejected"));
		}
	}

	private int getTimeBin(double time) {
		return (int) (Math.floor(time / timeBinSize));
	}

}
