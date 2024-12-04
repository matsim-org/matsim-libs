package org.matsim.contrib.dvrp.fleet.dvrp_load;

/**
 * This class represents a one-dimensional {@link DvrpLoad} where that unique dimension is the set of integers
 * If used directly, this class allows to simulate a dvrp fleet with regular homogeneous transported (typically persons).
 * However, if using this class through sub-classes, one can already have heterogeneous capacities and loads:
 * 	- An integerLoad A will not be compatible with another only if they are of the exact same class and if their types are the same (firstType.equals(secondType) returns true)
 * 	- Addition and subtraction methods will not work if performed with on an instance of a different sub-class or that does not have the same type (an exception will be thrown)
 * @author Tarek Chouaki (tkchouaki)
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

	protected IntegerLoad checkCompatibility(DvrpLoad other) {
		if(other == null) {
			return this;
		}
		Class<? extends DvrpLoad> currentClass = other.getClass();
		if(!(currentClass.isInstance(other))) {
			throw new IncompatibleLoadsException(this, other);
		}
		IntegerLoad integerLoad = (IntegerLoad) other;
		if(!this.getType().equals(integerLoad.getType())) {
			throw new IncompatibleLoadsException(this, other);
		}
		return integerLoad;
	}

	@Override
	public IntegerLoad add(DvrpLoad other) {
		return getType().fromInt(checkCompatibility(other).load + this.load);
	}

	@Override
	public IntegerLoad subtract(DvrpLoad other) {
		return getType().fromInt(this.load - checkCompatibility(other).load);
	}

	@Override
	public boolean fitsIn(DvrpLoad other) {
		try {
			IntegerLoad integerLoad = checkCompatibility(other);
			return this.load <= integerLoad.load;
		} catch (IncompatibleLoadsException e) {
			return false;
		}
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
