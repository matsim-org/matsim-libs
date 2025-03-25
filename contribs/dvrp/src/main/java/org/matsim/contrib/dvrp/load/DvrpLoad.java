package org.matsim.contrib.dvrp.load;

/**
 * This interface is meant to allow to flexibly define DVRP vehicle capacities
 * and occupancies that are more complex than a integer.
 * The implementations of this interface should define:
 * 
 * - Comparison: through the {@link DvrpLoad#fitsIn(DvrpLoad)} method. The
 * general meaning behind it is whether the current DvrpLoad can be put in a
 * vehicle that has the given load as a capacity. It must support DvrpLoad
 * objects of different implementations and return false if they are not
 * compatible.
 * 
 * - Addition: through the {@link DvrpLoad#add(DvrpLoad)} method.
 * 
 * - Subtraction: through the {@link DvrpLoad#subtract(DvrpLoad)} method.
 * 
 * Addition and subtraction implementations must support null parameters, and
 * return the current load in those cases. Moreover, negative values must be
 * supported.
 * 
 * Moreover, a DvrpLoad can be viewed as an array of Numbers that can be queried
 * by their index. This is done by implementing
 * {@link DvrpLoad#getElement(int)}.
 * 
 * Each DvrpLoad must have a {@link DvrpLoadType} which specifies the general
 * description of loads of that type.
 * 
 * Implementations should also redefine the `equals` method
 * 
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public interface DvrpLoad {
	/**
	 * @param other : Another DvrpVehicleLoad of the same implementation as the
	 *              current one.
	 * @return : A DvrpVehicleLoad of the same implementation representing the one
	 *         resulting from adding the current one to the given one.
	 */
	DvrpLoad add(DvrpLoad other);

	/**
	 * @param other : Another DvrpVehicleLoad of the same implementation as the
	 *              current one.
	 * @return : A DvrpVehicleLoad of the same implementation as the current one
	 *         representing the subtraction (current - other). If other is null,
	 *         current is returned.
	 */
	DvrpLoad subtract(DvrpLoad other);

	/**
	 * @param other : Another DvrpVehicleLoad.
	 * @return : True if the current DvrpVehicleLoad can fit in a vehicle that has a
	 *         capacity represented by the given one. False otherwise.
	 */
	boolean fitsIn(DvrpLoad other);

	/**
	 * @return : True if the current DvrpVehicleLoad represents an empty load. False
	 *         otherwise.
	 */
	boolean isEmpty();

	/**
	 * We assume that all DvrpLoad objects can be represented with an array of
	 * Number objects.
	 * This method returns a specific Number component of the load.
	 * 
	 * @param i: the index of the desired element
	 * @return : The i'th component of the current load
	 */
	Number getElement(int i);

}
