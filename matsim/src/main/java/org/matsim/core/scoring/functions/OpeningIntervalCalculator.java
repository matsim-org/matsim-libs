package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.population.Activity;

public interface OpeningIntervalCalculator {

	double[] getOpeningInterval(final Activity act);

}
