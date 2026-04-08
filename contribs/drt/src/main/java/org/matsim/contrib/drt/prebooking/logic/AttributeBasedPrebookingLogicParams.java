package org.matsim.contrib.drt.prebooking.logic;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Configuration parameters for {@link AttributeBasedPrebookingLogic}. Configures
 * the attribute name prefixes used to read submission time and planned departure
 * time from activity attributes. The full attribute name is formed by appending
 * a colon and the DRT mode name to the prefix.
 *
 * @author Samuel Hoenle (samuelhoenle)
 */
public class AttributeBasedPrebookingLogicParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "logic:attributes";

	@Parameter
	@Comment("Attribute name prefix for submission time. The full attribute name is formed"
			+ " by appending a colon and the DRT mode name (e.g. 'prebooking:submissionTime:drt').")
	private String submissionTimeAttributePrefix = "prebooking:submissionTime";

	@Parameter
	@Comment("Attribute name prefix for planned departure time. The full attribute name is formed"
			+ " by appending a colon and the DRT mode name (e.g. 'prebooking:plannedDepartureTime:drt').")
	private String plannedDepartureTimeAttributePrefix = "prebooking:plannedDepartureTime";

	public AttributeBasedPrebookingLogicParams() {
		super(SET_NAME);
	}

	public String getSubmissionTimeAttributePrefix() {
		return submissionTimeAttributePrefix;
	}

	public void setSubmissionTimeAttributePrefix(String submissionTimeAttributePrefix) {
		this.submissionTimeAttributePrefix = submissionTimeAttributePrefix;
	}

	public String getPlannedDepartureTimeAttributePrefix() {
		return plannedDepartureTimeAttributePrefix;
	}

	public void setPlannedDepartureTimeAttributePrefix(String plannedDepartureTimeAttributePrefix) {
		this.plannedDepartureTimeAttributePrefix = plannedDepartureTimeAttributePrefix;
	}
}
