package org.matsim.contrib.dvrp.fleet;

/**
 * This class represents a one-dimensional Dvrp vehicle load (and capacity).
 * If used directly, this class allows to simulate a dvrp fleet with regular homogeneous transported (typically persons).
 * However, if using this class through sub-classes, one must be careful:
 * 	- A scalar load of class A will not be compatible with a vehicle with a capacity represented by a scalar load of class B (the fitsIn method will return false)
 * 	- Addition and subtraction methods will not work if performed with on an an instance of a different sub-class (an exception will be thrown)
 */
public class ScalarVehicleLoad implements DvrpVehicleLoad {

	private final int capacity;

	public ScalarVehicleLoad(int capacity) {
		this.capacity = capacity;
	}

	public int getLoad() {
		return this.capacity;
	}

	@Override
	public DvrpVehicleLoad addTo(DvrpVehicleLoad other) {
		if(other == null) {
			return this;
		}
		Class<? extends DvrpVehicleLoad> currentClass = this.getClass();
		if(!other.getClass().equals(currentClass)) {
			throw new UnsupportedVehicleLoadException(other, currentClass);
		}
		ScalarVehicleLoad scalarVehicleLoad = (ScalarVehicleLoad) other;
		return fromInt(scalarVehicleLoad.capacity + this.capacity);
	}

	@Override
	public DvrpVehicleLoad subtract(DvrpVehicleLoad other) {
		if(other == null) {
			return this;
		}
		Class<? extends DvrpVehicleLoad> currentClass = other.getClass();
		if(!(currentClass.isInstance(other))) {
			throw new UnsupportedVehicleLoadException(other, currentClass);
		}
		ScalarVehicleLoad scalarVehicleLoad = (ScalarVehicleLoad) other;
		return fromInt(this.capacity - scalarVehicleLoad.capacity);
	}

	@Override
	public DvrpVehicleLoad getEmptyLoad() {
		return fromInt(0);
	}

	@Override
	public boolean fitsIn(DvrpVehicleLoad other) {
		if(!this.getClass().equals(other.getClass())) {
			return false;
		}
		ScalarVehicleLoad scalarVehicleLoad = (ScalarVehicleLoad) other;
		return this.capacity <= scalarVehicleLoad.capacity;
	}

	@Override
	public boolean isEmpty() {
		return this.capacity == 0;
	}

	protected ScalarVehicleLoad fromInt(int load) {
		return new ScalarVehicleLoad(load);
	}
}
