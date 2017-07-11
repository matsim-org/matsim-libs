package org.matsim.contrib.opdyts.example.networkparameters;

import floetteroed.utilities.math.Vector;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.opdyts.MATSimState;

/**
 * This is only to show that one can create own states that carry more
 * simulation-related information than the basic MATSimState class.
 * 
 * 
 * @author Gunnar Flötteröd
 *
 */
class NetworkParametersState extends MATSimState {

	NetworkParametersState(final Population population, final Vector vectorRepresentation) {
		super(population, vectorRepresentation);
	}
}
