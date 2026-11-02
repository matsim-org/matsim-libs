package org.matsim.contrib.msp.capability.result;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.msp.capability.CapabilityConfigGroup;
import org.matsim.contrib.msp.capability.structure.ActivityChain;
import org.matsim.contrib.msp.capability.structure.FundamentalPlanExtractor;

import java.util.List;

/**
 * <b>One row per person that passed the person filter</b>: the reduction of their day to the
 * fundamental plan, as four readable chains - each day spelled once by class and once by name, so that
 * the rows can be grouped by pattern and still be read.
 * <ul>
 *     <li>{@code fundamental_activity_chain_class} - the fundamental plan by class, e.g.
 *     {@code home-mandatory-home}. By construction it only ever holds home and mandatory, which makes
 *     it the column to count day patterns over;</li>
 *     <li>{@code fundamental_activity_chain} - the same day by name, i.e. what is left after
 *     {@link FundamentalPlanExtractor} has dropped the discretionary activities, e.g.
 *     {@code home-work-home};</li>
 *     <li>{@code original_activity_chain_class} - the original day by class, e.g.
 *     {@code home-mandatory-discretionary-home}. This is the column that shows <i>why</i> the
 *     reduction came out the way it did;</li>
 *     <li>{@code original_activity_chain} - the original day as the scenario spells it, e.g.
 *     {@code home-work-leisure-home}.</li>
 * </ul>
 * Activity types are normalised as {@link ActivityChain} describes: no typical-duration suffix, and
 * {@code home_morning} and {@code home_28200} both written as plain {@code home}.
 * <p>
 * The two fundamental chains have their consecutive home stays merged
 * ({@link ActivityChain#mergeConsecutiveHome}), so a person with no non-home mandatory activity reads
 * {@code home} rather than {@code home-home}: the discretionary activity that sat between the two home
 * stays is gone, so they never left. The original chains are left as they are.
 * <p>
 * <b>This table is wider than the three measure levels on purpose.</b> It has a row for every person
 * the filter accepted, including those the analysis then had to skip because their fundamental plan
 * cannot be analysed - no usable times, a fixed activity past midnight, no free period left. Those
 * persons have no row in {@link PersonRecord}, so this is the table that explains the difference
 * between the two row counts.
 */
public record FundamentalPlanRecord(
	Id<Person> personId,
	List<Activity> fundamental,
	List<Activity> original,
	CapabilityConfigGroup config
) {

	public static final String[] HEADER = {
		"person_id", "fundamental_activity_chain_class", "fundamental_activity_chain",
		"original_activity_chain_class", "original_activity_chain"
	};

	/**
	 * @return the fields in the order of {@link #HEADER}
	 */
	public Object[] toRecord() {

		List<Activity> merged = ActivityChain.mergeConsecutiveHome(this.fundamental, this.config);

		return new Object[]{
			this.personId,
			ActivityChain.classified(merged, this.config),
			ActivityChain.of(merged, this.config),
			ActivityChain.classified(this.original, this.config),
			ActivityChain.of(this.original, this.config)
		};
	}
}
