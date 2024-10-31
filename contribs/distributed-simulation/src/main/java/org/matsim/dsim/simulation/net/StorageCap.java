package org.matsim.dsim.simulation.net;

import lombok.Getter;
import org.matsim.api.core.v01.network.Link;

public class StorageCap {

    @Getter
    private final double max;

    @Getter
    private double released;
    @Getter
    private double consumed;
    private double occupied;

    StorageCap(Link link, double effectiveCellSize) {

        var capacity = link.getLength() * link.getNumberOfLanes() / effectiveCellSize;
        max = Math.max(capacity, link.getFlowCapacityPerSec());
    }

    double getUsed() {
        return occupied + consumed;
    }

    void consume(double pce) {
        consumed += pce;
    }

    void release(double pce) {
        released += pce;
    }

    void applyUpdates() {
        occupied = Math.max(0, getUsed() - released);
        released = 0;
        consumed = 0;
    }

    boolean isAvailable() {
        var availableCapacity = max - getUsed();
        return availableCapacity > 0;
    }
}
