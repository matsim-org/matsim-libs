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

package org.matsim.contrib.cadyts.general;

import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;
import org.apache.commons.math3.geometry.euclidean.threed.NotARotationMatrixException;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author cdobler
 */
public class CadytsConfigGroup extends ReflectiveConfigGroup{

	public static final String GROUP_NAME = "cadytsCar";

	public static final String REGRESSION_INERTIA = "regressionInertia";
	public static final String MIN_FLOW_STDDEV = "minFlowStddevVehH";
	public static final String FREEZE_ITERATION = "freezeIteration";
	public static final String PREPARATORY_ITERATIONS = "preparatoryIterations";
	public static final String VARIANCE_SCALE = "varianceScale";
	public static final String USE_BRUTE_FORCE = "useBruteForce";
	public static final String START_TIME = "startTime";
	public static final String END_TIME = "endTime";
	public static final String WRITE_ANALYSIS_FILE = "writeAnalysisFile";
	public static final String TIME_BIN_SIZE = "timeBinSize" ;

	private static final String CALIBRATED_LINKS = "calibratedLinks";
	public static final String CALIBRATED_LINES = "calibratedLines";
//	private static final String CALIBRATED_ITEMS = "calibratedItems";

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

	private final Set<String> calibratedItems = new HashSet<>();
	private final Set<String> calibratedLines = new HashSet<>();
	private final Set<String> calibratedLinks = new HashSet<>();

	public CadytsConfigGroup() {
		super(GROUP_NAME);
	}

//	@Override
//	public final void addParam(final String paramName, final String value) {
//		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
//		if ( "null".equalsIgnoreCase( value ) ) return;
//
//		if (REGRESSION_INERTIA.equals(paramName)) {
//			setRegressionInertia(Double.parseDouble(value));
//		} else if (MIN_FLOW_STDDEV.equals(paramName)) {
//			setMinFlowStddev_vehPerHour(Double.parseDouble(value));
//		} else if (FREEZE_ITERATION.equals(paramName)) {
//			setFreezeIteration(Integer.parseInt(value));
//		} else if (PREPARATORY_ITERATIONS.equals(paramName)) {
//			setPreparatoryIterations(Integer.parseInt(value));
//		} else if (VARIANCE_SCALE.equals(paramName)) {
//			setVarianceScale(Double.parseDouble(value));
//		} else if (USE_BRUTE_FORCE.equals(paramName)) {
//			setUseBruteForce(Boolean.parseBoolean(value));
//		} else if (WRITE_ANALYSIS_FILE.equals(paramName)) {
//			setWriteAnalysisFile(Boolean.parseBoolean(value));
//		} else if (START_TIME.equals(paramName)) {
//			setStartTime((int)Time.parseTime(value));   //The "hh:mm:ss" format is converted here to seconds after midnight
//		} else if (END_TIME.equals(paramName)) {
//			setEndTime((int)Time.parseTime(value));		//The "hh:mm:ss" format is converted here to seconds after midnight
//		} else if ( TIME_BIN_SIZE.equals(paramName)) {
//			setTimeBinSize(Integer.parseInt(value)) ;
//		} else if (CALIBRATED_LINKS.equals(paramName) || CALIBRATED_LINES.equals(paramName) || CALIBRATED_ITEMS.equals(paramName) ) {
//			this.calibratedItems.clear();
//			for (String linkId : CollectionUtils.stringToArray(value)) {
//				this.calibratedItems.add( linkId );
//			}
//		} else {
//			throw new IllegalArgumentException("Parameter '" + paramName + "' is not supported by config group '" + GROUP_NAME + "'.");
//		}
//	}

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();

//		comments.put(CALIBRATED_ITEMS, "Comma-separated list of items with counts  to be calibrated.");
		comments.put(START_TIME, "The first second of the day to be used for calibration.  hh:mm:ss format");
		comments.put(END_TIME, "The last second of the day to be used for calibration.  hh:mm:ss format");
		comments.put(TIME_BIN_SIZE, "Length of time bin for which counts are aggregated.  IN SECONDS!!!!  Default is 3600.") ;

		return comments;
	}

//	@Override
//	public final String getValue(final String param_name) {
//		throw new UnsupportedOperationException("Use getters for accessing values!");
//	}
//
//	@Override
//	public final Map<String, String> getParams() {
//		Map<String, String> params = super.getParams();
//
//		params.put(REGRESSION_INERTIA, Double.toString(getRegressionInertia()));
//		params.put(MIN_FLOW_STDDEV, Double.toString(getMinFlowStddev_vehPerHour()));
//		params.put(FREEZE_ITERATION, Integer.toString(getFreezeIteration()));
//		params.put(PREPARATORY_ITERATIONS, Integer.toString(getPreparatoryIterations()));
//		params.put(VARIANCE_SCALE, Double.toString(getVarianceScale()));
//		params.put(USE_BRUTE_FORCE, Boolean.toString(useBruteForce()));
//		params.put(WRITE_ANALYSIS_FILE, Boolean.toString(isWriteAnalysisFile()));
//		params.put(START_TIME, Integer.toString(getStartTime()));
//		params.put(END_TIME, Integer.toString(getEndTime()));
//		params.put(CALIBRATED_ITEMS, CollectionUtils.setToString(this.calibratedItems));
//		params.put(TIME_BIN_SIZE, Integer.toString(getTimeBinSize())) ;
//
//		return params;
//	}

	@StringSetter( REGRESSION_INERTIA )
	public final void setRegressionInertia(final double regressionInertia) {
		this.regressionInertia = regressionInertia;
	}
	@StringGetter( REGRESSION_INERTIA )
	public final double getRegressionInertia() {
		return this.regressionInertia;
	}
	@StringSetter( MIN_FLOW_STDDEV )
	public final void setMinFlowStddev_vehPerHour(final double minFlowStddev) {
		this.minFlowStddev = minFlowStddev;
	}
	@StringGetter( MIN_FLOW_STDDEV )
	public final double getMinFlowStddev_vehPerHour() {
		return this.minFlowStddev;
	}
	@StringSetter( FREEZE_ITERATION )
	public final void setFreezeIteration(final int freezeIteration) {
		this.freezeIteration = freezeIteration;
	}
	@StringGetter( FREEZE_ITERATION )
	public final int getFreezeIteration() {
		return this.freezeIteration;
	}
	@StringSetter( PREPARATORY_ITERATIONS )
	public final void setPreparatoryIterations(final int preparatoryIterations) {
		this.preparatoryIterations = preparatoryIterations;
	}
	@StringGetter( PREPARATORY_ITERATIONS )
	public final int getPreparatoryIterations() {
		return this.preparatoryIterations;
	}
	@StringSetter( VARIANCE_SCALE )
	public final void setVarianceScale(final double varianceScale) {
		this.varianceScale = varianceScale;
	}
	@StringGetter( VARIANCE_SCALE )
	public final double getVarianceScale() {
		return this.varianceScale;
	}
	@StringSetter( USE_BRUTE_FORCE )
	public final void setUseBruteForce(final boolean useBruteForce) {
		this.bruteForce = useBruteForce;
	}
	@StringGetter( USE_BRUTE_FORCE )
	public final boolean useBruteForce() {
		return this.bruteForce;
	}
	@StringSetter( WRITE_ANALYSIS_FILE )
	public final void setWriteAnalysisFile(final boolean writeAnalysisFile) {
		this.writeAnalysisFile = writeAnalysisFile;
	}
	@StringGetter( WRITE_ANALYSIS_FILE )
	public final boolean isWriteAnalysisFile() {
		return this.writeAnalysisFile;
	}
	@StringSetter( START_TIME )
	public final void setStartTime(final int startTime) {
		this.startTime = startTime;
	}
	@StringGetter( START_TIME )
	public final int getStartTime() {
		return this.startTime;
	}
	@StringSetter( END_TIME )
	public final void setEndTime(final int endTime) {
		this.endTime = endTime;
	}
	@StringGetter( END_TIME )
	public final int getEndTime() {
		return this.endTime;
	}
	// ===
//	@StringGetter( CALIBRATED_ITEMS )
//	private String getCalibratedItemsAsString() { return CollectionUtils.setToString( this.calibratedItems ); }
//	@StringSetter( CALIBRATED_ITEMS )
//	private void setCalibratedItemsAsString( String string ) { setCalibratedItems( CollectionUtils.stringToSet( string ) ); }
//	// ---
//	public final Set<String> getCalibratedItems() {
//		return Collections.unmodifiableSet(this.calibratedItems);
//	}
	public final void setCalibratedItems(final Set<String> links) {
//		this.calibratedItems.clear();
//		this.calibratedItems.addAll(links);
		throw new RuntimeException( Gbl.NOT_IMPLEMENTED );
	}
	// ===
	@StringGetter( CALIBRATED_LINES )
	private String getCalibratedLinesAsString() { return CollectionUtils.setToString( this.calibratedLines ); }
	@StringSetter( CALIBRATED_LINES )
	private void setCalibratedLinesAsString( String string ) { setCalibratedLines( CollectionUtils.stringToSet( string ) ); }
	// ---
	public final Set<String> getCalibratedLines() {
		return Collections.unmodifiableSet(this.calibratedLines);
	}
	public final void setCalibratedLines(final Set<String> links) {
		this.calibratedLines.clear();
		this.calibratedLines.addAll(links);
	}
	// ===
	@StringGetter( CALIBRATED_LINKS )
	private String getCalibratedLinksAsString() { return CollectionUtils.setToString( this.calibratedLinks ); }
	@StringSetter( CALIBRATED_LINKS )
	private void setCalibratedLinksAsString( String string ) { setCalibratedLinks( CollectionUtils.stringToSet( string ) ); }
	// ---
	public final Set<String> getCalibratedLinks() {
		return Collections.unmodifiableSet(this.calibratedLinks);
	}
	public final void setCalibratedLinks(final Set<String> links) {
		this.calibratedLinks.clear();
		this.calibratedLinks.addAll(links);
	}
	// ===
	@StringGetter( TIME_BIN_SIZE )
	public final int getTimeBinSize() {
		return timeBinSize;
	}
	@StringSetter( TIME_BIN_SIZE )
	public final void setTimeBinSize(int timeBinSize) {
		this.timeBinSize = timeBinSize;
	}

//	@Override
//	public void addParameterSet( final ConfigGroup paramSet ) {
//		if ( paramSet.getName().equals(  ))
//	}
//	public static class CalibratedItems extends ReflectiveConfigGroup {
//		public final String SET_TYPE="calibratedItems";
//		public CalibratedItems( ){
//			super( SET_TYPE );
//		}
//	}

}
