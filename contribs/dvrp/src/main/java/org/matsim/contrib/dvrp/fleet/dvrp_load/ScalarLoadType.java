package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.matsim.api.core.v01.Id;

/**
 * This abstract class serves as a basis for writing {@link DvrpLoadType} implementations that are scalar, i.e. comprising one slot/dimension.
 * New instances of the related {@link DvrpLoad} implementations must be buildable from one Number only. For this, the {@link ScalarLoadType#fromArray(Number[])} checks whether the passed array consists of only one number and delegates the instance creation to {@link ScalarLoadType#fromNumber(Number)} defined in subclasses.
 * Moreover, the equals method is defined here. It supposes that two {@link ScalarLoadType} objects represent the same type if they are of the same class and have the same id and slot name.
 * @author Tarek Chouaki (tkchouaki)
 */
public abstract class ScalarLoadType implements DvrpLoadType {

	private final Id<DvrpLoadType> id;
	private final String slotName;

	public ScalarLoadType(Id<DvrpLoadType> id, String slotName) {
		this.id = id;
		this.slotName = slotName;
	}

	@Override
	public final Id<DvrpLoadType> getId() {
		return this.id;
	}

	@Override
	public final String[] getSlotNames() {
		return new String[]{this.slotName};
	}

	@Override
	public final ScalarLoad fromArray(Number[] numbers) {
		if(numbers.length == 1) {
			return fromNumber(numbers[0]);
		}
		throw new IllegalStateException();
	}
	public abstract ScalarLoad fromNumber(Number number);

	// Redefining this method here with a more precise signature
	public abstract ScalarLoad getEmptyLoad();

	public boolean equals(Object other) {
		// In practice, only one instance of ScalarLoadType should be created per implementation
		// So the two if clauses below will quickly do the check in most cases
		if(this == other) {
			return true;
		}
		if(!this.getClass().equals(other.getClass())) {
			return false;
		}
		ScalarLoadType scalarLoadType = (ScalarLoadType) other;
		return this.slotName.equals(scalarLoadType.slotName) && this.id.equals(scalarLoadType.id);
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode() + this.getId().hashCode() + this.slotName.hashCode();
	}
}
