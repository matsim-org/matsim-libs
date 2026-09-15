package org.matsim.dsim.simulation.net;

import it.unimi.dsi.fastutil.doubles.DoubleArrayFIFOQueue;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class SimBuffer {

	private final Queue<DistributedMobsimVehicle> internalBuffer;
	private final DoubleArrayFIFOQueue arrivalTimes = new DoubleArrayFIFOQueue();

	private final FlowCapacity flowCap;
	private final double stuckThreshold;
	private final Consumer<SimNode> activateToNode;
	private final SimNode toNode;

	private double pceInBuffer = 0;

	public double getPceInBuffer() {
		return pceInBuffer;
	}

	double getMaxFlowCapacity() {
		return flowCap.getMax();
	}

	SimBuffer(FlowCapacity outflowCapacity, double stuckThreshold, SimNode toNode, Consumer<SimNode> activateToNode) {
		this.stuckThreshold = stuckThreshold;
		double minCapacity = Math.max(1, outflowCapacity.getMax());
		this.internalBuffer = new ArrayDeque<>((int) minCapacity);
		this.flowCap = outflowCapacity;
		this.toNode = toNode;
		this.activateToNode = activateToNode;
	}

	void add(DistributedMobsimVehicle vehicle, double now) {
		// TODO add flow efficiency
		this.flowCap.consume(vehicle.getSizeInEquivalents());
		this.pceInBuffer += vehicle.getSizeInEquivalents();
		this.internalBuffer.add(vehicle);
		this.arrivalTimes.enqueue(now);
		this.activateToNode.accept(toNode);
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

	@Override
	public String toString() {
		var vehicles = internalBuffer.stream().map(v -> v.getId().toString()).collect(Collectors.joining(", "));
		return "veh=[" + vehicles + "], flow=[" + flowCap + "]";
	}
}
