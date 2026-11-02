package org.matsim.contrib.msp.capability.result;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacility;
import org.matsim.contrib.msp.capability.measure.InsertionCalculator.Insertion;
import org.matsim.contrib.msp.capability.measure.InsertionCalculator.Totals;

/**
 * <b>Level 2, one row per facility</b>: what one place is worth to one person by one mode, aggregated
 * over the insertions of {@link InsertionRecord}.
 * <p>
 * The utility of the row is {@link #utility()}, the utility of the <b>best single insertion</b> - the
 * opportunity the person would actually take. It is deliberately not computed from the aggregates:
 * {@code sum_slack} and {@code c_insertion_min} belong to different insertions, so a utility built from
 * the pair would describe a trip nobody can make. The two are written anyway, as diagnostics, and
 * {@code best_insertion_id} says which level 1 row the utility came from.
 * <p>
 * A row exists only where the activity can actually be performed. The absence of a row means the person
 * cannot do this here - the place is too far away, closed, or there is no stretch of the day long
 * enough.
 */
public record FacilityRecord(
	Id<Person> personId,
	String mode,
	String actType,
	DiscretionaryActivityFacility facility,
	int mandatoryActivities,
	int fixedActivities,
	Totals totals
) {

	public static final String[] HEADER = {
		"person_id", "mode", "act_type", "facility_id", "facility_name", "facility_sub_type",
		"n_mandatory_act", "n_fixed_act", "n_insertions", "best_insertion_id",
		"best_slack", "best_t_extra", "sum_slack", "c_insertion_min", "u_potential_max"
	};

	/**
	 * @return {@code u_potential,max}: what the best realizable insertion at this place is worth
	 */
	public double utility() {
		return this.totals.maxUtility();
	}

	/**
	 * @return the sub-type of the place, empty string if the facilities do not say
	 */
	public String subType() {
		return this.facility.getSubType().orElse("");
	}

	/**
	 * @return the smallest cost of the possible insertions [utils]
	 */
	public double minCost() {
		return this.totals.minCost();
	}

	/**
	 * @return the fields in the order of {@link #HEADER}
	 */
	public Object[] toRecord() {
		Insertion best = this.totals.best();
		return new Object[]{
			this.personId, this.mode, this.actType,
			this.facility.getId(), this.facility.getName().orElse(""), subType(),
			this.mandatoryActivities, this.fixedActivities,
			this.totals.insertions(), this.totals.bestIndex(),
			Format.time(best.slack()), Format.time(best.extraTravelTime()),
			Format.time(this.totals.sumSlack()), Format.utils(this.totals.minCost()),
			Format.utils(utility())
		};
	}
}
