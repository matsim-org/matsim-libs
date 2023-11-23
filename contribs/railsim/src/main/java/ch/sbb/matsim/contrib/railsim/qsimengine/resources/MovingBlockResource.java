package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimCalc;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A moving block, which allows multiple trains and needs to make sure that they don't collide.
 */
final class MovingBlockResource implements RailResourceInternal {

	private final Id<RailResource> id;
	private final List<RailLink> links;
	private final Track[] tracks;

	/**
	 * Links that are reserved by trains.
	 */
	private final Map<RailLink, Set<MobsimDriverAgent>> moving = new HashMap<>();

	/**
	 * Map driver on segment to their incoming link.
	 */
	private final Map<MobsimDriverAgent, TrainEntry> reservations;

	MovingBlockResource(Id<RailResource> id, List<RailLink> links) {
		this.id = id;
		this.links = links;

		int capacity = links.stream().mapToInt(l -> l.tracks).min().orElseThrow();
		this.tracks = new Track[capacity];

		for (int i = 0; i < capacity; i++) {
			tracks[i] = new Track();
		}

		this.reservations = new HashMap<>();

		for (RailLink link : links) {
			moving.put(link, new HashSet<>());
		}
	}

	@Override
	public ResourceType getType() {
		return ResourceType.movingBlock;
	}

	@Override
	public List<RailLink> getLinks() {
		return links;
	}

	@Override
	public ResourceState getState(RailLink link) {
		// All links have the same state
		int used = 0;
		for (Track track : tracks) {
			if (track.incoming == null)
				used++;
		}

		if (used == 0)
			return ResourceState.EMPTY;

		return ResourceState.IN_USE;
	}

	@Override
	public boolean hasCapacity(double time, RailLink link, int track, TrainPosition position) {

		TrainEntry entry = reservations.get(position.getDriver());

		// No entry yet
		if (entry == null && track == RailResourceManager.ANY_TRACK) {
			track = chooseTrack(link);

			// No track was available
			if (track == RailResourceManager.ANY_TRACK)
				return false;
		} else if (entry != null && track == RailResourceManager.ANY_TRACK) {
			track = entry.track;
		} else if (entry != null && track != entry.track) {
			throw new IllegalStateException("Train is already on a different track.");
		}

		// entry should store track
		double dist = checkReserve(time, link, track, entry, position);

		return dist > 0;
	}

	@Override
	public double getReservedDist(RailLink link, TrainPosition position) {

		TrainEntry entry = reservations.get(position.getDriver());
		if (entry == null)
			return NO_RESERVATION;

		return entry.reservedDistance;
	}

	@Override
	public double reserve(double time, RailLink link, int track, TrainPosition position) {

		moving.get(link).add(position.getDriver());

		// store trains incoming link
		if (!reservations.containsKey(position.getDriver())) {

			if (track == RailResourceManager.ANY_TRACK)
				track = chooseTrack(link);

			TrainEntry e = new TrainEntry(track, position);
			Track t = tracks[track];

			reservations.put(position.getDriver(), e);

			// Mark this as used by this direction
			if (t.incoming == null)
				t.incoming = link;

			t.queue.add(e);
		}

		TrainEntry self = reservations.get(position.getDriver());

		double dist = checkReserve(time, link, self.track, self, position);

		self.reservedDistance = dist;

		return dist;
	}

	/**
	 * Chooses a track for trains that don't have one yet.
	 */
	private int chooseTrack(RailLink link) {
		int same = -1;
		int available = -1;
		int free = 0;

		for (int i = 0; i < tracks.length; i++) {
			if (tracks[i].incoming == link)
				same = i;
			else if (tracks[i].incoming == null) {
				available = i;
				free++;
			}
		}

		// If there is more than one free track, a new one is opened
		if (same != -1)
			return free > 1 ? available : same;

		return available;
	}

	/**
	 * Compute the distance that can be reserved without actually reserving it.
	 */
	private double checkReserve(double time, RailLink link, int track,
								@Nullable TrainEntry entry, TrainPosition position) {

		List<TrainEntry> queue = tracks[track].queue;

		// Approve whole link length for the first train in the queue
		int idx = entry != null ? queue.indexOf(entry) : queue.size();
		if (idx == 0) {
			return link.length;
		}

		// The train in front of this one
		TrainEntry inFront = queue.get(idx - 1);

		// tail is on the same link
		if (Objects.equals(inFront.position.getTailLink(), link.getLinkId())) {

			// available distance can be at most the link length
            return Math.min(
				link.length,
				inFront.position.getTailPosition() + RailsimCalc.projectedDistance(time, inFront.position)
			);
		}

		// train is moving on this link, and it is not the tail -> no capacity at all
		if (moving.get(link).contains(inFront.position.getDriver())) {
			return 0;
		}

		// might only happen if train passed the link completely already
		throw new IllegalStateException("This situation is not yet tested/implemented.");
	}

	@Override
	public void release(RailLink link, MobsimDriverAgent driver) {

		moving.get(link).remove(driver);

		boolean allFree = true;
		for (Set<MobsimDriverAgent> others : moving.values()) {
			if (others.contains(driver)) {
				allFree = false;
				break;
			}
		}

		// Remove this train from the incoming map
		if (allFree) {

			TrainEntry entry = reservations.remove(driver);

			Track track = tracks[entry.track];
			track.queue.removeIf(p -> p.position.getDriver() == driver);

			// This track is completely free again
			if (track.queue.isEmpty())
				track.incoming = null;
		}
	}

	/**
	 * Class to keep track of train positions and reservations.
	 */
	private static final class TrainEntry {

		final int track;
		final TrainPosition position;

		double reservedDistance;

		public TrainEntry(int track, TrainPosition position) {
			this.track = track;
			this.position = position;
		}
	}

	/**
	 * One instance for each available track.
	 */
	private static final class Track {

		/**
		 * Link used to enter this track, which indicates the direction.
		 */
		private RailLink incoming;

		/**
		 * Train currently on this track.
		 */
		private final List<TrainEntry> queue = new ArrayList<>();

	}
}
