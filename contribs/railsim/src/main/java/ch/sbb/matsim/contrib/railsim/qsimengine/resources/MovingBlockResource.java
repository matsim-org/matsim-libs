package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.*;

/**
 * A moving block, which allows multiple trains and needs to make sure that they don't collide.
 */
final class MovingBlockResource implements RailResourceInternal {

	private final Id<RailResource> id;
	private final List<RailLink> links;
	private final int capacity;

	/**
	 * Trains on this segment by their incoming link.
	 */
	private final Map<RailLink, List<TrainEntry>> queues = new HashMap<>();

	/**
	 * Currently moving trains per link.
	 */
	private final Map<RailLink, Set<MobsimDriverAgent>> moving = new HashMap<>();

	/**
	 * Map driver on segment to their incoming link.
	 */
	private final Map<MobsimDriverAgent, TrainEntry> reservations;

	MovingBlockResource(Id<RailResource> id, List<RailLink> links) {
		this.id = id;
		this.links = links;
		this.capacity = links.stream().mapToInt(l -> l.tracks).min().orElseThrow();
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
		return queues.isEmpty() ? ResourceState.EMPTY : ResourceState.IN_USE;
	}

	@Override
	public boolean hasCapacity(RailLink link, TrainPosition position) {
		// Train already present will have capacity
		if (reservations.containsKey(position.getDriver()))
			return true;

		return queues.size() < capacity;
	}

	@Override
	public double getReservedDist(RailLink link, TrainPosition position) {

		TrainEntry entry = reservations.get(position.getDriver());
		if (entry == null)
			return NO_RESERVATION;

		// return remaining reserved distance
		if (entry.lastLink.getLinkId().equals(position.getHeadLink())) {
			double diff = position.getHeadPosition() - entry.lastHeadPosition;
			return entry.reservedDistance - diff;
		}

		return 0;
	}

	@Override
	public double reserve(RailLink link, TrainPosition position) {

		moving.get(link).add(position.getDriver());

		// store trains incoming link
		if (!reservations.containsKey(position.getDriver())) {

			TrainEntry e = new TrainEntry(link, position);

			reservations.put(position.getDriver(), e);
			queues.computeIfAbsent(link, l -> new ArrayList<>()).add(e);
		}

		TrainEntry self = reservations.get(position.getDriver());
		List<TrainEntry> queue = queues.get(self.entryLink);


		// Approve whole link length for the first train in the queue
		int idx = queue.indexOf(self);
		if (idx == 0) {

			if (link.getLinkId().equals(position.getHeadLink()))
				self.reservedDistance = link.length - position.getHeadPosition();
			else
				self.reservedDistance = link.length;

			self.lastHeadPosition = position.getHeadPosition();
			self.lastLink = link;

			return self.reservedDistance;
		}

		// The train in front of this one
		TrainEntry inFront = queue.get(idx - 1);

		// TODO: implement

		return 0;
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
			Iterator<Map.Entry<RailLink, List<TrainEntry>>> it = queues.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<RailLink, List<TrainEntry>> e = it.next();
				e.getValue().removeIf(p -> p.position.getDriver() == driver);

				// Remove the whole entry if no more trains are incoming
				if (e.getValue().isEmpty())
					it.remove();
			}

			reservations.remove(driver);
		}
	}

	/**
	 * Class to keep track of train positions and reservations.
	 */
	private static final class TrainEntry {

		final RailLink entryLink;
		final TrainPosition position;

		double reservedDistance;
		double lastHeadPosition;
		RailLink lastLink;

		public TrainEntry(RailLink entryLink, TrainPosition position) {
			this.entryLink = entryLink;
			this.position = position;
		}
	}
}
