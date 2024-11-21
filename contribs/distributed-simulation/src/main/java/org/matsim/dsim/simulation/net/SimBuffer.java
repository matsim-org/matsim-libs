package org.matsim.dsim.simulation.net;

import it.unimi.dsi.fastutil.doubles.DoubleArrayFIFOQueue;
import lombok.Getter;
import org.matsim.core.mobsim.qsim.interfaces.DistributedMobsimVehicle;

import java.util.ArrayDeque;
import java.util.Queue;

class SimBuffer {

	private final Queue<DistributedMobsimVehicle> internalBuffer;
	private final DoubleArrayFIFOQueue arrivalTimes = new DoubleArrayFIFOQueue();

	private final FlowCapacity flowCap;
	private final double stuckThreshold;

	@Getter
	private double pceInBuffer = 0;

	double getMaxFlowCapacity() {
		return flowCap.getMax();
	}

	SimBuffer(FlowCapacity outflowCapacity, double stuckThreshold) {
		this.stuckThreshold = stuckThreshold;
		double minCapacity = Math.max(1, outflowCapacity.getMax());
		this.internalBuffer = new ArrayDeque<>((int) minCapacity);
		this.flowCap = outflowCapacity;
	}

	void add(DistributedMobsimVehicle vehicle, double now) {
		// TODO add flow efficiency
		this.flowCap.consume(vehicle.getSizeInEquivalents());
		this.pceInBuffer += vehicle.getSizeInEquivalents();
		this.internalBuffer.add(vehicle);
		this.arrivalTimes.enqueue(now);
	}

	DistributedMobsimVehicle pollFirst() {
		DistributedMobsimVehicle result = this.internalBuffer.remove();
		this.pceInBuffer -= result.getSizeInEquivalents();
		this.arrivalTimes.dequeueDouble();
		return result;
	}

	DistributedMobsimVehicle peek() {
		return this.internalBuffer.peek();
	}

	boolean isAvailable(double now) {
		flowCap.update(now);
		return pceInBuffer < flowCap.getMax() && flowCap.isAvailable();
	}

	boolean isStuck(double now) {
		return !arrivalTimes.isEmpty() && arrivalTimes.firstDouble() + stuckThreshold <= now;
	}
}
