package org.matsim.contrib.msp.capability.result;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.msp.capability.DiscretionaryActivityParams;
import org.matsim.contrib.msp.capability.facilities.DiscretionaryActivityFacility;
import org.matsim.contrib.msp.capability.measure.InsertionCalculator.Insertion;
import org.matsim.contrib.msp.capability.structure.FreePeriod;

/**
 * <b>Level 1, one row per insertion</b>: one concrete opportunity the person has to perform the
 * discretionary activity - this place, in this stretch of the day.
 * <p>
 * The row carries the parameters its own numbers were computed with ({@code u_0}, {@code t_typ},
 * {@code k}), so that
 * <pre>
 *   u_potential = u_0 + k * ln(1 + slack / t_typ) - c_insertion
 * </pre>
 * can be recomputed, or recomputed differently, from the csv alone. Levels 2 and 3 can be derived from
 * this level by grouping, which is why the person's activity counts are repeated here as well.
 * <p>
 * The trailing columns are there to draw an agent's day: all three corners of the detour, so that the
 * triangle can be drawn from the row alone - a solid line from origin to destination for the trip the
 * agent was making anyway, dotted lines through the facility for the detour - plus the start time window
 * the activity could be placed in.
 *
 * @param insertionId the index of the free period this insertion sits in, which is also the index of the
 * matching row of {@link CapabilityWriter#ACTIVITIES_FILE}'s free stretches. Insertions of one person
 * and facility are numbered in the order of the day.
 */
public record InsertionRecord(
	Id<Person> personId,
	String mode,
	String actType,
	DiscretionaryActivityFacility facility,
	int insertionId,
	int mandatoryActivities,
	int fixedActivities,
	FreePeriod period,
	Insertion insertion,
	DiscretionaryActivityParams params
) {

	public static final String[] HEADER = {
		"person_id", "mode", "act_type", "facility_id", "facility_name", "facility_sub_type", "insertion_id",
		"slack", "t_extra", "c_insertion", "u_0", "t_typ", "k", "u_potential",
		"n_mandatory_act", "n_fixed_act",
		"period_from", "period_to", "origin_act", "dest_act",
		"origin_x", "origin_y", "dest_x", "dest_y", "facility_x", "facility_y",
		"earliest_start", "latest_start", "travel_in", "travel_out"
	};

	/**
	 * @return the fields in the order of {@link #HEADER}
	 */
	public Object[] toRecord() {
		Coord origin = this.period.origin().getCoord();
		Coord destination = this.period.destination().getCoord();
		return new Object[]{
			this.personId, this.mode, this.actType,
			this.facility.getId(), this.facility.getName().orElse(""), this.facility.getSubType().orElse(""),
			this.insertionId,
			Format.time(this.insertion.slack()),
			Format.time(this.insertion.extraTravelTime()),
			Format.utils(this.insertion.cost()),
			Format.utils(this.params.getBaseUtility()),
			Format.time(this.params.getReferenceDuration()),
			Format.utils(this.params.getFlexibilityUtility()),
			Format.utils(this.insertion.utility()),
			this.mandatoryActivities, this.fixedActivities,
			Format.time(this.period.availableFrom()), Format.time(this.period.deadline()),
			this.period.originType(), this.period.destinationType(),
			Format.x(origin), Format.y(origin),
			Format.x(destination), Format.y(destination),
			Format.x(this.facility.getCoord()), Format.y(this.facility.getCoord()),
			Format.time(this.insertion.earliestStart()), Format.time(this.insertion.latestStart()),
			Format.time(this.insertion.travelIn()), Format.time(this.insertion.travelOut())
		};
	}
}
