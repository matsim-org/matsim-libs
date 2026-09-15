package org.matsim.contrib.drt.prebooking.logic;

import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Configuration parameters for {@link ProbabilityBasedPrebookingLogic}. Defines
 * the probability that a DRT trip is prebooked and the time slack for
 * submission before departure.
 *
 * @author Samuel Hoenle (samuelhoenle)
 */
public class ProbabilityBasedPrebookingLogicParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "logic:probability";

	@Parameter
	@Comment("Probability that a DRT trip is prebooked. A value of 0.0 means no trips are prebooked,"
			+ " a value of 1.0 means all trips are prebooked.")
	@DecimalMin("0.0")
	@DecimalMax("1.0")
	private double probability = 1.0;

	@Parameter
	@Comment("Time in seconds before the planned departure time at which the prebooking request"
			+ " is submitted. For example, a value of 900 means the request is submitted 15 minutes"
			+ " before departure.")
	@PositiveOrZero
	private double submissionSlack = 900.0;

	public ProbabilityBasedPrebookingLogicParams() {
		super(SET_NAME);
	}

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	@PositiveOrZero
	public double getSubmissionSlack() {
		return submissionSlack;
	}

	public void setSubmissionSlack(@PositiveOrZero double submissionSlack) {
		this.submissionSlack = submissionSlack;
	}
}
