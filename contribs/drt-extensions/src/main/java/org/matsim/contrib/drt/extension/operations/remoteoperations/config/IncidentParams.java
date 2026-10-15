/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.config;

import com.google.common.base.Verify;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.List;

/**
 * Configuration for stochastic remote-guidance incidents. When present (as an optional parameter set of
 * {@link RemoteGuidanceParams}), driving vehicles stochastically run into incidents that must be processed by a remote
 * operator; while an incident is processed the vehicle is held in place. Incident processing is a decoupled M/M/m queue
 * over all operators: an incident occupies exactly one free operator for a severity-dependent duration; if none is free
 * the incident queues and the vehicle keeps holding.
 * <p>
 * Incidents are generated per vehicle-kilometre travelled (occupied and empty km alike). Each
 * {@link IncidentSeverityParams} sub-set is an independent Poisson process with its own distance-based rate, so the
 * incident mix and the overall incident rate follow directly from the per-class rates (their superposition).
 *
 * @author nkuehnel / MOIA
 */
public class IncidentParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "incidents";

	@Parameter
	@Comment("Policy for choosing which free operator processes an incident. Defaults to RANDOM_FREE.")
	private IncidentAssignmentPolicy assignmentPolicy = IncidentAssignmentPolicy.RANDOM_FREE;

	public IncidentParams() {
		super(SET_NAME);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (IncidentSeverityParams.SET_NAME.equals(type)) {
			return new IncidentSeverityParams();
		}
		throw new IllegalArgumentException("Unsupported parameter set type: " + type);
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set instanceof IncidentSeverityParams) {
			super.addParameterSet(set);
		} else {
			throw new IllegalArgumentException("Unsupported parameter set: " + set.getName());
		}
	}

	public List<IncidentSeverityParams> getSeverityParams() {
		return getParameterSets(IncidentSeverityParams.SET_NAME).stream()
				.map(IncidentSeverityParams.class::cast)
				.toList();
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Verify.verify(!getSeverityParams().isEmpty(),
				"At least one incidentSeverity parameter set must be defined when incidents are enabled.");
	}

	public IncidentAssignmentPolicy getAssignmentPolicy() {
		return assignmentPolicy;
	}

	public void setAssignmentPolicy(IncidentAssignmentPolicy assignmentPolicy) {
		this.assignmentPolicy = assignmentPolicy;
	}
}