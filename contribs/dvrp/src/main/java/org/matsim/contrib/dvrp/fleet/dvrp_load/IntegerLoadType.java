package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpLoadType;

public abstract class IntegerLoadType extends ScalarLoadType {
	private static final Logger LOGGER = LogManager.getLogger(IntegerLoadType.class);

	public IntegerLoadType(Id<DvrpLoadType> id, String slotName) {
		super(id, slotName);
	}

	@Override
	public IntegerLoad fromNumber(Number load) {
		// TODO[IRTX] should just check and throw an exception if it's not an integer ?
		if(!(load instanceof Integer)) {
			LOGGER.info("Passed number representing load is not an integer, converting it");
		}
		return fromInt(load.intValue());
	}

	public abstract IntegerLoad fromInt(int load);

	public final IntegerLoad getEmptyLoad() {
		return this.fromInt(0);
	}
}
