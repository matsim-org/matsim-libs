package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Rail links which can has multiple tracks and corresponds to exactly one link.
 */
public final class RailLink implements HasLinkId {

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
	final double freeSpeed;
	final double minimumHeadwayTime;

	/**
	 * Id of the resource this link belongs to.
	 */
	@Nullable
	final Id<RailResource> resource;

	public RailLink(Link link) {
		id = link.getId();
		state = new TrackState[RailsimUtils.getTrainCapacity(link)];
		Arrays.fill(state, TrackState.FREE);
		reservations = new MobsimDriverAgent[state.length];
		length = link.getLength();
		freeSpeed = link.getFreespeed();
		minimumHeadwayTime = RailsimUtils.getMinimumTrainHeadwayTime(link);
		String resourceId = RailsimUtils.getResourceId(link);
		resource = resourceId != null ? Id.create(resourceId, RailResource.class) : null;
	}

	@Override
	public Id<Link> getLinkId() {
		return id;
	}

	@Nullable
	public Id<RailResource> getResourceId() {
		return resource;
	}

	/**
	 * Number of tracks on this link.
	 */
	public int getNumberOfTracks(){
		return state.length;
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
	 * Check if driver has already reserved this link.
	 */
	public boolean isBlockedBy(MobsimDriverAgent driver) {
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
			if (state[i] == TrackState.FREE) {
				reservations[i] = driver;
				state[i] = TrackState.BLOCKED;
				return i;
			}
		}
		throw new IllegalStateException("No track was free.");
	}

	/**
	 * Release a non-free track to be free again.
	 */
	public int releaseTrack(MobsimDriverAgent driver) {
		for (int i = 0; i < state.length; i++) {
			if (reservations[i] == driver) {
				state[i] = TrackState.FREE;
				reservations[i] = null;
				return i;
			}
		}
		throw new IllegalStateException("Driver " + driver + " has not reserved the track.");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RailLink link = (RailLink) o;
		return Objects.equals(id, link.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "RailLink{" +
			"id=" + id +
			", resource=" + resource +
			'}';
	}
}
