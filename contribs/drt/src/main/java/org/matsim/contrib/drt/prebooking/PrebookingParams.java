package org.matsim.contrib.drt.prebooking;

import org.matsim.core.config.ReflectiveConfigGroup;

public class PrebookingParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "prebooking";

	public PrebookingParams() {
		super(SET_NAME);
	}

	@Parameter
	@Comment("Defines whether vehicles drive immediately to the next"
			+ "(prebooked) future task and wait for the planned stop to begin, or wait at the current"
			+ "position and depart to arrive on time at the following stop. The latter behavior (not"
			+ "the default) may lead to larger ucnertainty in highly congested scenarios.")
	public boolean scheduleWaitBeforeDrive = false; // in the future, this could also become a double value indicating
													// how many minutes before the next stop the vehicle should plan to
													// be there
}