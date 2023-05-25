package ch.sbb.matsim.contrib.railsim.qsimengine;

/**
 * Current state of a track.
 */
public enum TrackState {
	FREE,

	/**
	 * Blocked tracks that are exclusively available for trains.
	 */
	BLOCKED,
}
