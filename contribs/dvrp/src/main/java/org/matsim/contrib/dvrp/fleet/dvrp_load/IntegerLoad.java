package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.matsim.contrib.dvrp.fleet.DvrpLoad;

/**
 * This class represents a one-dimensional Dvrp vehicle load (and capacity).
 * If used directly, this class allows to simulate a dvrp fleet with regular homogeneous transported (typically persons).
 * However, if using this class through sub-classes, one must be careful:
 * 	- A scalar load of class A will not be compatible with a vehicle with a capacity represented by a scalar load of class B (the fitsIn method will return false)
 * 	- Addition and subtraction methods will not work if performed with on an an instance of a different sub-class (an exception will be thrown)
 */
public class IntegerLoad extends ScalarLoad {

	private final IntegerLoadType loadType;
	private final int load;

	public IntegerLoad(int load, IntegerLoadType loadType) {
		super(loadType);
		this.load = load;
		this.loadType = loadType;
	}

	public int getLoad() {
		return this.load;
	}

	@Override
	public IntegerLoadType getType() {
		return this.loadType;
	}

	@Override
	public IntegerLoad addTo(DvrpLoad other) {
		if(other == null) {
			return this;
		}
		Class<? extends DvrpLoad> currentClass = other.getClass();
		if(!(currentClass.isInstance(other))) {
			throw new UnsupportedVehicleLoadException(other, currentClass);
		}
		IntegerLoad integerLoad = (IntegerLoad) other;
		if(!this.getType().equals(integerLoad.getType())) {
			throw new UnsupportedVehicleLoadException(other, this.getClass());
		}
		return getType().fromInt(integerLoad.load + this.load);
	}

	@Override
	public IntegerLoad subtract(DvrpLoad other) {
		if(other == null) {
			return this;
		}
		Class<? extends DvrpLoad> currentClass = other.getClass();
		if(!(currentClass.isInstance(other))) {
			throw new UnsupportedVehicleLoadException(other, currentClass);
		}
		IntegerLoad integerLoad = (IntegerLoad) other;
		if(!this.getType().equals(integerLoad.getType())) {
			throw new UnsupportedVehicleLoadException(other, this.getClass());
		}
		return getType().fromInt(this.load - integerLoad.load);
	}

	@Override
	public boolean fitsIn(DvrpLoad other) {
		if(!this.getClass().equals(other.getClass())) {
			return false;
		}
		IntegerLoad integerLoad = (IntegerLoad) other;
		if(!integerLoad.getType().equals(this.getType())) {
			return false;
		}
		return this.load <= integerLoad.load;
	}

	@Override
	public boolean isEmpty() {
		return this.load == 0;
	}

	@Override
	public Number getElement(int index) {
		if(index > 0) {
			throw new IndexOutOfBoundsException();
		}
		return this.getLoad();
	}

	@Override
	public Number[] asArray() {
		return new Number[]{this.getLoad()};
	}

	@Override
	public boolean equals(Object o) {
		if(!o.getClass().equals(this.getClass())) {
			return false;
		}
		IntegerLoad otherLoad = (IntegerLoad) o;
		return this.getType().equals(otherLoad.getType()) && this.getLoad() == otherLoad.getLoad();
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode() + this.loadType.hashCode() + this.getLoad();
	}
}
