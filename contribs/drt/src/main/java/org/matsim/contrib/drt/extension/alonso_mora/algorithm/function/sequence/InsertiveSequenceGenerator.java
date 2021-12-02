package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop.StopType;
import org.matsim.contrib.dvrp.schedule.Schedule;

/**
 * Insertive sequence generator as described by Alonso-Mora et al.
 * 
 * The generator keeps the order of requests that are already assigned to the
 * vehicle and inserts new pickup and dropoff stops into that ordering.
 * 
 * @author sebhoerl
 */
public class InsertiveSequenceGenerator implements SequenceGenerator {
	private final ExtensiveSequenceGenerator generator;
	private final IndexCalculator indexCalculator;

	public InsertiveSequenceGenerator(AlonsoMoraVehicle vehicle, Collection<AlonsoMoraRequest> onboardRequests,
			Collection<AlonsoMoraRequest> requests) {
		this.generator = new ExtensiveSequenceGenerator(onboardRequests, requests);
		this.indexCalculator = new DefaultIndexCalculator(vehicle, onboardRequests, requests);

		skipInvalidSequences();
	}

	InsertiveSequenceGenerator(IndexCalculator indexCalculator, Collection<AlonsoMoraRequest> onboardRequests,
			Collection<AlonsoMoraRequest> requests) {
		// Constructor for testing
		this.generator = new ExtensiveSequenceGenerator(onboardRequests, requests);
		this.indexCalculator = indexCalculator;
	}

	private boolean isValidSequence() {
		int currentIndex = 0;

		for (AlonsoMoraStop stop : generator.get()) {
			if (stop.getType().equals(StopType.Pickup)) {
				Integer pickupIndex = indexCalculator.getPickupIndex(stop.getRequest());

				if (pickupIndex != null) {
					if (pickupIndex < currentIndex) {
						return false;
					} else {
						currentIndex = pickupIndex;
					}
				}
			} else {
				Integer dropoffIndex = indexCalculator.getDropoffIndex(stop.getRequest());

				if (dropoffIndex != null) {
					if (dropoffIndex < currentIndex) {
						return false;
					} else {
						currentIndex = dropoffIndex;
					}
				}
			}
		}

		return true;
	}

	private void skipInvalidSequences() {
		while (generator.hasNext() && !isValidSequence()) {
			generator.abort();
		}
	}

	@Override
	public void advance() {
		generator.advance();
		skipInvalidSequences();
	}

	@Override
	public void abort() {
		generator.abort();
		skipInvalidSequences();
	}

	@Override
	public boolean hasNext() {
		return generator.hasNext();
	}

	@Override
	public List<AlonsoMoraStop> get() {
		return generator.get();
	}

	@Override
	public boolean isComplete() {
		return generator.isComplete();
	}

	interface IndexCalculator {
		Integer getPickupIndex(AlonsoMoraRequest request);

		Integer getDropoffIndex(AlonsoMoraRequest request);
	}

	class DefaultIndexCalculator implements IndexCalculator {
		private final Map<AlonsoMoraRequest, Integer> pickupSequence = new HashMap<>();
		private final Map<AlonsoMoraRequest, Integer> dropoffSequence = new HashMap<>();

		DefaultIndexCalculator(AlonsoMoraVehicle vehicle, Collection<AlonsoMoraRequest> onboardRequests,
				Collection<AlonsoMoraRequest> requests) {
			Schedule schedule = vehicle.getVehicle().getSchedule();

			Collection<AlonsoMoraRequest> vehicleRequests = new HashSet<>();
			vehicleRequests.addAll(onboardRequests);
			requests.stream().filter(r -> r.getVehicle() == vehicle).forEach(vehicleRequests::add);

			for (AlonsoMoraRequest request : vehicleRequests) {
				pickupSequence.put(request, schedule.getTasks().indexOf(request.getPickupTask()));
				dropoffSequence.put(request, schedule.getTasks().indexOf(request.getDropoffTask()));
			}
		}

		@Override
		public Integer getPickupIndex(AlonsoMoraRequest request) {
			return pickupSequence.get(request);
		}

		@Override
		public Integer getDropoffIndex(AlonsoMoraRequest request) {
			return dropoffSequence.get(request);
		}
	}

	static public class Factory implements SequenceGeneratorFactory {
		@Override
		public SequenceGenerator createGenerator(AlonsoMoraVehicle vehicle,
				Collection<AlonsoMoraRequest> onboardRequests, Collection<AlonsoMoraRequest> requests, double now) {
			return new InsertiveSequenceGenerator(vehicle, onboardRequests, requests);
		}
	}
}
