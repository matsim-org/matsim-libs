package org.matsim.contrib.dvrp.fleet.dvrp_load;

/**
 * This abstract class serves as a base type for {@link DvrpLoad} implementations that consist of a single (usually number) value.
 * This means that such loads have only one dimension/slot. This is mainly handled in {@link ScalarLoadType}.
 * Here we just force subclasses to implement {@link DvrpLoad} methods that return a {@link DvrpLoad} ({@link DvrpLoad#add(DvrpLoad)} and {@link DvrpLoad#subtract(DvrpLoad)}) or a {@link DvrpLoadType} ({@link DvrpLoad#getType()}) to have a more precise signature, returning {@link ScalarLoad} and {@link ScalarLoadType} respectively.
 * @author tarek.chouaki
 */
public abstract class ScalarLoad implements DvrpLoad {

	private final ScalarLoadType scalarLoadType;

	public ScalarLoad(ScalarLoadType scalarLoadType) {
		this.scalarLoadType = scalarLoadType;
	}

	@Override
	public ScalarLoadType getType() {
		return this.scalarLoadType;
	}
	// These methods are redefined here with more precise signature
	public abstract ScalarLoad add(DvrpLoad load);
	public abstract ScalarLoad subtract(DvrpLoad load);
}
