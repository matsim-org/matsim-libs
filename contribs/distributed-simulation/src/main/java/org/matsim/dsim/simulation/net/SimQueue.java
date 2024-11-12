package org.matsim.dsim.simulation.net;

import lombok.RequiredArgsConstructor;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.qsim.interfaces.DistributedMobsimVehicle;

@RequiredArgsConstructor
class SimQueue {

	private final SimDequeue internalQ;
	private final FlowCapacity inflowCapacity;
	private final StorageCapacity storageCapacity;

	boolean isEmpty() {
		return internalQ.isEmpty();
	}

	boolean isAccepting(SimLink.LinkPosition position, double now) {
		inflowCapacity.update(now);
		storageCapacity.update(now);

		return switch (position) {
			case QStart -> storageCapacity.isAvailable() && inflowCapacity.isAvailable();
			case QEnd -> storageCapacity.isAvailable();
			case Buffer -> throw new IllegalArgumentException("Queue is not accepting for buffer. This is a programming" +
				" error and must be fixed.");
		};
	}

	/**
	 * NOTE: Use isAccepting instead!!!
	 * This method is intended to be used from SplitInLink. It does not update the inflow and storage capacity. The
	 * SplitInLink must keep track of released vehicles during a timestep and therefore queries occupied storage capacity
	 * to build a difference.
	 */
	double getOccupied() {
		return storageCapacity.getOccupied();
	}

	DistributedMobsimVehicle peek() {
		return internalQ.peek();
	}

	DistributedMobsimVehicle poll(double now) {
		var vehicle = internalQ.poll();
		storageCapacity.release(vehicle.getSizeInEquivalents(), now);
		return vehicle;
	}

	void add(DistributedMobsimVehicle vehicle, SimLink.LinkPosition position) {

		// in any case we want to consume storage capacity
		storageCapacity.consume(vehicle.getSizeInEquivalents());
		switch (position) {
			case QStart -> {
				// if a vehicle is added from upstream we also want to enfore the inflow capacity constraint
				internalQ.addLast(vehicle);
				inflowCapacity.consume(vehicle.getSizeInEquivalents());
			}
			case QEnd -> internalQ.addFirst(vehicle);
			case Buffer -> throw new IllegalArgumentException("Vehicle can't be added into the buffer, because this method is in the" +
				"Queue implementation. This is a programming error and should not happen!!");
		}
	}

	static SimQueue create(Link link, QSimConfigGroup config, double effectiveCellSize) {
		var internalQueue = createInternalQueue(config);
		var inflowCap = FlowCapacity.createInflowCapacity(link, config, effectiveCellSize);
		var storageCapacity = createStorageCapacity(link, config, effectiveCellSize);
		link.getAttributes().putAttribute("maxInflowUsedInQsim", inflowCap.getMax());
		link.getAttributes().putAttribute("storageCapacityUsedInQsim", storageCapacity.getMax());
		return new SimQueue(internalQueue, inflowCap, storageCapacity);
	}

	static SimDequeue createInternalQueue(QSimConfigGroup config) {
		return switch (config.getLinkDynamics()) {
			case FIFO -> new FIFOQueue();
			case PassingQ -> new PassingQueue();
			case SeepageQ ->
				throw new RuntimeException("Config:qsim.linkDynamics = 'SeepageQ' is not supported. Supported options are: 'FIFO' and 'PassingQ'");
		};
	}

	static StorageCapacity createStorageCapacity(Link link, QSimConfigGroup config, double effectiveCellSize) {
		return switch (config.getTrafficDynamics()) {
			case queue -> new SimpleStorageCapacity(link, effectiveCellSize);
			case kinematicWaves -> new KinematicWavesStorageCapacity(link, effectiveCellSize);
			case withHoles ->
				throw new RuntimeException("Config:qsim.trafficDynamics = 'withHoles' is not supported. options are 'queue' and 'kinematicWaves'");
		};
	}
}
