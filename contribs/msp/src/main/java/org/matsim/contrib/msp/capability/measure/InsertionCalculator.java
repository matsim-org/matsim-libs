package org.matsim.contrib.msp.capability.measure;

import org.matsim.contrib.msp.capability.DiscretionaryActivityParams;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacility;
import org.matsim.contrib.msp.capability.structure.FreePeriod;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out whether a discretionary activity can be inserted into a {@link FreePeriod}, how much freedom the
 * agent has in when to start it, and what the insertion is worth.
 * <p>
 * For a free period from {@code origin} (available from {@code t_from}) to {@code destination} (due at
 * {@code t_to}), and a facility {@code f}:
 * <pre>
 *   t_in     = travel time origin -&gt; f
 *   t_out    = travel time f -&gt; destination
 *   t_direct = travel time origin -&gt; destination
 *   t_extra  = t_in + t_out - t_direct                     the detour
 *
 *   A       = max(opening, t_from + t_in)                  earliest arrival at f
 *   B       = min(closing, t_to - t_out)                   latest departure from f
 *   D       = B - A                                        time available on site
 *   slack   = D - minDuration                              if D &gt;= minDuration
 *
 *   c_insertion = cost(t_extra)                            see {@link InsertionCost}
 *   u_potential = u_0 + k * ln(1 + slack / t_typ) - c_insertion
 * </pre>
 * {@code slack} is the width of the window in which the activity may be <i>started</i>: it is what the
 * agent can shift the activity around by. {@code u_potential} is what makes an insertion comparable to
 * any other one: it holds the benefit and the cost <b>of one and the same insertion</b>, so the best of
 * them is an opportunity the agent could actually take, rather than a mix of the flexibility of one
 * position and the detour of another. Note that it can be negative, when the detour costs more than
 * being able to perform the activity is worth.
 * <p>
 * A facility that is open in several windows a day contributes each of them separately, so a restaurant
 * with a lunch and a dinner service offers more freedom than one open only in the evening.
 */
public final class InsertionCalculator {

	/**
	 * Stands in for a facility that is open around the clock.
	 */
	private static final OpeningTime ALWAYS_OPEN =
		new OpeningTimeImpl(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

	private final CapabilityTravelTimes travelTimes;
	private final DiscretionaryActivityParams params;
	private final InsertionCost cost;

	public InsertionCalculator(CapabilityTravelTimes travelTimes, DiscretionaryActivityParams params, InsertionCost cost) {
		this.travelTimes = travelTimes;
		this.params = params;
		this.cost = cost;
	}

	/**
	 * @return the result for one free period, or {@link Insertion#IMPOSSIBLE} if the activity does not
	 * fit into it
	 */
	public Insertion evaluate(FreePeriod period, DiscretionaryActivityFacility facility) {

		double in = this.travelTimes.travelTime(period.origin(), facility, period.availableFrom());
		double out = this.travelTimes.travelTime(facility, period.destination(), period.availableFrom() + in);

		if (Double.isInfinite(in) || Double.isInfinite(out)) {
			return Insertion.IMPOSSIBLE;
		}

		double direct = this.travelTimes.travelTime(period.origin(), period.destination(), period.availableFrom());
		// a disconnected direct trip would make the detour meaningless, so treat it as zero in that case
		double extra = Math.max(0, in + out - (Double.isInfinite(direct) ? 0 : direct));

		// only a search space cut: a facility that costs more of a detour than this is not considered
		if (extra > this.params.getMaxExtraTravelTime()) {
			return Insertion.IMPOSSIBLE;
		}

		double arrival = period.availableFrom() + in;
		double latestDeparture = period.deadline() - out;

		// the activity is performed in whichever opening window gives the agent the most room; the
		// windows of one facility are alternatives rather than something to add up, because the agent
		// performs the activity once
		double slack = -1;
		double earliestStart = Double.NaN;
		double latestStart = Double.NaN;

		for (OpeningTime window : windowsOf(facility)) {
			double from = Math.max(window.getStartTime(), arrival);
			double to = Math.min(window.getEndTime(), latestDeparture);
			double available = to - from - this.params.getMinDuration();

			if (available >= 0 && available > slack) {
				slack = available;
				earliestStart = from;
				latestStart = to - this.params.getMinDuration();
			}
		}

		if (slack < 0) {
			return Insertion.IMPOSSIBLE;
		}

		double c = this.cost.cost(extra);
		return new Insertion(slack, extra, c, this.params.utility(slack) - c,
			earliestStart, latestStart, in, out);
	}

	/**
	 * Evaluates every free period of one person for one facility. Each period is evaluated exactly
	 * once, and the results are kept, so that a caller needing them per period - the insertion level of
	 * the output does - does not have to ask for them a second time.
	 *
	 * @return one entry per given period, in their order. An entry of a period the activity does not
	 * fit into is {@link Insertion#IMPOSSIBLE} rather than missing, so the index of an entry is the
	 * index of its period.
	 */
	public Totals evaluate(List<FreePeriod> periods, DiscretionaryActivityFacility facility) {
		List<Insertion> byPeriod = new ArrayList<>(periods.size());
		for (FreePeriod period : periods) {
			byPeriod.add(evaluate(period, facility));
		}
		return new Totals(List.copyOf(byPeriod));
	}

	private static List<OpeningTime> windowsOf(DiscretionaryActivityFacility facility) {
		return facility.isAlwaysOpen() ? List.of(ALWAYS_OPEN) : facility.getOpeningTimes();
	}

	/**
	 * The outcome for a single free period.
	 *
	 * @param slack the width of the start time window [s], 0 if the activity only just fits
	 * @param extraTravelTime {@code t_extra}, the detour [s]
	 * @param cost {@code c_insertion}, what the detour costs in utility
	 * @param utility {@code u_potential}, the benefit of this insertion net of its cost. Can be
	 * negative.
	 * @param earliestStart the earliest the activity could be started [s]
	 * @param latestStart the latest it could be started [s]. Equal to {@code earliestStart} when the
	 * activity only just fits.
	 * @param travelIn the travel time from the origin of the period to the facility [s]
	 * @param travelOut the travel time from the facility to the destination of the period [s]
	 */
	public record Insertion(double slack, double extraTravelTime, double cost, double utility,
							double earliestStart, double latestStart, double travelIn, double travelOut) {

		public static final Insertion IMPOSSIBLE = new Insertion(-1, Double.NaN, Double.NaN, Double.NaN,
			Double.NaN, Double.NaN, Double.NaN, Double.NaN);

		public boolean possible() {
			return this.slack >= 0;
		}
	}

	/**
	 * The outcomes for one person and one facility, one entry per free period of the person's day.
	 * <p>
	 * The facility level of the output is this, aggregated: {@link #best()} is the opportunity the
	 * agent would actually take, while {@link #sumSlack()} and {@link #minCost()} are reported
	 * alongside it as diagnostics. They are deliberately <b>not</b> combined into a utility: no single
	 * insertion offers the flexibility of all positions at the cost of the cheapest one.
	 *
	 * @param byPeriod one entry per free period, in the order of the periods;
	 * {@link Insertion#IMPOSSIBLE} where the activity does not fit
	 */
	public record Totals(List<Insertion> byPeriod) {

		/**
		 * @return true if the activity fits into at least one free period
		 */
		public boolean possible() {
			return this.byPeriod.stream().anyMatch(Insertion::possible);
		}

		/**
		 * @return the number of free periods the activity fits into. Only comparable between persons
		 * with the same number of mandatory activities.
		 */
		public int insertions() {
			return (int) this.byPeriod.stream().filter(Insertion::possible).count();
		}

		/**
		 * @return the index of the insertion with the highest {@link Insertion#utility()}, i.e. the
		 * opportunity the agent would take; -1 if there is none
		 */
		public int bestIndex() {
			int best = -1;
			for (int i = 0; i < this.byPeriod.size(); i++) {
				Insertion insertion = this.byPeriod.get(i);
				if (insertion.possible() && (best < 0 || insertion.utility() > this.byPeriod.get(best).utility())) {
					best = i;
				}
			}
			return best;
		}

		/**
		 * @return the best insertion
		 * @throws IllegalStateException if the activity does not fit anywhere
		 */
		public Insertion best() {
			int best = bestIndex();
			if (best < 0) {
				throw new IllegalStateException("The activity cannot be inserted anywhere, so there is no best "
					+ "insertion. Ask possible() first.");
			}
			return this.byPeriod.get(best);
		}

		/**
		 * @return {@code u_potential,max}, the utility of the best insertion, {@link Double#NaN} if the
		 * activity does not fit anywhere
		 */
		public double maxUtility() {
			int best = bestIndex();
			return best < 0 ? Double.NaN : this.byPeriod.get(best).utility();
		}

		/**
		 * @return the summed slack [s] over the possible insertions. A diagnostic: it says how much
		 * freedom the day holds in total, which is more than any single insertion can offer.
		 */
		public double sumSlack() {
			return this.byPeriod.stream().filter(Insertion::possible).mapToDouble(Insertion::slack).sum();
		}

		/**
		 * @return the smallest {@code c_insertion} of the possible insertions, {@link Double#NaN} if
		 * there is none. A diagnostic, for the same reason as {@link #sumSlack()}.
		 */
		public double minCost() {
			return this.byPeriod.stream().filter(Insertion::possible)
				.mapToDouble(Insertion::cost).min().orElse(Double.NaN);
		}
	}
}
