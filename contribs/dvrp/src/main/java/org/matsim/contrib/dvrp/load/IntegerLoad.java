package org.matsim.contrib.dvrp.load;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class IntegerLoad implements DvrpLoad {
	private final int value;

	IntegerLoad(int value) {
		this.value = value;
	}

	protected IntegerLoad check(DvrpLoad load) {
        if (load instanceof IntegerLoad otherLoad) {
            return otherLoad;
        }

        throw new IllegalStateException("Passed load is not an IntegerLoad");
	}

	@Override
	public IntegerLoad add(DvrpLoad other) {
		return new IntegerLoad(check(other).value + this.value);
	}

	@Override
	public IntegerLoad subtract(DvrpLoad other) {
		return new IntegerLoad(this.value - check(other).value);
	}

	@Override
	public boolean fitsIn(DvrpLoad other) {
		IntegerLoad integerLoad = check(other);
		return this.value <= integerLoad.value;
	}

	@Override
	public boolean isEmpty() {
		return this.value == 0;
	}

	@Override
	public Number getElement(int index) {
		if(index > 0) {
			throw new IndexOutOfBoundsException();
		}

		return value;
	}

	public int getValue() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof IntegerLoad otherLoad) {
			return this.value == otherLoad.value;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode() + this.value;
	}

	static public IntegerLoad fromValue(int value) {
		return new IntegerLoad(value);
	}
}
