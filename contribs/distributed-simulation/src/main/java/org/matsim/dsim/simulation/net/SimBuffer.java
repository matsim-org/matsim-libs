package org.matsim.dsim.simulation.net;

import lombok.Getter;
import org.matsim.core.mobsim.qsim.interfaces.DistributedMobsimVehicle;

import java.util.ArrayDeque;
import java.util.Queue;

class SimBuffer {

	private final Queue<DistributedMobsimVehicle> internalBuffer;
	private final FlowCapacity flowCap;

	@Getter
	private double pceInBuffer = 0;

	double getMaxFlowCapacity() {
		return flowCap.getMax();
	}

	SimBuffer(FlowCapacity outflowCapacity) {
		var minCapacity = Math.max(1, outflowCapacity.getMax());
		this.internalBuffer = new ArrayDeque<>((int) minCapacity);
		this.flowCap = outflowCapacity;
	}

	void add(DistributedMobsimVehicle vehicle, double now) {
		// TODO add flow efficiency
		//TODO implement stuck timer elsewhere
		//vehicle.startStuckTimer(now);
		this.flowCap.consume(vehicle.getSizeInEquivalents());
		this.pceInBuffer += vehicle.getSizeInEquivalents();
		this.internalBuffer.add(vehicle);
	}

	DistributedMobsimVehicle pollFirst() {
		var result = this.internalBuffer.remove();
		this.pceInBuffer -= result.getSizeInEquivalents();
		//TODO implement stuck time elsewhere
		//result.resetStuckTimer();
		return result;
	}

	DistributedMobsimVehicle peek() {
		return this.internalBuffer.peek();
	}

	boolean isAvailable(double now) {
		flowCap.update(now);
		return pceInBuffer < flowCap.getMax() && flowCap.isAvailable();
	}
}
