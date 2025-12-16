package org.matsim.modechoice;


/**
 * Functional interface to filter mode and trip combinations.
 */
@FunctionalInterface
public interface TripModeFilter {

	/**
	 * Don't filter any mode.
	 */
	static final TripModeFilter ACCEPT_ALL = (mode, tripIdx) -> true;

	/**
	 * Decide whether mode on trip is accepted.
	 */
	boolean accept(String mode, int tripIdx);

}
