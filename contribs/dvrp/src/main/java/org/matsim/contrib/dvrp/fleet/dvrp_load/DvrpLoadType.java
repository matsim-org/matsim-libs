package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.matsim.api.core.v01.Id;

/**
 * This interface allows to factor the logic behind particular implementations of {@link DvrpLoad}.
 * Typically, each implementation of DvrpLoad would be accompanied by a DvrpLoadType implementation that defines the following methods:
 * - {@link DvrpLoadType#getEmptyLoad()}: to retrieve the DvrpLoad of the related implementation that represents an empty quantity
 * - {@link DvrpLoadType#fromArray(Number[])}: to build {@link DvrpLoad} instances  from an array of numbers. This method should throw an exception if non-valid numbers are passed.
 * - {@link DvrpLoadType#getId()}: The ID by which the current DvrpLoadType is known in the current simulation
 * - {@link DvrpLoadType#getSlotNames()}: An array representing the String names of the multiple dimensions of the current load type.
 * @author Tarek Chouaki (tkchouaki)
 */
public interface DvrpLoadType {
	DvrpLoad fromArray(Number[] array);
	DvrpLoad getEmptyLoad();
	String[] getSlotNames();
	Id<DvrpLoadType> getId();
}
