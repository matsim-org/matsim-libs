package org.matsim.dsim.simulation.net;

import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class SimBuffer {

	private final Queue<DistributedMobsimVehicle> internalBuffer;

	/**
	 * Since when the vehicle at the front of the buffer has been waiting there: since it entered the empty buffer, or since the vehicle
	 * before it left. As {@code bufferLastMovedTime} in qsim's {@code QueueWithBuffer}, so that a blocked link moves one stuck vehicle
	 * per stuck time, and not every vehicle in its buffer that has waited for that long.
	 */
	private double lastMovedTime = Double.NEGATIVE_INFINITY;

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
		if (this.internalBuffer.isEmpty()) {
			this.lastMovedTime = now;
		}
		this.internalBuffer.add(vehicle);
		this.activateToNode.accept(toNode);
	}

	DistributedMobsimVehicle pollFirst(double now) {
		DistributedMobsimVehicle result = this.internalBuffer.remove();
		this.pceInBuffer -= result.getSizeInEquivalents();
		this.lastMovedTime = now;
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
		return !internalBuffer.isEmpty() && lastMovedTime + stuckThreshold <= now;
	}

	@Override
	public String toString() {
		var vehicles = internalBuffer.stream().map(v -> v.getId().toString()).collect(Collectors.joining(", "));
		return "veh=[" + vehicles + "], flow=[" + flowCap + "]";
	}
}
