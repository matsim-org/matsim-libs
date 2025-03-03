package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEndsParkingSearch;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEndsParkingSearchEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ParkingOccupancyObserver implements MobsimScopeEventHandler, VehicleEntersTrafficEventHandler, VehicleEndsParkingSearchEventHandler, BeforeMobsimListener, MobsimBeforeSimStepListener {
	ParkingCapacityInitializer parkingCapacityInitializer;
	Network network;

	Map<Id<Link>, Integer> indexByLinkId;
	int[] parkingOccupancyOfLastTimeStep;
	int[] parkingOccupancy;
	int[] capacity;

	double lastTimeStep = -1;

	@Inject
	public ParkingOccupancyObserver(Network network, ParkingCapacityInitializer parkingCapacityInitializer) {
		this.parkingCapacityInitializer = parkingCapacityInitializer;
		this.network = network;
	}

	private void initialize(Network network) {
		indexByLinkId = new HashMap<>(network.getLinks().size());
		int linkCount = network.getLinks().size();

		capacity = new int[linkCount];
		parkingOccupancy = new int[linkCount];
		parkingOccupancyOfLastTimeStep = new int[linkCount];

		Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialCapacities = parkingCapacityInitializer.initialize();

		int counter = 0;
		for (Id<Link> id : network.getLinks().keySet()) {
			indexByLinkId.put(id, counter++);

			ParkingCapacityInitializer.ParkingInitialCapacity parkingInitialCapacity = initialCapacities.getOrDefault(id, new ParkingCapacityInitializer.ParkingInitialCapacity(0, 0));
			int index = indexByLinkId.get(id);

			capacity[index] = parkingInitialCapacity.capacity();
			parkingOccupancyOfLastTimeStep[index] = parkingInitialCapacity.initial();
			parkingOccupancy[index] = parkingInitialCapacity.initial();
		}
	}

	synchronized Map<Id<Link>, ParkingCount> getParkingCount(double now, Map<Id<Link>, Double> weightedLinks) {
		Map<Id<Link>, ParkingCount> result = new HashMap<>();
		for (Map.Entry<Id<Link>, Double> entry : weightedLinks.entrySet()) {
			Id<Link> linkId = entry.getKey();
			double weight = entry.getValue();
			int index = indexByLinkId.get(linkId);
			int occupancy = parkingOccupancyOfLastTimeStep[index];
			result.put(linkId, new ParkingCount(occupancy, capacity[index], weight));
		}
		return result;
	}

	@Override
	public synchronized void handleEvent(VehicleEntersTrafficEvent event) {
		// unpark vehicle
		Id<Link> linkId = event.getLinkId();

		// We might have initialized to little initial parking, thus more vehicles enter traffic than expected. In this case, we just ignore the event.
		if (parkingOccupancy[indexByLinkId.get(linkId)] > 0) {
			parkingOccupancy[indexByLinkId.get(linkId)]--;
		}
	}

	@Override
	public synchronized void handleEvent(VehicleEndsParkingSearch event) {
		// park vehicle
		Id<Link> linkId = event.getLinkId();
		parkingOccupancy[indexByLinkId.get(linkId)]++;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// initialize
		initialize(network);
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		double currentTimeStep = e.getSimulationTime();
		if (currentTimeStep == lastTimeStep) {
			throw new IllegalArgumentException("Already processed time " + currentTimeStep);
		}

		if (currentTimeStep < lastTimeStep) {
			throw new IllegalArgumentException("Events must be ordered by time.");
		}

		// currentTimeStep > lastEventTime => we have to store current count under last and initialize a new parkingCount
		this.parkingOccupancyOfLastTimeStep = Arrays.copyOf(this.parkingOccupancy, this.parkingOccupancy.length);

		lastTimeStep = currentTimeStep;
	}
}
