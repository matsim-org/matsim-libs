package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import com.google.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEndsParkingSearch;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEndsParkingSearchEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ParkingOccupancyObserver implements MobsimScopeEventHandler, VehicleEntersTrafficEventHandler, VehicleEndsParkingSearchEventHandler, BeforeMobsimListener, MobsimBeforeSimStepListener {
	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ParkingOccupancyObserver.class);

	private ParkingCapacityInitializer parkingCapacityInitializer;
	private Network network;
	private OutputDirectoryHierarchy outputDirectoryHierarchy;
	private Config config;

	Map<Id<Link>, Integer> indexByLinkId;
	int[] parkingOccupancyOfLastTimeStep;
	int[] parkingOccupancy;
	int[] capacity;

	double lastTimeStep = -1;

	@Inject
	public ParkingOccupancyObserver(Network network, ParkingCapacityInitializer parkingCapacityInitializer, Config config, OutputDirectoryHierarchy outputDirectoryHierarchy) {
		this.parkingCapacityInitializer = parkingCapacityInitializer;
		this.network = network;
		this.config = config;
		this.outputDirectoryHierarchy = outputDirectoryHierarchy;
	}

	@Override
	public synchronized void handleEvent(VehicleEntersTrafficEvent event) {
		checkTime(event.getTime());

		// unpark vehicle
		Id<Link> linkId = event.getLinkId();

		// We might have initialized to little initial parking, thus more vehicles enter traffic than expected. In this case, we just ignore the event.
		if (parkingOccupancy[indexByLinkId.get(linkId)] > 0) {
			parkingOccupancy[indexByLinkId.get(linkId)]--;
		}
	}

	@Override
	public synchronized void handleEvent(VehicleEndsParkingSearch event) {
		checkTime(event.getTime());

		// park vehicle
		Id<Link> linkId = event.getLinkId();
		parkingOccupancy[indexByLinkId.get(linkId)]++;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		initialize(event.getIteration(), network);

		//reset timer
		lastTimeStep = -1;
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

	private void checkTime(double now) {
		if (now != lastTimeStep) {
			throw new IllegalArgumentException("Time " + now + " does not match last time " + lastTimeStep);
		}
	}

	private void initialize(int iteration, Network network) {
		indexByLinkId = new HashMap<>(network.getLinks().size());
		int linkCount = network.getLinks().size();

		capacity = new int[linkCount];
		parkingOccupancy = new int[linkCount];
		parkingOccupancyOfLastTimeStep = new int[linkCount];

		Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialCapacities = parkingCapacityInitializer.initialize();
		writeInitialParkingOccupancy(iteration, initialCapacities);

		int counter = 0;
		for (Id<Link> id : network.getLinks().keySet()) {
			indexByLinkId.put(id, counter++);

			ParkingCapacityInitializer.ParkingInitialCapacity parkingInitialCapacity = initialCapacities.getOrDefault(id, new ParkingCapacityInitializer.ParkingInitialCapacity(0, 0));
			int index = indexByLinkId.get(id);

			capacity[index] = parkingInitialCapacity.capacity();
			parkingOccupancyOfLastTimeStep[index] = parkingInitialCapacity.occupancy();
			parkingOccupancy[index] = parkingInitialCapacity.occupancy();
		}
	}

	synchronized Map<Id<Link>, ParkingCount> getParkingCount(double now, Map<Id<Link>, Double> weightedLinks) {
		checkTime(now);

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

	private void writeInitialParkingOccupancy(int iteration, Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialCapacities) {
		String file = outputDirectoryHierarchy.getIterationFilename(iteration, ParkingUtils.PARKING_INITIAL_FILE);
		BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(file);

		log.info("Writing initial parking occupancy to {}", file);
		try {
			CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.Builder.create()
				.setDelimiter(config.global().getDefaultDelimiter().charAt(0))
				.setHeader(new String[]{"linkId", "capacity", "occupancy"}).build());

			for (Map.Entry<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> entry : initialCapacities.entrySet()) {
				csvPrinter.printRecord(entry.getKey(), entry.getValue().capacity(), entry.getValue().occupancy());
			}
			csvPrinter.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		log.info("Finished writing initial parking occupancy to {}", file);
	}
}
