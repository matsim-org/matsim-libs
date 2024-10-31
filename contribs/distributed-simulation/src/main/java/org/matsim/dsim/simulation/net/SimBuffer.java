package org.matsim.dsim.simulation.net;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Queue;

class SimBuffer {

    private final Queue<SimVehicle> internalBuffer;
    private final FlowCap flowCap;

    @Getter
    private double pceInBuffer = 0;

    double getMaxFlowCapacity() {
        return flowCap.max;
    }

    SimBuffer(double flowCapacityPerSecond) {
        var minCapacity = Math.max(1, flowCapacityPerSecond);
        this.internalBuffer = new ArrayDeque<>((int) minCapacity);
        this.flowCap = new FlowCap(flowCapacityPerSecond);
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

    boolean isAvailable() {
        return pceInBuffer < flowCap.max && flowCap.accumulatedCapacity > 0;
    }

    void updateFlowCapacity(double now) {
        flowCap.update(now);
    }

    private static class FlowCap {

        private final double max;

        private double lastUpdateTime;
        private double accumulatedCapacity;

        FlowCap(double flowCapacityPerSecond) {
            this.max = flowCapacityPerSecond;
            this.accumulatedCapacity = flowCapacityPerSecond;
            this.lastUpdateTime = 0;
        }

        void consume(double pce) {
            accumulatedCapacity -= pce;
        }

        void update(double now) {
            if (lastUpdateTime < now) {
                var timeSteps = now - lastUpdateTime;
                var accFlow = timeSteps * max + accumulatedCapacity;
                accumulatedCapacity = Math.min(accFlow, max);
                lastUpdateTime = now;
            }
        }
    }
}
