/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.config;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * One incident severity class, modelled as an independent Poisson process over driven distance: incidents of this class
 * occur at hazard rate {@link #lambdaPerMeter} per metre driven, and each lasts a duration sampled from the distribution
 * selected by {@link #durationDistribution} (log-normal by default) with parameters {@link #durationMu} and
 * {@link #durationSigma} (in log-space, i.e. of the underlying normal). The overall incident process is the
 * superposition of all severity classes, so the classes are fully independent — adding or removing one does not change
 * the others' rates.
 * <p>
 * Severity only affects the incident duration (how long an operator is occupied and the vehicle is held) — it does NOT
 * change how much operator capacity an incident consumes (an incident always occupies exactly one operator, see
 * {@link IncidentAssignmentPolicy}). The number of severity classes is data-driven: add as many {@code incidentSeverity}
 * parameter sets as needed.
 *
 * @author nkuehnel / MOIA
 */
public class IncidentSeverityParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "incidentSeverity";

	/**
	 * Family of the incident duration distribution. {@link #durationMu} and {@link #durationSigma} always parameterise
	 * the underlying log-normal; the other families are derived so that the <em>mean</em> duration (hence the service
	 * <em>rate</em>) is identical across all three, and only the squared coefficient of variation (SCV) differs. This
	 * makes the choice a controlled experiment on service-time variability alone: {@code LOGNORMAL} has
	 * SCV = exp(sigma^2)-1, {@code EXPONENTIAL} has SCV = 1 (memoryless, reduces the operator pool to an exact M/M/m
	 * queue), and {@code DETERMINISTIC} has SCV = 0 (M/D/m). The common mean is exp(mu + sigma^2/2).
	 */
	public enum DurationDistribution {LOGNORMAL, EXPONENTIAL, DETERMINISTIC}

	@Parameter
	@Comment("A human-readable name for this severity class (e.g. 'minor', 'moderate', 'severe'). Optional.")
	private String severityName;

	@Parameter
	@Comment("Incident hazard rate per metre driven [1/m] for this severity class, as an independent Poisson process "
			+ "over vehicle-kilometres travelled (both occupied and empty km). E.g. 1e-6 means on average one incident "
			+ "of this class per 1000 km of driving.")
	@Positive
	private double lambdaPerMeter;

	@Parameter
	@Comment("Mu (mean in log-space) of the log-normal incident duration distribution for this class [-]. "
			+ "The median duration in seconds is exp(mu).")
	private double durationMu;

	@Parameter
	@Comment("Sigma (standard deviation in log-space) of the log-normal incident duration distribution for this "
			+ "class [-]. Must be non-negative; 0 yields a deterministic duration of exp(mu).")
	@PositiveOrZero
	private double durationSigma;

	@Parameter
	@Comment("Family of the incident duration distribution: LOGNORMAL (default), EXPONENTIAL or DETERMINISTIC. All three "
			+ "share the SAME mean duration exp(mu + sigma^2/2) - hence the same service rate - and differ only in "
			+ "variability (SCV). EXPONENTIAL reduces the operator pool to an exact M/M/m queue (SCV=1, memoryless); "
			+ "DETERMINISTIC gives M/D/m (SCV=0); LOGNORMAL keeps the empirical long tail (SCV=exp(sigma^2)-1). Intended "
			+ "for varying the service-time variability while holding the mean fixed.")
	private DurationDistribution durationDistribution = DurationDistribution.LOGNORMAL;

	public IncidentSeverityParams() {
		super(SET_NAME);
	}

	public String getSeverityName() {
		return severityName;
	}

	public void setSeverityName(String severityName) {
		this.severityName = severityName;
	}

	public double getLambdaPerMeter() {
		return lambdaPerMeter;
	}

	public void setLambdaPerMeter(double lambdaPerMeter) {
		this.lambdaPerMeter = lambdaPerMeter;
	}

	public double getDurationMu() {
		return durationMu;
	}

	public void setDurationMu(double durationMu) {
		this.durationMu = durationMu;
	}

	public double getDurationSigma() {
		return durationSigma;
	}

	public void setDurationSigma(double durationSigma) {
		this.durationSigma = durationSigma;
	}

	public DurationDistribution getDurationDistribution() {
		return durationDistribution;
	}

	public void setDurationDistribution(DurationDistribution durationDistribution) {
		this.durationDistribution = durationDistribution;
	}
}