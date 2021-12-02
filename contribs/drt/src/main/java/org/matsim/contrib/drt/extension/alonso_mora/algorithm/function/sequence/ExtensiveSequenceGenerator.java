package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop.StopType;

/**
 * The extensive sequence generator as described by Alonso-Mora et al.
 * 
 * The generator loops through all possible combinations of pickup and dropoff
 * stops given the onboard and new requests for the vehicle.
 * 
 * @author sebhoerl
 */
public class ExtensiveSequenceGenerator implements SequenceGenerator {
	private int[] currentSequence;
	private int currentIndex;

	private int sequenceLength;

	private int[] requestIndices;
	private boolean[] pickupIndices;

	private final List<AlonsoMoraStop> stops;
	private Set<Integer> onboardIndices = new HashSet<>();

	private boolean finished = false;

	public ExtensiveSequenceGenerator(Collection<AlonsoMoraRequest> onboardRequests,
			Collection<AlonsoMoraRequest> requests) {
		this.sequenceLength = onboardRequests.size() + requests.size() * 2;

		this.currentSequence = new int[this.sequenceLength];
		this.currentIndex = 0;

		this.requestIndices = new int[sequenceLength];
		this.pickupIndices = new boolean[sequenceLength];

		this.stops = new ArrayList<>(this.sequenceLength);

		int requestIndex = 0;
		int stopIndex = 0;

		for (AlonsoMoraRequest request : onboardRequests) {
			onboardIndices.add(requestIndex);

			requestIndices[stopIndex] = requestIndex;
			pickupIndices[stopIndex] = false;
			stops.add(new AlonsoMoraStop(StopType.Dropoff, request.getDropoffLink(), request));
			stopIndex++;

			requestIndex++;
		}

		for (AlonsoMoraRequest request : requests) {
			requestIndices[stopIndex] = requestIndex;
			pickupIndices[stopIndex] = true;
			stops.add(new AlonsoMoraStop(StopType.Pickup, request.getPickupLink(), request));
			stopIndex++;

			requestIndices[stopIndex] = requestIndex;
			pickupIndices[stopIndex] = false;
			stops.add(new AlonsoMoraStop(StopType.Dropoff, request.getDropoffLink(), request));
			stopIndex++;

			requestIndex++;
		}

		internalAdvance(false);
	}

	@Override
	public void advance() {
		internalAdvance(true);
	}

	@Override
	public void abort() {
		internalAbort();
		internalAdvance(false);
	}

	void internalAdvance(boolean dropCurrent) {
		while (!finished && (!isFeasible() || dropCurrent)) {
			if (currentIndex < sequenceLength - 1 && isFeasible()) {
				currentIndex++;
				currentSequence[currentIndex] = 0;
			} else if (currentSequence[currentIndex] < sequenceLength - 1) {
				currentSequence[currentIndex]++;
			} else {
				internalAbort();
			}

			dropCurrent = false;
		}
	}

	void internalAbort() {
		while (currentIndex > 0) {
			currentIndex--;

			if (currentSequence[currentIndex] < sequenceLength - 1) {
				currentSequence[currentIndex]++;
				return;
			}
		}

		if (currentSequence[0] < sequenceLength - 1) {
			currentSequence[0]++;
			return;
		}

		finished = true;
	}

	boolean isFeasible() {
		Set<Integer> pickedUp = new HashSet<>(onboardIndices);
		Set<Integer> droppedOff = new HashSet<>();

		for (int i = 0; i <= currentIndex; i++) {
			int stopIndex = currentSequence[i];
			int requestIndex = requestIndices[stopIndex];

			if (pickupIndices[stopIndex]) {
				if (pickedUp.contains(requestIndex)) {
					return false;
				} else {
					pickedUp.add(requestIndex);
				}
			} else {
				if (droppedOff.contains(requestIndex)) {
					return false;
				} else if (pickedUp.contains(requestIndex)) {
					droppedOff.add(requestIndex);
				} else {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public boolean hasNext() {
		return !finished;
	}

	@Override
	public boolean isComplete() {
		return currentIndex == sequenceLength - 1;
	}

	@Override
	public List<AlonsoMoraStop> get() {
		return IntStream.of(currentSequence).limit(currentIndex + 1).mapToObj(i -> stops.get(i))
				.collect(Collectors.toList());
	}

	static public class Factory implements SequenceGeneratorFactory {
		@Override
		public SequenceGenerator createGenerator(AlonsoMoraVehicle vehicle,
				Collection<AlonsoMoraRequest> onboardRequests, Collection<AlonsoMoraRequest> requests, double now) {
			return new ExtensiveSequenceGenerator(onboardRequests, requests);
		}
	}
}
