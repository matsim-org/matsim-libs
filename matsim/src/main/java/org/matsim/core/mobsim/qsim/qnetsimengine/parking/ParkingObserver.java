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

import java.util.HashMap;
import java.util.Map;

public class ParkingObserver implements BeforeMobsimListener, VehicleEntersTrafficEventHandler, VehicleEndsParkingSearchEventHandler {
	ParkingCapacityInitializer parkingCapacityInitializer;

	Map<Id<Link>, Integer> indexByLinkId;
	int[] parkingCount;
	int[] capacity;

	@Inject
	public ParkingObserver(Network network, ParkingCapacityInitializer parkingCapacityInitializer) {
		this.parkingCapacityInitializer = parkingCapacityInitializer;
		initialize(network);
	}

	private void initialize(Network network) {
		indexByLinkId = new HashMap<>(network.getLinks().size());
		int linkCount = network.getLinks().size();
		capacity = new int[linkCount];
		parkingCount = new int[linkCount];

		Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialCapacities = parkingCapacityInitializer.initialize();

		int counter = 0;
		for (Id<Link> id : network.getLinks().keySet()) {
			indexByLinkId.put(id, counter++);

			ParkingCapacityInitializer.ParkingInitialCapacity parkingInitialCapacity = initialCapacities.getOrDefault(id, new ParkingCapacityInitializer.ParkingInitialCapacity(0, 0));
			int index = indexByLinkId.get(id);

			capacity[index] = parkingInitialCapacity.capacity();
			parkingCount[index] = parkingInitialCapacity.initial();
		}
	}

	Map<Id<Link>, ParkingCount> getParkingCount(Map<Id<Link>, Double> weightedLinks) {
		Map<Id<Link>, ParkingCount> result = new HashMap<>();
		weightedLinks.forEach((linkId, weight) -> {
			int index = indexByLinkId.get(linkId);
			new ParkingCount(parkingCount[index], capacity[index], weight);
			result.put(linkId, new ParkingCount(parkingCount[index], capacity[index], weight));
		});
		return result;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		// unpark vehicle
		Id<Link> linkId = event.getLinkId();
		parkingCount[indexByLinkId.get(linkId)]--;
	}

	@Override
	public void handleEvent(VehicleEndsParkingSearch event) {
		// park vehicle
		Id<Link> linkId = event.getLinkId();
		parkingCount[indexByLinkId.get(linkId)]++;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

	}

	@Override
	public void reset(int iteration) {
		//TODO
	}
}
