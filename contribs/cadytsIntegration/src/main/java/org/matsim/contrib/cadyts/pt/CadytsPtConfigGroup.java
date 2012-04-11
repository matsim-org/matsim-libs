/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.cadyts.pt;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Module;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author mrieser / senozon
 */
public class CadytsPtConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "cadytsPt"; // TODO change to cadytsPt

	public static final String REGRESSION_INERTIA = "regressionInertia";
	public static final String MIN_FLOW_STDDEV = "minFlowStddevVehH";
	public static final String FREEZE_ITERATION = "freezeIteration";
	public static final String PREPARATORY_ITERATIONS = "preparatoryIterations";
	public static final String VARIANCE_SCALE = "varianceScale";
	public static final String USE_BRUTE_FORCE = "useBruteForce";
	public static final String START_TIME = "startTime";
	public static final String END_TIME = "endTime";
	public static final String WRITE_ANALYSIS_FILE = "writeAnalysisFile";
	private static final String CALIBRATED_LINES = "calibratedLines";

	private final Set<Id> calibratedLines = new HashSet<Id>();

	public CadytsPtConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public void addParam(final String paramName, final String value) {
		if (CALIBRATED_LINES.equals(paramName)) {
			this.calibratedLines.clear();
			for (String lineId : CollectionUtils.stringToArray(value)) {
				this.calibratedLines.add(new IdImpl(lineId));
			}
		} else {
			super.addParam(paramName, value);
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();

		comments.put(CALIBRATED_LINES, "Comma-separated list of transit lines to be calibrated.");

		return comments;
	}

	@Override
	public String getValue(final String param_name) {
		// TODO Auto-generated method stub
		return super.getValue(param_name);
	}

	@Override
	public Map<String, String> getParams() {
		// TODO Auto-generated method stub
		return super.getParams();
	}

	public Set<Id> getCalibratedLines() {
		return Collections.unmodifiableSet(this.calibratedLines);
	}

	public void setCalibratedLines(final Set<Id> lines) {
		this.calibratedLines.clear();
		this.calibratedLines.addAll(lines);
	}

}
