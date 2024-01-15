package org.matsim.contrib.drt.sharingmetrics;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

import java.util.*;

/**
 * @author nkuehnel / MOIA
 */
public class SharingMetricsTracker implements DrtRequestSubmittedEventHandler, PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler, MobsimBeforeCleanupListener {

	record Segment(double start, int occupancy) {
	}

	private final Map<Id<DvrpVehicle>, List<Id<Request>>> map = new HashMap<>();

	private final Map<Id<Request>, List<Segment>> segments = new HashMap<>();

	private final Map<Id<Request>, List<Id<Person>>> knownGroups = new HashMap<>();

	private final Map<Id<Request>, Double> sharingFactors = new HashMap<>();
	private final Map<Id<Request>, Boolean> poolingRate = new HashMap<>();


	public SharingMetricsTracker() {
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		knownGroups.put(event.getRequestId(), new ArrayList<>(event.getPersonIds()));
	}

	@Override
	public void handleEvent(PassengerDroppedOffEvent event) {

		List<Id<Person>> passengers = knownGroups.get(event.getRequestId());
		passengers.remove(event.getPersonId());

		if (passengers.isEmpty()) {
			// all passengers of the group have been dropped off
			knownGroups.remove(event.getRequestId());

			List<Id<Request>> occupancy = map.get(event.getVehicleId());
			occupancy.remove(event.getRequestId());
			occupancy.forEach(p -> segments.get(p).add(new Segment(event.getTime(), occupancy.size())));

			List<Segment> finishedSegments = segments.remove(event.getRequestId());

			double total = 0;
			double portion = 0;

			boolean pooled = false;

			Segment last = finishedSegments.get(0);
			if (last.occupancy > 1) {
				pooled = true;
			}
			for (int i = 1; i < finishedSegments.size(); i++) {
				Segment next = finishedSegments.get(i);
				double duration = next.start - last.start;
				total += duration;
				portion += duration / last.occupancy;
				last = next;
				if (last.occupancy > 1) {
					pooled = true;
				}
			}

			double duration = event.getTime() - last.start;
			total += duration;
			portion += duration / last.occupancy;

			double sharingFactor = total / portion;
			sharingFactors.put(event.getRequestId(), sharingFactor);
			poolingRate.put(event.getRequestId(), pooled);
		}
	}

	@Override
	public void handleEvent(PassengerPickedUpEvent event) {
		List<Id<Request>> occupancy = map.computeIfAbsent(event.getVehicleId(), vehicleId -> new ArrayList<>());
		if (occupancy.contains(event.getRequestId())) {
			if (knownGroups.get(event.getRequestId()).size() > 1) {
				//group request, skip for additional persons
				return;
			} else {
				throw new RuntimeException("Single rider request picked up twice!");
			}
		} else {
			occupancy.add(event.getRequestId());
			occupancy.forEach(
					p -> segments.computeIfAbsent(p, requestId -> new ArrayList<>()).add(new Segment(event.getTime(), occupancy.size()))
			);
		}
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		map.clear();
		segments.clear();
		poolingRate.clear();
		sharingFactors.clear();
		knownGroups.clear();
	}

	public Map<Id<Request>, Double> getSharingFactors() {
		return Collections.unmodifiableMap(sharingFactors);
	}

	public Map<Id<Request>, Boolean> getPoolingRates() {
		return Collections.unmodifiableMap(poolingRate);
	}
}
