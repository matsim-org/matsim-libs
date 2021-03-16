package org.matsim.contrib.parking.parkingproxy;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

/**
 * Generates an initial distribution of cars.
 * 
 * @author tkohl / Senozon
 *
 */
public interface InitialLoadGenerator {

	/**
	 * Generates the list of initial car positions and their weight.
	 * 
	 * @return a List of (car position, car weight)-pairs
	 */
	Collection<Tuple<Coord, Integer>> calculateInitialCarPositions();

}
