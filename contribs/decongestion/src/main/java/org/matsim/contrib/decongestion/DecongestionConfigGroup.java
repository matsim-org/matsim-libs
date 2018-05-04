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
	
	public DecongestionConfigGroup() {
		super(GROUP_NAME);
	}
	
	// General parameters
	private boolean enableDecongestionPricing = true;
	private DecongestionApproach decongestionApproach = DecongestionApproach.PID;
	private boolean RUN_FINAL_ANALYSIS = true;
	private int UPDATE_PRICE_INTERVAL = 1;
	private int WRITE_OUTPUT_ITERATION = 1;
	private boolean WRITE_LINK_INFO_CHARTS = false; // set to false for big networks
	private double TOLERATED_AVERAGE_DELAY_SEC = 1.0; // set to 1.0 to account for rounding errors
	private double FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT = 0.1; // set above 0.0 to disable pricing in the previous iterations
	private double FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT = 1.0; // set below 1.0 to disable price adjustment for final iterations
	private double TOLL_BLEND_FACTOR = 1.0; // default: 1.0
	private boolean msa = false;
	
	// BangBang approach
	private double INITIAL_TOLL = 10.0;
	private double TOLL_ADJUSTMENT = 1.0;

	// PID approach
	private double Kp = 1.0;
	private double Kd = 1.0;
	private double Ki = 1.0;
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
	
	@StringGetter( "Kp" )
	public double getKp() {
		return Kp;
	}

	@StringSetter( "Kp" )
	public void setKp(double kp) {
		Kp = kp;
	}

	@StringGetter( "Ki" )
	public double getKi() {
		return Ki;
	}

	@StringSetter( "Ki" )
	public void setKi(double ki) {
		Ki = ki;
	}

	@StringGetter( "Kd" )
	public double getKd() {
		return Kd;
	}

	@StringSetter( "Kd" )
	public void setKd(double kd) {
		Kd = kd;
	}
	
	@StringGetter( "WRITE_OUTPUT_ITERATION" )
	public int getWRITE_OUTPUT_ITERATION() {
		return WRITE_OUTPUT_ITERATION;
	}

	@StringGetter( "UPDATE_PRICE_INTERVAL" )
	public int getUPDATE_PRICE_INTERVAL() {
		return UPDATE_PRICE_INTERVAL;
	}

	@StringGetter( "TOLERATED_AVERAGE_DELAY_SEC" )
	public double getTOLERATED_AVERAGE_DELAY_SEC() {
		return TOLERATED_AVERAGE_DELAY_SEC;
	}

	@StringGetter( "TOLL_ADJUSTMENT" )
	public double getTOLL_ADJUSTMENT() {
		return TOLL_ADJUSTMENT;
	}

	@StringGetter( "FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT" )
	public double getFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT() {
		return FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT;
	}

	@StringGetter( "TOLL_BLEND_FACTOR" )
	public double getTOLL_BLEND_FACTOR() {
		return TOLL_BLEND_FACTOR;
	}

	@StringSetter( "TOLL_ADJUSTMENT" )
	public void setTOLL_ADJUSTMENT(double tOLL_ADJUSTMENT) {
		TOLL_ADJUSTMENT = tOLL_ADJUSTMENT;
	}
	
	@StringSetter( "UPDATE_PRICE_INTERVAL" )
	public void setUPDATE_PRICE_INTERVAL(int uPDATE_PRICE_INTERVAL) {
		UPDATE_PRICE_INTERVAL = uPDATE_PRICE_INTERVAL;
	}

	@StringSetter( "TOLL_BLEND_FACTOR" )
	public void setTOLL_BLEND_FACTOR(double tOLL_BLEND_FACTOR) {
		TOLL_BLEND_FACTOR = tOLL_BLEND_FACTOR;
	}

	@StringSetter( "WRITE_OUTPUT_ITERATION" )
	public void setWRITE_OUTPUT_ITERATION(int wRITE_OUTPUT_ITERATION) {
		WRITE_OUTPUT_ITERATION = wRITE_OUTPUT_ITERATION;
	}

	@StringSetter( "TOLERATED_AVERAGE_DELAY_SEC" )
	public void setTOLERATED_AVERAGE_DELAY_SEC(double tOLERATED_AVERAGE_DELAY_SEC) {
		TOLERATED_AVERAGE_DELAY_SEC = tOLERATED_AVERAGE_DELAY_SEC;
	}

	@StringSetter( "FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT" )
	public void setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(double fRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT) {
		FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT = fRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT;
	}

	@StringGetter( "INITIAL_TOLL" )
	public double getINITIAL_TOLL() {
		return INITIAL_TOLL;
	}

	@StringSetter( "INITIAL_TOLL" )
	public void setINITIAL_TOLL(double iNITIAL_TOLL) {
		INITIAL_TOLL = iNITIAL_TOLL;
	}

	@StringGetter( "FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT" )
	public double getFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT() {
		return FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT;
	}

	@StringSetter( "FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT" )
	public void setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(
			double fRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT) {
		FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT = fRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT;
	}

	@StringGetter( "RUN_FINAL_ANALYSIS" )
	public boolean isRUN_FINAL_ANALYSIS() {
		return RUN_FINAL_ANALYSIS;
	}

	@StringSetter( "RUN_FINAL_ANALYSIS" )
	public void setRUN_FINAL_ANALYSIS(boolean rUN_FINAL_ANALYSIS) {
		RUN_FINAL_ANALYSIS = rUN_FINAL_ANALYSIS;
	}

	@StringGetter( "WRITE_LINK_INFO_CHARTS" )
	public boolean isWRITE_LINK_INFO_CHARTS() {
		return WRITE_LINK_INFO_CHARTS;
	}

	@StringSetter( "WRITE_LINK_INFO_CHARTS" )
	public void setWRITE_LINK_INFO_CHARTS(boolean wRITE_CHARTS) {
		WRITE_LINK_INFO_CHARTS = wRITE_CHARTS;
	}

	@StringGetter( "msa" )
	public boolean isMsa() {
		return msa;
	}

	@StringSetter( "msa" )
	public void setMsa(boolean msa) {
		this.msa = msa;
	}

	@StringGetter( "integralApproach" )
	public IntegralApproach getIntegralApproach() {
		return integralApproach;
	}

	@StringSetter( "integralApproach" )
	public void setIntegralApproach(IntegralApproach integralApproach) {
		this.integralApproach = integralApproach;
	}

	@StringGetter( "integralApproachAverageAlpha" )
	public double getIntegralApproachAverageAlpha() {
		return integralApproachAverageAlpha;
	}

	@StringSetter( "integralApproachAverageAlpha" )
	public void setIntegralApproachAverageAlpha(double integralApproachAverageAlpha) {
		this.integralApproachAverageAlpha = integralApproachAverageAlpha;
	}

	@StringGetter( "integralApproachUnusedHeadwayFactor" )
	public double getIntegralApproachUnusedHeadwayFactor() {
		return integralApproachUnusedHeadwayFactor;
	}

	@StringSetter( "integralApproachUnusedHeadwayFactor" )
	public void setIntegralApproachUnusedHeadwayFactor(double integralApproachUnusedHeadwayFactor) {
		this.integralApproachUnusedHeadwayFactor = integralApproachUnusedHeadwayFactor;
	}

	@StringGetter( "decongestionApproach" )
	public DecongestionApproach getDecongestionApproach() {
		return decongestionApproach;
	}

	@StringSetter( "decongestionApproach" )
	public void setDecongestionApproach(DecongestionApproach decongestionApproach) {
		this.decongestionApproach = decongestionApproach;
	}

	@StringGetter( "enableDecongestionPricing" )
	public boolean isEnableDecongestionPricing() {
		return enableDecongestionPricing;
	}

	@StringSetter( "enableDecongestionPricing" )
	public void setEnableDecongestionPricing(boolean enableDecongestionPricing) {
		this.enableDecongestionPricing = enableDecongestionPricing;
	}
			
}

