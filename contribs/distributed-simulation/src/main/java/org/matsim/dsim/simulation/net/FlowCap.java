package org.matsim.dsim.simulation.net;

import lombok.Getter;
import org.matsim.api.core.v01.network.Link;

public class FlowCap {

    @Getter
    private final double max;

    private double lastUpdateTime;
    private double accumulatedCapacity;

    FlowCap(Link link) {
        this.max = link.getFlowCapacityPerSec();
        this.accumulatedCapacity = link.getFlowCapacityPerSec();
        this.lastUpdateTime = 0;
    }

    boolean isAvailable() {
        return accumulatedCapacity > 1e-10; // give some error margin here
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
