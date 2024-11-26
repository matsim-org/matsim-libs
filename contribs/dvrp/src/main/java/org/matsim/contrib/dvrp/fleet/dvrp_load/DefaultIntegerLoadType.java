package org.matsim.contrib.dvrp.fleet.dvrp_load;

public class DefaultIntegerLoadType extends IntegerLoadType {

	public static final String TYPE_NAME = "defaultLoadType";
	public static final String SLOT_NAME = "load";

	private final IntegerLoad emptyLoad = new IntegerLoad(0, this);
	public DefaultIntegerLoadType() {
		super(TYPE_NAME, SLOT_NAME);
	}

	@Override
	public IntegerLoad fromInt(int load) {
		return new IntegerLoad(load, this);
	}
}
