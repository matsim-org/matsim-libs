package org.matsim.contrib.msp.capability.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.msp.capability.structure.FundamentalPlanExtractor;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.Facility;
import org.matsim.facilities.OpeningTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A place at which a discretionary activity can be performed: the opportunity the capability analysis
 * tries to insert into a {@link FundamentalPlanExtractor fundamental plan}.
 * <p>
 * This is a view on a MATSim {@link ActivityFacility} rather than a replacement for it. Everything the
 * analysis needs is already expressible in a standard MATSim facilities file — location as
 * {@code x}/{@code y}/{@code linkId}, opening hours as {@code <opentime>} of an
 * {@code <activity>}, type and sub-type as {@code <attributes>} — so the facilities stay readable and
 * writable with {@link org.matsim.facilities.MatsimFacilitiesReader} and
 * {@link org.matsim.facilities.FacilitiesWriter}, and they stay usable by the rest of MATSim. What
 * this class adds is the capability vocabulary on top:
 * <ul>
 *     <li>{@link #getType()} — what can be done here (shop, leisure, restaurant, bar, ...). Defined
 *     by whoever builds the facilities; it is deliberately <em>not</em> tied to the MATSim activity
 *     types of the scenario.</li>
 *     <li>{@link #getSubType()} — which kind of it (the brand of the supermarket, the kind of
 *     restaurant, ...), the basis of the variety analysis. Optional.</li>
 *     <li>{@link #getOpeningTimes()} — when the place is open, and with it {@link #isOpenDuring},
 *     the question the insertion step asks of a gap in the plan.</li>
 * </ul>
 * One instance stands for one type offered at one facility. A facility that offers several types —
 * a mall where one can both shop and eat — is therefore represented by several instances sharing an
 * {@link #getId() id}.
 * <p>
 * Implements {@link Facility}, so an instance can be handed to a router directly as a trip end.
 * Instances are immutable.
 */
public final class DiscretionaryActivityFacility implements Facility {

	private final ActivityFacility facility;
	private final String type;
	private final String subType;
	private final List<OpeningTime> openingTimes;

	/**
	 * @param facility the MATSim facility this is a view on, not null
	 * @param type what can be done here, not null
	 * @param subType which kind of it, null if unknown
	 * @param openingTimes when the place is open. Empty means always open, see
	 * {@link #getOpeningTimes()}.
	 */
	public DiscretionaryActivityFacility(ActivityFacility facility, String type, String subType,
								   Collection<OpeningTime> openingTimes) {
		this.facility = Objects.requireNonNull(facility, "a facility is required");
		this.type = Objects.requireNonNull(type, "a type is required");
		this.subType = subType;

		// sorted by hand rather than kept in a SortedSet: OpeningTimeImpl.compareTo is not a natural
		// ordering but an interval relation, and it reports the earlier of two disjoint windows as the
		// greater one, so a TreeSet of them comes out in reverse
		List<OpeningTime> sorted = new ArrayList<>(openingTimes);
		sorted.sort(Comparator.comparingDouble(OpeningTime::getStartTime).thenComparingDouble(OpeningTime::getEndTime));
		this.openingTimes = Collections.unmodifiableList(sorted);
	}

	/**
	 * Creates a facility that is open all day and has no sub-type.
	 */
	public static DiscretionaryActivityFacility create(ActivityFacility facility, String type) {
		return new DiscretionaryActivityFacility(facility, type, null, Collections.emptySet());
	}

	/**
	 * @return the id of the underlying MATSim facility. Not unique among discretionary activity
	 * facilities: a facility offering several types has one instance per type.
	 */
	public Id<ActivityFacility> getId() {
		return this.facility.getId();
	}

	/**
	 * @return what can be done here, e.g. {@code shop} or {@code restaurant}. A vocabulary of its own,
	 * not necessarily one of the MATSim activity types of the scenario.
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * @return which kind of {@link #getType()} this is, e.g. the brand of the supermarket or the kind
	 * of restaurant. Empty if the facilities do not say.
	 */
	public Optional<String> getSubType() {
		return Optional.ofNullable(this.subType);
	}

	/**
	 * @return the name of the place, e.g. "Konsum Striesen". Taken from the description of the MATSim
	 * facility, which is what its {@code desc} attribute round trips. Empty if the facility has none.
	 */
	public Optional<String> getName() {
		if (this.facility instanceof ActivityFacilityImpl impl
			&& impl.getDesc() != null && !impl.getDesc().isBlank()) {
			return Optional.of(impl.getDesc());
		}
		return Optional.empty();
	}

	/**
	 * @return the MATSim facility this is a view on, e.g. to reach its activity options or attributes
	 */
	public ActivityFacility getFacility() {
		return this.facility;
	}

	@Override
	public Coord getCoord() {
		return this.facility.getCoord();
	}

	@Override
	public Id<Link> getLinkId() {
		return this.facility.getLinkId();
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return this.facility.getCustomAttributes();
	}

	/**
	 * @return the windows during which the place is open, ordered by start time. Empty if it is always
	 * open — that is the convention of {@link org.matsim.facilities.ActivityOption}, where an activity
	 * option without any {@code <opentime>} is open around the clock. A single window may be open ended
	 * on either side, see {@link OpeningTime}.
	 */
	public List<OpeningTime> getOpeningTimes() {
		return this.openingTimes;
	}

	/**
	 * @return true if the place is open around the clock, i.e. no opening times are given
	 */
	public boolean isAlwaysOpen() {
		return this.openingTimes.isEmpty();
	}

	/**
	 * @return when the place opens, i.e. the start of its first window.
	 * {@link Double#NEGATIVE_INFINITY} if it is always open or opens open endedly.
	 */
	public double getOpeningTime() {
		return isAlwaysOpen() ? Double.NEGATIVE_INFINITY : this.openingTimes.get(0).getStartTime();
	}

	/**
	 * @return when the place closes, i.e. the end of its last window.
	 * {@link Double#POSITIVE_INFINITY} if it is always open or closes open endedly.
	 */
	public double getClosingTime() {
		if (isAlwaysOpen()) {
			return Double.POSITIVE_INFINITY;
		}
		// ordered by start time, which for overlapping windows need not put the latest end last, so take
		// the maximum rather than the end of the last window
		return this.openingTimes.stream().mapToDouble(OpeningTime::getEndTime).max().orElseThrow();
	}

	/**
	 * @return true if the place is open at the given point in time, boundaries included
	 */
	public boolean isOpenAt(double time) {
		if (isAlwaysOpen()) {
			return true;
		}
		return this.openingTimes.stream()
			.anyMatch(window -> window.getStartTime() <= time && time <= window.getEndTime());
	}

	/**
	 * Asks whether an activity lasting from {@code from} to {@code to} fits into the opening hours.
	 * One single window has to cover the whole interval: an agent cannot shop across a midday closure,
	 * even though the place is open before and after it.
	 *
	 * @return true if the place is open for the whole interval
	 * @throws IllegalArgumentException if {@code to} is before {@code from}
	 */
	public boolean isOpenDuring(double from, double to) {
		if (to < from) {
			throw new IllegalArgumentException("The interval ends before it starts: [" + from + ", " + to + "]");
		}
		if (isAlwaysOpen()) {
			return true;
		}
		return this.openingTimes.stream()
			.anyMatch(window -> window.getStartTime() <= from && to <= window.getEndTime());
	}

	@Override
	public String toString() {
		return "[id=" + getId() + "][type=" + this.type + "][subType=" + this.subType + "]"
			+ (isAlwaysOpen()
			? "[alwaysOpen]"
			: "[open=" + Time.writeTime(getOpeningTime()) + "-" + Time.writeTime(getClosingTime()) + "]");
	}
}
