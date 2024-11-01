package org.matsim.dsim.simulation.net;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Queue;

class SimBuffer {

    private final Queue<SimVehicle> internalBuffer;
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

    void add(SimVehicle vehicle, double now) {
        // TODO add flow efficiency
        vehicle.startStuckTimer(now);
        this.flowCap.consume(vehicle.getPce());
        this.pceInBuffer += vehicle.getPce();
        this.internalBuffer.add(vehicle);
    }

    SimVehicle pollFirst() {
        var result = this.internalBuffer.remove();
        this.pceInBuffer -= result.getPce();
        result.resetStuckTimer();
        return result;
    }

    SimVehicle peek() {
        return this.internalBuffer.peek();
    }

	boolean isAvailable(double now) {
        flowCap.update(now);
		return pceInBuffer < flowCap.getMax() && flowCap.isAvailable();
    }
}
