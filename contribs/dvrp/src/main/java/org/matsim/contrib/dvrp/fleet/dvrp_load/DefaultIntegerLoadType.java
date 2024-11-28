package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpLoadType;

public class DefaultIntegerLoadType extends IntegerLoadType {

	public static final String TYPE_NAME = "defaultLoadType";
	public static final String SLOT_NAME = "load";

	public DefaultIntegerLoadType() {
		super(Id.create(TYPE_NAME, DvrpLoadType.class), SLOT_NAME);
	}

	@Override
	public IntegerLoad fromInt(int load) {
		return new IntegerLoad(load, this);
	}


}
