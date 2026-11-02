package org.matsim.contrib.msp.capability.result;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.contrib.msp.capability.measure.CapabilityLogsums;

import java.util.List;

/**
 * <b>Level 3, one row per person, mode and activity type</b>: the headline numbers, aggregated over the
 * facilities of {@link FacilityRecord}.
 * <p>
 * <b>Every analysed person has a row</b>, including those who cannot reach anything at all - their
 * {@code n_reachable_facilities} is 0 and their utility columns are empty. A person that the person
 * filter rejected, or whose plan cannot be analysed, has no row at all: the analysis makes no statement
 * about them.
 * <ul>
 *     <li>{@code u_potential_max} - the best single opportunity anywhere. Says how well off the person
 *     is if they only ever go to their one best place.</li>
 *     <li>{@code u_star} - the nested logsum over all reachable places, which also values having a
 *     choice. See {@link CapabilityLogsums#nestedUtilityLogsum}.</li>
 *     <li>{@code cost_logsum} - the accessibility-style measure over the costs alone, for comparison
 *     with the accessibility literature. It is blind to what can be done at a place and to
 *     flexibility.</li>
 * </ul>
 * {@code beta_1} and {@code beta_2} are written out because {@code u_star} cannot be recomputed from
 * the csv without them.
 */
public record PersonRecord(
	Id<Person> personId,
	String mode,
	String actType,
	int mandatoryActivities,
	int fixedActivities,
	List<FacilityRecord> facilities,
	CapabilityConfigGroup config
) {

	public static final String[] HEADER = {
		"person_id", "mode", "act_type", "n_mandatory_act", "n_fixed_act",
		"n_reachable_facilities", "n_sub_types", "n_insertions",
		"u_potential_max", "u_star", "cost_logsum", "beta_1", "beta_2"
	};

	/**
	 * @return the fields in the order of {@link #HEADER}
	 */
	public Object[] toRecord() {
		return new Object[]{
			this.personId, this.mode, this.actType,
			this.mandatoryActivities, this.fixedActivities,
			this.facilities.size(), subTypes(), insertions(),
			Format.utils(maxUtility()), Format.utils(utilityLogsum()), Format.utils(costLogsum()),
			Format.utils(this.config.getBetaWithinSubType()), Format.utils(this.config.getBetaAcrossSubType())
		};
	}

	/**
	 * @return how many kinds of the place the person can choose from, e.g. how many supermarket brands.
	 * Facilities whose sub-type is unknown do not contribute.
	 */
	public int subTypes() {
		return (int) this.facilities.stream()
			.map(FacilityRecord::subType)
			.filter(subType -> !subType.isBlank())
			.distinct()
			.count();
	}

	/**
	 * @return the number of possible insertions over all reachable facilities
	 */
	public int insertions() {
		return this.facilities.stream().mapToInt(facility -> facility.totals().insertions()).sum();
	}

	/**
	 * @return {@code u_potential,max}, {@link Double#NaN} if nothing is reachable
	 */
	public double maxUtility() {
		return this.facilities.stream()
			.mapToDouble(FacilityRecord::utility)
			.max()
			.orElse(Double.NaN);
	}

	/**
	 * @return {@code u*}, {@link Double#NaN} if nothing is reachable
	 */
	public double utilityLogsum() {
		return CapabilityLogsums.nestedUtilityLogsum(this.facilities.stream()
			.map(facility -> new CapabilityLogsums.Alternative(facility.subType(), facility.utility()))
			.toList(), this.config);
	}

	/**
	 * @return the cost logsum, {@link Double#NaN} if nothing is reachable
	 */
	public double costLogsum() {
		return CapabilityLogsums.costLogsum(
			this.facilities.stream().mapToDouble(FacilityRecord::minCost).toArray(), this.config);
	}
}
