package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * The car legs a run actually drove, as node-to-node travel times: what the matrix is supposed to
 * predict, measured on the trips that really happened.
 * <p>
 * This is the other kind of comparison, next to {@link RoutedTravelTimes}, and the two answer different
 * questions. The routing reference asks <em>how well does the matrix approximate a time-dependent
 * router on these travel times</em> - the error of the approximation alone, on any pair one likes.
 * This asks <em>how well does the matrix predict what the simulation did</em>, which also contains
 * everything the matrix cannot know: that an agent took a route a router would not have taken, that it
 * sat in a queue the average index does not describe, that it departed at a moment the bin averages
 * over. The first error is a property of the method, the second is the one an analysis using the matrix
 * actually suffers.
 * <p>
 * <b>Node to node, exactly.</b> A leg is not compared as the agent experienced it from door to door,
 * because that is not the question a matrix answers. The qsim inserts a vehicle at the end of its first
 * link and ends the leg after it has driven its last link, so a leg's own travel time covers a
 * first-link insertion delay and a full last link, neither of which is in the matrix. Instead the
 * journey is cut at the nodes: the clock starts when the vehicle <b>leaves its first link</b> (passing
 * {@code toNode(startLink)}) and stops when it <b>enters its last link</b> (passing
 * {@code fromNode(endLink)}), which is precisely the interval {@code getTravelTime(fromNode, toNode, t)}
 * is about. No correction term, no last-link convention, nothing to get wrong - and
 * {@link Leg#departureTime()} is the moment the vehicle passed the first node, which is the departure
 * time the matrix is then asked about.
 * <p>
 * <b>Only legs that stayed inside the study area</b> are kept. The matrix is built over the subnetwork,
 * so for a leg that left the area and came back it would answer about a path through the area that the
 * vehicle never drove; counting that as an error of the congestion model would be wrong. How many legs
 * were dropped for this is logged, and a large share is the sign that the buffer around the city is too
 * small.
 * <p>
 * Collected as an {@link org.matsim.core.events.handler.EventHandler} on the pass
 * {@link org.matsim.contrib.msp.traveltimematrix.prepare.MeasuredTraffic} already makes over the events,
 * and capped by a reservoir sample, because a city-sized run has millions of legs and a few tens of
 * thousands are plenty for a MAPE.
 *
 * @see RunValidateTravelTimeMatrix the command
 */
public final class ExperiencedLegs implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
	LinkEnterEventHandler, LinkLeaveEventHandler {

	private static final Logger log = LogManager.getLogger(ExperiencedLegs.class);

	/**
	 * One car leg of the run, cut at the nodes.
	 *
	 * @param fromNode the end of the leg's first link, where the clock starts
	 * @param toNode the start of the leg's last link, where it stops
	 * @param departureTime when the vehicle passed {@code fromNode} [s]
	 * @param travelTime what it took to get from {@code fromNode} to {@code toNode} [s]
	 * @param distance the length of the links driven in between [m]
	 */
	public record Leg(Node fromNode, Node toNode, double departureTime, double travelTime, double distance) {
	}

	/**
	 * The state of a leg that is being driven, kept per vehicle while the events come in.
	 */
	private static final class OngoingLeg {

		private final Link startLink;
		private double leftFirstLink = Double.NaN;
		private Link lastEnteredLink;
		private double lastEnteredAt = Double.NaN;
		private double distance;
		private boolean leftTheStudyArea;

		private OngoingLeg(Link startLink) {
			this.startLink = startLink;
		}
	}

	private final Network network;
	private final String mode;
	private final int maximumNumberOfLegs;
	private final SplittableRandom random;

	private final Map<Id<Vehicle>, OngoingLeg> ongoing = new HashMap<>();
	private final List<Leg> legs = new ArrayList<>();

	private long seen;
	private long outsideTheStudyArea;
	private long tooShort;

	/**
	 * @param network the study area subnetwork the matrix is built over. A link that is not in it is a
	 * link outside the study area, which is how a leg is found to have left it.
	 * @param mode the network mode to collect, usually {@link org.matsim.api.core.v01.TransportMode#car}
	 * @param maximumNumberOfLegs how many legs to keep. Beyond that a reservoir sample replaces one at
	 * random, so the kept legs are a uniform sample of all of them and not just the early ones - the
	 * morning peak must not be the whole sample.
	 * @param seed of that sampling
	 */
	public ExperiencedLegs(Network network, String mode, int maximumNumberOfLegs, long seed) {
		this.network = network;
		this.mode = mode;
		this.maximumNumberOfLegs = maximumNumberOfLegs;
		this.random = new SplittableRandom(seed);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (!this.mode.equals(event.getNetworkMode())) {
			return;
		}
		// the link may be outside the study area; the leg is then dropped when it ends, not here, so that
		// the count of what was dropped is a count of legs and not of events
		this.ongoing.put(event.getVehicleId(), new OngoingLeg(this.network.getLinks().get(event.getLinkId())));
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		OngoingLeg leg = this.ongoing.get(event.getVehicleId());
		if (leg != null && Double.isNaN(leg.leftFirstLink)) {
			leg.leftFirstLink = event.getTime();
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {

		OngoingLeg leg = this.ongoing.get(event.getVehicleId());
		if (leg == null) {
			return;
		}

		// the length of the link entered before this one: the distance between the two nodes the leg is
		// cut at is the sum of the links in between, so the last link entered is not part of it
		if (leg.lastEnteredLink != null) {
			leg.distance += leg.lastEnteredLink.getLength();
		}

		Link link = this.network.getLinks().get(event.getLinkId());
		if (link == null) {
			leg.leftTheStudyArea = true;
		}
		leg.lastEnteredLink = link;
		leg.lastEnteredAt = event.getTime();
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {

		OngoingLeg leg = this.ongoing.remove(event.getVehicleId());
		if (leg == null || !this.mode.equals(event.getNetworkMode())) {
			return;
		}

		this.seen++;

		if (leg.startLink == null || leg.leftTheStudyArea || leg.lastEnteredLink == null
			|| !leg.lastEnteredLink.getId().equals(event.getLinkId())) {
			this.outsideTheStudyArea++;
			return;
		}

		double travelTime = leg.lastEnteredAt - leg.leftFirstLink;
		if (Double.isNaN(travelTime) || travelTime <= 0) {
			// a leg of one or two links: its two nodes are the same node, or the vehicle left its first
			// link and entered its last in the same instant. There is no travel time between those nodes
			// to predict.
			this.tooShort++;
			return;
		}

		add(new Leg(leg.startLink.getToNode(), leg.lastEnteredLink.getFromNode(), leg.leftFirstLink, travelTime,
			leg.distance));
	}

	/**
	 * Reservoir sampling: the first {@code maximumNumberOfLegs} are kept, and the n-th leg after that
	 * replaces a random one of them with probability {@code max/n}.
	 */
	private void add(Leg leg) {
		if (this.legs.size() < this.maximumNumberOfLegs) {
			this.legs.add(leg);
			return;
		}
		long kept = this.seen - this.outsideTheStudyArea - this.tooShort;
		int replace = this.random.nextInt((int) Math.min(Integer.MAX_VALUE, kept));
		if (replace < this.maximumNumberOfLegs) {
			this.legs.set(replace, leg);
		}
	}

	@Override
	public void reset(int iteration) {
		this.ongoing.clear();
		this.legs.clear();
		this.seen = 0;
		this.outsideTheStudyArea = 0;
		this.tooShort = 0;
	}

	/**
	 * @return the legs that were kept. Node-to-node, inside the study area, in no particular order.
	 * @throws IllegalStateException if there are none, which means the events and the study area do not
	 * belong together rather than that the run had no traffic
	 */
	public List<Leg> getLegs() {

		if (this.legs.isEmpty()) {
			throw new IllegalStateException("Not one " + this.mode + " leg of the run stayed inside the study area "
				+ "network of " + this.network.getLinks().size() + " links (" + this.seen + " legs seen, "
				+ this.outsideTheStudyArea + " of them outside it, " + this.tooShort + " too short to have a "
				+ "node-to-node travel time). Check that the events and the shape file are from the same scenario, "
				+ "and that --modes names the mode the run actually used.");
		}

		log.info("Kept {} of {} {} legs of the run as node-to-node travel times: {} left the study area network and "
				+ "{} were too short to have one. Mean experienced travel time {} s, mean distance {} m.",
			this.legs.size(), this.seen, this.mode, this.outsideTheStudyArea, this.tooShort,
			String.format(Locale.ROOT, "%.1f", this.legs.stream().mapToDouble(Leg::travelTime).average().orElse(0)),
			String.format(Locale.ROOT, "%.0f", this.legs.stream().mapToDouble(Leg::distance).average().orElse(0)));

		return List.copyOf(this.legs);
	}

	/**
	 * @return how many legs of the mode were seen, kept or not
	 */
	public long getNumberOfLegsSeen() {
		return this.seen;
	}

	/**
	 * @return how many were dropped for leaving the study area. A large share means the buffer around
	 * the city is too small - see
	 * {@link org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork}.
	 */
	public long getNumberOfLegsOutsideTheStudyArea() {
		return this.outsideTheStudyArea;
	}

	/**
	 * The legs as queries for {@link MatrixEvaluation}: the experienced travel time becomes the
	 * reference each variant is compared against.
	 * <p>
	 * The free-speed travel time is left unknown, unlike in the sampled study: here it is not a separate
	 * piece of information but one of the variants - the {@code FREE_SPEED} row of the error table is
	 * exactly the free-speed travel time of every one of these pairs.
	 */
	public List<MatrixEvaluation.Query> asQueries() {
		return getLegs().stream()
			.map(leg -> new MatrixEvaluation.Query(leg.fromNode(), leg.toNode(), leg.departureTime(),
				leg.travelTime(), leg.distance(), Double.NaN))
			.toList();
	}
}
