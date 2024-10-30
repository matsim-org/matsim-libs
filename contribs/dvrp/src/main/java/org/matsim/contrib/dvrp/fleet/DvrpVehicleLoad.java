package org.matsim.contrib.dvrp.fleet;


/**
 * This interface is meant to allow to flexibly define DVRP vehicle capacities and occupancies that are more complex than a integer.
 * The implementations of this interface should define addition and subtraction method. They should also support negative values.
 * Moreover, the `addTo` and `subtract` methods should support null values and return the current element in that case.
 * Implementations should also redefine the `equals` method
 * @author tarek.chouaki
 */
public interface DvrpVehicleLoad {
	class UnsupportedVehicleLoadException extends IllegalStateException {
		public UnsupportedVehicleLoadException(DvrpVehicleLoad left, Class<? extends DvrpVehicleLoad> dvrpVehicleLoadClass) {
			super(String.format("%s is not a direct instance (might be a subclass instance) of the %s class", left.toString(), dvrpVehicleLoadClass.toString()));
		}
	}

	/**
	 * @param other : Another DvrpVehicleLoad of the same implementation as the current one.
	 * @return : A DvrpVehicleLoad of the same implementation representing the one resulting from adding the current one to the given one.
	 */
	DvrpVehicleLoad addTo(DvrpVehicleLoad other);

	/**
	 * @param other : Another DvrpVehicleLoad of the same implementation as the current one.
	 * @return : A DvrpVehicleLoad of the same implementation as the current one representing the subtraction (current - other). If other is null, current is returned.
	 */
	DvrpVehicleLoad subtract(DvrpVehicleLoad other);

	/**
	 * @return : An instance of the current implementation representing an empty load.
	 */
	DvrpVehicleLoad getEmptyLoad();

	/**
	 * @param other : Another DvrpVehicleLoad.
	 * @return : True if the current DvrpVehicleLoad can fit in a vehicle that has a capacity represented by the given one. False otherwise.
	 */
	boolean fitsIn(DvrpVehicleLoad other);

	/**
	 * @return : True if the current DvrpVehicleLoad represents an empty load. False otherwise.
	 */
	boolean isEmpty();

}
