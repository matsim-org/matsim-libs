package org.matsim.modechoice;

/**
 * Mode availability enumeration with different options.
 * Different options are required in case there a different pricing schemes, depending on the usage of the mode.
 */
public enum ModeAvailability {

	YES,
	NO,

	MONTHLY_SUBSCRIPTION,
	YEARLY_SUBSCRIPTION,
	DAILY_TICKET,

	/**
	 * May be used if the other options are not applicable.
	 */
	OTHER;

	/**
	 * True for all options except {@link #NO}.
	 */
	public boolean isModeAvailable() {
		return this != NO;
	}

}
