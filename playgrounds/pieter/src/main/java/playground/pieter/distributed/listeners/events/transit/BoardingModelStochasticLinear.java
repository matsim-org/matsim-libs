package playground.pieter.distributed.listeners.events.transit;

import org.matsim.core.gbl.MatsimRandom;

import java.util.Random;

/**
 * Created by fouriep on 12/19/14.
 */
public class BoardingModelStochasticLinear implements BoardingModel {
    private Random random;

    public BoardingModelStochasticLinear() {
        random = MatsimRandom.getLocalInstance();
    }

    @Override
    public boolean canBoard(double occupancy) {
        return random.nextDouble() > occupancy;
    }
}
