package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop.StopType;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * A simplified sequence generator that is not included in the paper by
 * Alonso-Mora et al., which is based on a best-response Euclidean distance
 * search. This implementation was used in AMoDeus and the papers by Ruch et al.
 * on ridesharing.
 * 
 * Given the current position of the vehicle, the closest stop in terms of
 * Euclidean distance is added as the first element of the route. After that,
 * the next closest element to the added stop is added, and so on. In case a
 * sequence is aborted due to constraint violations, the second closest element
 * is added, and so on.
 * 
 * @author sebhoerl
 */
public class EuclideanSequenceGenerator implements SequenceGenerator {
	private List<AlonsoMoraStop> stops;

	private LinkedList<Integer> sequence = new LinkedList<>();
	private LinkedList<Integer> candidates = new LinkedList<>();
	private LinkedList<Integer> aborted = new LinkedList<>();

	private BitSet dropoffMask;
	private BitSet onboardMask;

	private int numberOfStops;
	private Link vehicleLink;

	private boolean finished = false;

	public EuclideanSequenceGenerator(Link vehicleLink, Collection<AlonsoMoraRequest> onboardRequests,
			Collection<AlonsoMoraRequest> requests) {
		this.numberOfStops = onboardRequests.size() + requests.size() * 2;
		this.vehicleLink = vehicleLink;

		this.dropoffMask = new BitSet(numberOfStops);
		this.dropoffMask.set(0, numberOfStops, false);

		this.onboardMask = new BitSet(numberOfStops);
		this.onboardMask.set(0, numberOfStops, false);

		this.sequence = new LinkedList<>();

		int stopIndex = 0;
		stops = new ArrayList<>(numberOfStops);

		for (AlonsoMoraRequest request : onboardRequests) {
			stops.add(new AlonsoMoraStop(StopType.Dropoff, request.getDropoffLink(), request));
			onboardMask.set(stopIndex);
			stopIndex++;
		}

		for (AlonsoMoraRequest request : requests) {
			stops.add(new AlonsoMoraStop(StopType.Pickup, request.getPickupLink(), request));
			stopIndex++;

			stops.add(new AlonsoMoraStop(StopType.Dropoff, request.getDropoffLink(), request));
			dropoffMask.set(stopIndex);
			stopIndex++;
		}

		IntStream.range(0, numberOfStops).forEach(candidates::add);
		sortCandidates();
	}

	private void sortCandidates() {
		Link currentLink = sequence.size() == 0 ? //
				vehicleLink : stops.get(sequence.get(sequence.size() - 1)).getLink();

		// Precaching the distances for sorting

		List<Double> distances = new ArrayList<>(Collections.nCopies(stops.size(), Double.POSITIVE_INFINITY));

		for (int index : candidates) {
			double distance = CoordUtils.calcEuclideanDistance(currentLink.getCoord(),
					stops.get(index).getLink().getCoord());
			distances.set(index, distance);
		}

		Collections.sort(candidates, (a, b) -> {
			return Double.compare(distances.get(a), distances.get(b));
		});

		while (!checkFirstIsValid()) {
			candidates.add(candidates.removeFirst());
		}
	}

	private boolean checkFirstIsValid() {
		int firstIndex = candidates.getFirst();

		if (dropoffMask.get(firstIndex) && !onboardMask.get(firstIndex)) {
			int pickupIndex = firstIndex - 1;

			if (!sequence.contains(pickupIndex)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void advance() {
		sequence.add(candidates.removeFirst());

		candidates.addAll(aborted);
		aborted.clear();

		if (candidates.size() == 0) {
			finished = true;
		} else {
			sortCandidates();

			if (!checkFirstIsValid()) {
				abort();
			}
		}
	}

	@Override
	public void abort() {
		aborted.add(candidates.removeFirst());

		while (candidates.size() > 0 && !checkFirstIsValid()) {
			aborted.add(candidates.removeFirst());
		}

		if (candidates.size() == 0) {
			finished = true;
		}
	}

	@Override
	public boolean hasNext() {
		return !finished;
	}

	@Override
	public List<AlonsoMoraStop> get() {
		List<AlonsoMoraStop> result = new ArrayList<>(sequence.size() + 1);
		result.addAll(sequence.stream().map(i -> stops.get(i)).collect(Collectors.toList()));
		result.add(stops.get(candidates.getFirst()));
		return result;
	}

	@Override
	public boolean isComplete() {
		return sequence.size() + 1 == numberOfStops;
	}

	static public class Factory implements SequenceGeneratorFactory {
		@Override
		public SequenceGenerator createGenerator(AlonsoMoraVehicle vehicle,
				Collection<AlonsoMoraRequest> onboardRequests, Collection<AlonsoMoraRequest> requests, double now) {
			return new EuclideanSequenceGenerator(vehicle.getNextDiversion(now).link, onboardRequests, requests);
		}
	}
}
