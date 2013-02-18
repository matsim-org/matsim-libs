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
import org.matsim.core.utils.misc.Time;

import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;

/**
 * @author mrieser / senozon
 */
public class CadytsPtConfigGroup extends Module {

	public static final String GROUP_NAME = "cadytsPt";

	public static final String REGRESSION_INERTIA = "regressionInertia";
	public static final String MIN_FLOW_STDDEV = "minFlowStddevVehH";
	public static final String FREEZE_ITERATION = "freezeIteration";
	public static final String PREPARATORY_ITERATIONS = "preparatoryIterations";
	public static final String VARIANCE_SCALE = "varianceScale";
	public static final String USE_BRUTE_FORCE = "useBruteForce";
	public static final String START_TIME = "startTime";
	public static final String END_TIME = "endTime";
	public static final String WRITE_ANALYSIS_FILE = "writeAnalysisFile";
	public static final String CALIBRATED_LINES = "calibratedLines";
	public static final String TIME_BIN_SIZE = "timeBinSize" ;

	private double regressionInertia = MATSimUtilityModificationCalibrator.DEFAULT_REGRESSION_INERTIA;
	private double minFlowStddev = MATSimUtilityModificationCalibrator.DEFAULT_MIN_FLOW_STDDEV_VEH_H;
	private int freezeIteration = MATSimUtilityModificationCalibrator.DEFAULT_FREEZE_ITERATION;
	private int preparatoryIterations = MATSimUtilityModificationCalibrator.DEFAULT_PREPARATORY_ITERATIONS;
	private double varianceScale = MATSimUtilityModificationCalibrator.DEFAULT_VARIANCE_SCALE;
	private boolean bruteForce = MATSimUtilityModificationCalibrator.DEFAULT_BRUTE_FORCE;
	private boolean writeAnalysisFile = false;
	private int startTime = 0;
	private int endTime = (int)Time.MIDNIGHT-1;
	private int timeBinSize = 3600 ;

	private final Set<Id> calibratedLines = new HashSet<Id>();

	public CadytsPtConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public void addParam(final String paramName, final String value) {
		if (REGRESSION_INERTIA.equals(paramName)) {
			setRegressionInertia(Double.parseDouble(value));
		} else if (MIN_FLOW_STDDEV.equals(paramName)) {
			setMinFlowStddev_vehPerHour(Double.parseDouble(value));
		} else if (FREEZE_ITERATION.equals(paramName)) {
			setFreezeIteration(Integer.parseInt(value));
		} else if (PREPARATORY_ITERATIONS.equals(paramName)) {
			setPreparatoryIterations(Integer.parseInt(value));
		} else if (VARIANCE_SCALE.equals(paramName)) {
			setVarianceScale(Double.parseDouble(value));
		} else if (USE_BRUTE_FORCE.equals(paramName)) {
			setUseBruteForce(Boolean.parseBoolean(value));
		} else if (WRITE_ANALYSIS_FILE.equals(paramName)) {
			setWriteAnalysisFile(Boolean.parseBoolean(value));
		} else if (START_TIME.equals(paramName)) {
			//setStartTime(Integer.parseInt(value));	//original
			setStartTime((int)Time.parseTime(value));   //The "hh:mm:ss" format is converted here to seconds after midnight
		} else if (END_TIME.equals(paramName)) {
			//setEndTime(Integer.parseInt(value));		//original
			setEndTime((int)Time.parseTime(value));		//The "hh:mm:ss" format is converted here to seconds after midnight
		} else if ( TIME_BIN_SIZE.equals(paramName)) {
			setTimeBinSize(Integer.parseInt(value)) ;
		} else if (CALIBRATED_LINES.equals(paramName)) {
			this.calibratedLines.clear();
			for (String lineId : CollectionUtils.stringToArray(value)) {
				this.calibratedLines.add(new IdImpl(lineId));
			}
		} else {
			throw new IllegalArgumentException("Parameter '" + paramName + "' is not supported by config group '" + GROUP_NAME + "'.");
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();

		comments.put(CALIBRATED_LINES, "Comma-separated list of transit lines to be calibrated.");
		comments.put(START_TIME, "The first second of the day to be used for calibration.  hh:mm:ss format");
		comments.put(END_TIME, "The last second of the day to be used for calibration.  hh:mm:ss format");
		comments.put(TIME_BIN_SIZE, "Length of time bin for which counts are aggregated.  IN SECONDS!!!!  Default is 3600.") ;

		return comments;
	}

	@Override
	public String getValue(final String param_name) {
		throw new UnsupportedOperationException("Use getters for accessing values!");
	}

	@Override
	public Map<String, String> getParams() {
		Map<String, String> params = super.getParams();

		params.put(REGRESSION_INERTIA, Double.toString(getRegressionInertia()));
		params.put(MIN_FLOW_STDDEV, Double.toString(getMinFlowStddev_vehPerHour()));
		params.put(FREEZE_ITERATION, Integer.toString(getFreezeIteration()));
		params.put(PREPARATORY_ITERATIONS, Integer.toString(getPreparatoryIterations()));
		params.put(VARIANCE_SCALE, Double.toString(getVarianceScale()));
		params.put(USE_BRUTE_FORCE, Boolean.toString(useBruteForce()));
		params.put(WRITE_ANALYSIS_FILE, Boolean.toString(isWriteAnalysisFile()));
		params.put(START_TIME, Time.writeTime(getStartTime()));
		params.put(END_TIME, Time.writeTime(getEndTime()));
		params.put(CALIBRATED_LINES, CollectionUtils.idSetToString(this.calibratedLines));
		params.put(TIME_BIN_SIZE, Integer.toString(getTimeBinSize())) ;

		return params;
	}

	public void setRegressionInertia(final double regressionInertia) {
		this.regressionInertia = regressionInertia;
	}

	public double getRegressionInertia() {
		return this.regressionInertia;
	}

	public void setMinFlowStddev_vehPerHour(final double minFlowStddev) {
		this.minFlowStddev = minFlowStddev;
	}

	public double getMinFlowStddev_vehPerHour() {
		return this.minFlowStddev;
	}

	public void setFreezeIteration(final int freezeIteration) {
		this.freezeIteration = freezeIteration;
	}

	public int getFreezeIteration() {
		return this.freezeIteration;
	}

	public void setPreparatoryIterations(final int preparatoryIterations) {
		this.preparatoryIterations = preparatoryIterations;
	}

	public int getPreparatoryIterations() {
		return this.preparatoryIterations;
	}

	public void setVarianceScale(final double varianceScale) {
		this.varianceScale = varianceScale;
	}

	public double getVarianceScale() {
		return this.varianceScale;
	}

	public void setUseBruteForce(final boolean useBruteForce) {
		this.bruteForce = useBruteForce;
	}

	public boolean useBruteForce() {
		return this.bruteForce;
	}

	public void setWriteAnalysisFile(final boolean writeAnalysisFile) {
		this.writeAnalysisFile = writeAnalysisFile;
	}

	public boolean isWriteAnalysisFile() {
		return this.writeAnalysisFile;
	}

	public void setStartTime(final int startTime) {
		this.startTime = startTime;
	}

	public int getStartTime() {
		return this.startTime;
	}

	public void setEndTime(final int endTime) {
		this.endTime = endTime;
	}

	public int getEndTime() {
		return this.endTime;
	}

	public Set<Id> getCalibratedLines() {
		return Collections.unmodifiableSet(this.calibratedLines);
	}

	public void setCalibratedLines(final Set<Id> lines) {
		this.calibratedLines.clear();
		this.calibratedLines.addAll(lines);
	}
	
	public int getTimeBinSize() {
		return timeBinSize;
	}

	public void setTimeBinSize(int timeBinSize) {
		this.timeBinSize = timeBinSize;
	}

}