package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

/**
 * Created by fouriep on 12/16/14.
 */
public class BoardingModelIgnoringOccupancy implements BoardingModel {
    @Override
    public boolean canBoard(double occupancy) {
        return true;
    }
}
