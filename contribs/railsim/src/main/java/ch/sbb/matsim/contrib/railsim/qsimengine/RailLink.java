package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.Arrays;

/**
 * Rail tracks in railsim, which corresponds to a MATSim link, but with additional states.
 */
class RailLink {

	private final Id<Link> id;

	/**
	 * States per track.
	 */
	private final TrackState[] state;

	/**
	 * Reservations held for each track.
	 */
	private final MobsimDriverAgent[] reservations;

	final double length;
	final double minimumHeadwayTime;

	public RailLink(Link link) {
		id = link.getId();
		state = new TrackState[RailsimUtils.getTrainCapacity(link)];
		Arrays.fill(state, TrackState.FREE);
		reservations = new MobsimDriverAgent[state.length];
		length = link.getLength();
		minimumHeadwayTime = RailsimUtils.getMinimumTrainHeadwayTime(link);
	}

	public Id<Link> getId() {
		return id;
	}

	/**
	 * Number of tracks on this segment.
	 */
	public int getTracks() {
		return state.length;
	}


	/**
	 * Returns the allowed freespeed, depending on the context, which is given via driver.
	 */
	public double getAllowedFreespeed(MobsimDriverAgent driver) {

		// TODO: every context information such as vehicle, or transit line is stored in the driver
		// can be retrieved here using the utils

		return 30;
	}

	/**
	 * Whether at least one track is free.
	 */
	public boolean hasFreeTrack() {
		for (TrackState trackState : state) {
			if (trackState == TrackState.FREE)
				return true;
		}
		return false;
	}

	/**
	 * Reserve a track for a specific driver.
	 */
	public int reserveTrack(MobsimDriverAgent driver) {
		for (int i = 0; i < state.length; i++) {
			if (state[i] == TrackState.FREE) {
				state[i] = TrackState.RESERVED;
				reservations[i] = driver;
				return i;
			}
		}
		throw new IllegalStateException("No track was free.");
	}

	/**
	 * Block a track that was previously reserved.
	 */
	public int blockTrack(MobsimDriverAgent driver) {
		for (int i = 0; i < state.length; i++) {
			if (reservations[i] == driver) {
				state[i] = TrackState.BLOCKED;
				return i;
			}
		}
		throw new IllegalStateException("No track was reserved to be blocked.");
	}

	/**
	 * Release a non-free track to be free again.
	 */
	public void releaseTrack(MobsimDriverAgent driver) {
		for (int i = 0; i < state.length; i++) {
			if (reservations[i] == driver) {
				state[i] = TrackState.FREE;
				reservations[i] = null;
				return;
			}
		}
		throw new IllegalStateException("Driver " + driver + " has not reserved the track.");
	}
}
