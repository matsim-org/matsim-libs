package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import java.io.Serializable;

/**
 * Created by fouriep on 12/16/14.
 */
public interface BoardingModel extends Serializable{
    public boolean canBoard(double occupancy);
}
