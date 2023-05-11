package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Rail tracks in railsim, which may corresponds to multiple MATSim links (if there are opposing links).
 * Therefor this kind of track is bidirectional.
 */
final class RailLink implements HasLinkId {

	private final Id<Link> id;

	@Nullable
	private final Id<Link> oppositeId;

	/**
	 * States per track.
	 */
	private final TrackState[] state;

	/**
	 * Reservations held for each track.
	 */
	private final MobsimDriverAgent[] reservations;

	final double length;
	final double freeSpeed;
	final double minimumHeadwayTime;

	// TODO: from and to node most likely needed at some point
	// A node can be blocked if a train is crossing the path

	public RailLink(Link link, @Nullable Id<Link> opposite) {
		id = link.getId();
		oppositeId = opposite;
		state = new TrackState[RailsimUtils.getTrainCapacity(link)];
		Arrays.fill(state, TrackState.FREE);
		reservations = new MobsimDriverAgent[state.length];
		length = link.getLength();
		freeSpeed = link.getFreespeed();
		minimumHeadwayTime = RailsimUtils.getMinimumTrainHeadwayTime(link);
	}

	@Override
	public Id<Link> getLinkId() {
		return id;
	}

	/**
	 * Returns the allowed freespeed, depending on the context, which is given via driver.
	 */
	public double getAllowedFreespeed(MobsimDriverAgent driver) {

		// TODO: additional context information such as transit line is stored in the driver
		// TODO: speed depending on vehicle type

		return Math.min(freeSpeed, driver.getVehicle().getVehicle().getType().getMaximumVelocity());
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
	 *
	 * @return -1 if not track was free, otherwise track number.
	 */
	public int reserveTrack(MobsimDriverAgent driver) {
		for (int i = 0; i < state.length; i++) {
			if (state[i] == TrackState.FREE) {
				state[i] = TrackState.RESERVED;
				reservations[i] = driver;
				return i;
			}
		}

		return -1;
	}

	/**
	 * Check if driver has already reserved this link.
	 */
	public boolean isReserved(MobsimDriverAgent driver) {
		for (MobsimDriverAgent reservation : reservations) {
			if (reservation == driver)
				return true;
		}
		return false;
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
