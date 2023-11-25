package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import java.util.Random;
import org.matsim.core.gbl.MatsimRandom;

/** Created by fouriep on 12/19/14. */
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
