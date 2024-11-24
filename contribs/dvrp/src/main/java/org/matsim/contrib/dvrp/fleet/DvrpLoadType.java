package org.matsim.contrib.dvrp.fleet;

/**
 * This interface allows to factor the logic behind particular implementations of {@link DvrpLoad}.
 * Typically, each implementation of DvrpLoad would be accompanied by a DvrpLoadType implementation that defines the following methods:
 * - {@link DvrpLoadType#getEmptyLoad()}: to retrieve the DvrpLoad of the related implementation that represents an empty quantity
 * - {@link DvrpLoadType#fromArray(Number[])}: to build {@link DvrpLoad} instances  from an array of numbers. This method should throw an exception if non-valid numbers are passed.
 * - {@link DvrpLoadType#numberOfDimensions()}: the size of the array necessary to build a new instance, also the size of the array returned by {@link DvrpLoad#asArray()}
 * - {@link DvrpLoadType#getName()}: The String name by which the current DvrpLoadType is known in the current simulation
 * - {@link DvrpLoadType#getSlotNames()}: An array of the same size as {@link DvrpLoadType#numberOfDimensions()}, representing the String names of the multiple dimensions of the current load type.
 * @author tarek.chouaki
 */
public interface DvrpLoadType {
	DvrpLoad fromArray(Number[] array);
	DvrpLoad getEmptyLoad();
	int numberOfDimensions();
	String[] getSlotNames();
	String getName();
}
