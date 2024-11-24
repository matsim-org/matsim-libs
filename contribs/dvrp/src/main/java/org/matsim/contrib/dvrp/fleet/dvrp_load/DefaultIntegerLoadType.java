package org.matsim.contrib.dvrp.fleet.dvrp_load;

public class DefaultIntegerLoadType extends IntegerLoadType {
	private final IntegerLoad emptyLoad = new IntegerLoad(0, this);
	public DefaultIntegerLoadType() {
		super("defaultLoadType", "load");
	}

	@Override
	public IntegerLoad fromInt(int load) {
		return new IntegerLoad(load, this);
	}
}
