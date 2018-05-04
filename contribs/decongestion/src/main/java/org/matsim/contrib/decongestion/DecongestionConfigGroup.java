/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.decongestion;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Contains the input parameters, e.g. how often the interval-based output is written out, the number of iterations for which the price is kept constant, ...
 * 
 * @author ikaddoura
 */

public class DecongestionConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "decongestion" ;

	private static final String Kp="Kp" ;
	private static final String Ki="Ki" ;
	private static final String Kd = "Kd";
	private static final String WRITE_OUTPUT_ITERATION = "writeOutputIteration";
	private static final String UPDATE_PRICE_INTERVAL = "updatePriceInterval";
	private static final String TOLERATED_AVERAGE_DELAY_SEC = "toleratedAverageDelaySec";
	private static final String TOLL_ADJUSTMENT = "tollAdjustment";
	private static final String FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT = "fractionOfIterationsToEndPriceAdjustment";
	private static final String TOLL_BLEND_FACTOR = "tollBlendFactor";
	private static final String INITIAL_TOLL = "initialToll";
	private static final String FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT = "fractionOfIterationsToStartPriceAdjustment";
	private static final String RUN_FINAL_ANALYSIS = "runFinalAnalysis";
	private static final String WRITE_LINK_INFO_CHARTS = "writeLinkInfoCharts";
	private static final String MSA = "usingMsa";
	private static final String INTEGRAL_APPROACH = "integralApproach";
	private static final String INTEGRAL_APPROACH_AVERAGE_ALPHA = "integralApproachAverageAlpha";
	private static final String INTEGRAL_APPROACH_UNUSED_HEADWAY_FACTOR = "integralApproachUnusedHeadwayFactor";
	private static final String DECONGESTION_APPROACH = "decongestionApproach";
	private static final String ENABLE_DECONGESTION_PRICING = "enableDecongestionPricing";
	
	public DecongestionConfigGroup() {
		super(GROUP_NAME);
	}
	
	// General parameters
	private boolean enableDecongestionPricing = true;
	private DecongestionApproach decongestionApproach = DecongestionApproach.PID;
	private boolean runFinalAnalysis = true;
	private int updatePriceInterval = 1;
	private int writeOutputIteration = 1;
	private boolean writeLinkInfoCharts = false; // set to false for big networks
	private double toleratedAverageDelaySec = 1.0; // set to 1.0 to account for rounding errors
	private double fractionOfIterationsToStartPriceAdjustment = 0.1; // set above 0.0 to disable pricing in the previous iterations
	private double fractionOfIterationsToEndPriceAdjustment = 1.0; // set below 1.0 to disable price adjustment for final iterations
	private double tollBlendFactor = 1.0; // default: 1.0
	private boolean msa = false;
	
	// BangBang approach
	private double initialToll = 10.0;
	private double tollAdjustment = 1.0;

	// PID approach
	private double kp = 1.0;
	private double kd = 1.0;
	private double ki = 1.0;
	private IntegralApproach integralApproach = IntegralApproach.Zero;
	private double integralApproachAverageAlpha = 0.1;
	private double integralApproachUnusedHeadwayFactor = 10.;
	
	// ######################################################################################
	
	public enum IntegralApproach {
		Average, UnusedHeadway, Zero
	}
	
	public enum DecongestionApproach {
		BangBang, PID, P_MC
	}
	
	@StringGetter( Kp )
	public double getKp() {
		return kp;
	}

	@StringSetter( Kp )
	public void setKp(double kp) {
		this.kp = kp;
	}

	@StringGetter( Ki )
	public double getKi() {
		return ki;
	}

	@StringSetter( Ki )
	public void setKi(double ki) {
		this.ki = ki;
	}

	@StringGetter( Kd )
	public double getKd() {
		return kd;
	}

	@StringSetter( Kd )
	public void setKd(double kd) {
		this.kd = kd;
	}
	
	@StringGetter( WRITE_OUTPUT_ITERATION )
	public int getWriteOutputIteration() {
		return writeOutputIteration;
	}

	@StringGetter( UPDATE_PRICE_INTERVAL )
	public int getUpdatePriceInterval() {
		return updatePriceInterval;
	}

	@StringGetter( TOLERATED_AVERAGE_DELAY_SEC )
	public double getToleratedAverageDelaySec() {
		return toleratedAverageDelaySec;
	}

	@StringGetter( TOLL_ADJUSTMENT )
	public double getTollAdjustment() {
		return tollAdjustment;
	}

	@StringGetter( FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT )
	public double getFractionOfIterationsToEndPriceAdjustment() {
		return fractionOfIterationsToEndPriceAdjustment;
	}

	@StringGetter( TOLL_BLEND_FACTOR )
	public double getTollBlendFactor() {
		return tollBlendFactor;
	}

	@StringSetter( TOLL_ADJUSTMENT )
	public void setTollAdjustment(double tOLL_ADJUSTMENT) {
		tollAdjustment = tOLL_ADJUSTMENT;
	}
	
	@StringSetter( UPDATE_PRICE_INTERVAL )
	public void setUpdatePriceInterval(int uPDATE_PRICE_INTERVAL) {
		updatePriceInterval = uPDATE_PRICE_INTERVAL;
	}

	@StringSetter( TOLL_BLEND_FACTOR )
	public void setTollBlendFactor(double tOLL_BLEND_FACTOR) {
		tollBlendFactor = tOLL_BLEND_FACTOR;
	}

	@StringSetter( WRITE_OUTPUT_ITERATION )
	public void setWriteOutputIteration(int wRITE_OUTPUT_ITERATION) {
		writeOutputIteration = wRITE_OUTPUT_ITERATION;
	}

	@StringSetter( TOLERATED_AVERAGE_DELAY_SEC )
	public void setToleratedAverageDelaySec(double tOLERATED_AVERAGE_DELAY_SEC) {
		toleratedAverageDelaySec = tOLERATED_AVERAGE_DELAY_SEC;
	}

	@StringSetter( FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT )
	public void setFractionOfIterationsToEndPriceAdjustment(double fRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT) {
		fractionOfIterationsToEndPriceAdjustment = fRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT;
	}

	@StringGetter( INITIAL_TOLL )
	public double getInitialToll() {
		return initialToll;
	}

	@StringSetter( INITIAL_TOLL )
	public void setInitialToll(double iNITIAL_TOLL) {
		initialToll = iNITIAL_TOLL;
	}

	@StringGetter( FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT )
	public double getFractionOfIterationsToStartPriceAdjustment() {
		return fractionOfIterationsToStartPriceAdjustment;
	}

	@StringSetter( FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT )
	public void setFractionOfIterationsToStartPriceAdjustment(
			double fRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT) {
		fractionOfIterationsToStartPriceAdjustment = fRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT;
	}

	@StringGetter( RUN_FINAL_ANALYSIS )
	public boolean isRunFinalAnalysis() {
		return runFinalAnalysis;
	}

	@StringSetter( RUN_FINAL_ANALYSIS )
	public void setRunFinalAnalysis(boolean rUN_FINAL_ANALYSIS) {
		runFinalAnalysis = rUN_FINAL_ANALYSIS;
	}

	@StringGetter( WRITE_LINK_INFO_CHARTS )
	public boolean isWriteLinkInfoCharts() {
		return writeLinkInfoCharts;
	}

	@StringSetter( WRITE_LINK_INFO_CHARTS )
	public void setWriteLinkInfoCharts(boolean wRITE_CHARTS) {
		writeLinkInfoCharts = wRITE_CHARTS;
	}

	@StringGetter( MSA )
	public boolean isMsa() {
		return msa;
	}

	@StringSetter( MSA )
	public void setMsa(boolean msa) {
		this.msa = msa;
	}

	@StringGetter( INTEGRAL_APPROACH )
	public IntegralApproach getIntegralApproach() {
		return integralApproach;
	}

	@StringSetter( INTEGRAL_APPROACH )
	public void setIntegralApproach(IntegralApproach integralApproach) {
		this.integralApproach = integralApproach;
	}

	@StringGetter( INTEGRAL_APPROACH_AVERAGE_ALPHA )
	public double getIntegralApproachAverageAlpha() {
		return integralApproachAverageAlpha;
	}

	@StringSetter( INTEGRAL_APPROACH_AVERAGE_ALPHA )
	public void setIntegralApproachAverageAlpha(double integralApproachAverageAlpha) {
		this.integralApproachAverageAlpha = integralApproachAverageAlpha;
	}

	@StringGetter( INTEGRAL_APPROACH_UNUSED_HEADWAY_FACTOR )
	public double getIntegralApproachUnusedHeadwayFactor() {
		return integralApproachUnusedHeadwayFactor;
	}

	@StringSetter( INTEGRAL_APPROACH_UNUSED_HEADWAY_FACTOR )
	public void setIntegralApproachUnusedHeadwayFactor(double integralApproachUnusedHeadwayFactor) {
		this.integralApproachUnusedHeadwayFactor = integralApproachUnusedHeadwayFactor;
	}

	@StringGetter( DECONGESTION_APPROACH )
	public DecongestionApproach getDecongestionApproach() {
		return decongestionApproach;
	}

	@StringSetter( DECONGESTION_APPROACH )
	public void setDecongestionApproach(DecongestionApproach decongestionApproach) {
		this.decongestionApproach = decongestionApproach;
	}

	@StringGetter( ENABLE_DECONGESTION_PRICING )
	public boolean isEnableDecongestionPricing() {
		return enableDecongestionPricing;
	}

	@StringSetter( ENABLE_DECONGESTION_PRICING )
	public void setEnableDecongestionPricing(boolean enableDecongestionPricing) {
		this.enableDecongestionPricing = enableDecongestionPricing;
	}
			
}

