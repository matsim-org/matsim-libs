package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.matsim.api.core.v01.Id;

/**
 * This is the default implementation of {@link IntegerLoadType} that is bound by default for every DVRP mode.
 * It allows to keep the compatibility with simple integer representation of homogeneous vehicle capacities and loads
 * @author Tarek Chouaki (tkchouaki)
 */
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
