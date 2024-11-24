package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class IntegerLoadType extends ScalarLoadType {
	private static final Logger LOGGER = LogManager.getLogger(IntegerLoadType.class);
	public IntegerLoadType(String name, String slotName) {
		super(name, slotName);
	}

	@Override
	public IntegerLoad fromNumber(Number load) {
		// TODO should just check and throw an exception if it's not an integer ?
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
