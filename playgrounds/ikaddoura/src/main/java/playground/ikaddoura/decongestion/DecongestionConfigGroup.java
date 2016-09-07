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

package playground.ikaddoura.decongestion;

/**
 * Contains the input parameters, e.g. how often the interval-based output is written out, the number of iterations for which the price is kept constant, ...
 * 
 * @author ikaddoura
 */

public class DecongestionConfigGroup {
	
	// No pricing
//	private TollingApproach TOLLING_APPROACH = TollingApproach.NoPricing;

	// BangBang approach
//	private TollingApproach TOLLING_APPROACH = TollingApproach.BangBang;
	private double INITIAL_TOLL = 10.0;
	private double TOLL_ADJUSTMENT = 1.0; // default: 0.1

	// PID approach
	private TollingApproach TOLLING_APPROACH = TollingApproach.PID;
	private double TOLL_BLEND_FACTOR = 1.0; // default: 1.0
	private double Kp = 1.0;
	private double Ki = 1.0;
	private double Kd = 1.0;
	
	// General parameters
	private boolean RUN_FINAL_ANALYSIS = true;
	private int UPDATE_PRICE_INTERVAL = 1;
	private int WRITE_OUTPUT_ITERATION = 1;
	private boolean WRITE_LINK_INFO_CHARTS = true; // set to false for big networks
	private double TOLERATED_AVERAGE_DELAY_SEC = 1.0; // set to 1.0 to account for rounding errors
	private double FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT = 0.1; // set above 0.0 to disable pricing in the previous iterations
	private double FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT = 1.0; // set below 1.0 to disable price adjustment for final iterations
	
	public enum TollingApproach {
        NoPricing, V8, BangBang, PID
	}
	
	// ######################################################################################
	
	public double getKp() {
		return Kp;
	}

	public void setKp(double kp) {
		Kp = kp;
	}

	public double getKi() {
		return Ki;
	}

	public void setKi(double ki) {
		Ki = ki;
	}

	public double getKd() {
		return Kd;
	}

	public void setKd(double kd) {
		Kd = kd;
	}
	
	public int getWRITE_OUTPUT_ITERATION() {
		return WRITE_OUTPUT_ITERATION;
	}

	public int getUPDATE_PRICE_INTERVAL() {
		return UPDATE_PRICE_INTERVAL;
	}

	public double getTOLERATED_AVERAGE_DELAY_SEC() {
		return TOLERATED_AVERAGE_DELAY_SEC;
	}

	public double getTOLL_ADJUSTMENT() {
		return TOLL_ADJUSTMENT;
	}

	public double getFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT() {
		return FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT;
	}

	public TollingApproach getTOLLING_APPROACH() {
		return TOLLING_APPROACH;
	}

	public double getTOLL_BLEND_FACTOR() {
		return TOLL_BLEND_FACTOR;
	}

	public void setTOLLING_APPROACH(TollingApproach tOLLING_APPROACH) {
		TOLLING_APPROACH = tOLLING_APPROACH;
	}

	public void setTOLL_ADJUSTMENT(double tOLL_ADJUSTMENT) {
		TOLL_ADJUSTMENT = tOLL_ADJUSTMENT;
	}

	public void setUPDATE_PRICE_INTERVAL(int uPDATE_PRICE_INTERVAL) {
		UPDATE_PRICE_INTERVAL = uPDATE_PRICE_INTERVAL;
	}

	public void setTOLL_BLEND_FACTOR(double tOLL_BLEND_FACTOR) {
		TOLL_BLEND_FACTOR = tOLL_BLEND_FACTOR;
	}

	public void setWRITE_OUTPUT_ITERATION(int wRITE_OUTPUT_ITERATION) {
		WRITE_OUTPUT_ITERATION = wRITE_OUTPUT_ITERATION;
	}

	public void setTOLERATED_AVERAGE_DELAY_SEC(double tOLERATED_AVERAGE_DELAY_SEC) {
		TOLERATED_AVERAGE_DELAY_SEC = tOLERATED_AVERAGE_DELAY_SEC;
	}

	public void setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(double fRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT) {
		FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT = fRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT;
	}

	public double getINITIAL_TOLL() {
		return INITIAL_TOLL;
	}

	public void setINITIAL_TOLL(double iNITIAL_TOLL) {
		INITIAL_TOLL = iNITIAL_TOLL;
	}

	public double getFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT() {
		return FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT;
	}

	public void setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(
			double fRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT) {
		FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT = fRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT;
	}

	@Override
	public String toString() {
		return "DecongestionConfigGroup [INITIAL_TOLL=" + INITIAL_TOLL + ", TOLL_ADJUSTMENT=" + TOLL_ADJUSTMENT
				+ ", TOLLING_APPROACH=" + TOLLING_APPROACH + ", TOLL_BLEND_FACTOR=" + TOLL_BLEND_FACTOR + ", Kp=" + Kp
				+ ", Ki=" + Ki + ", Kd=" + Kd + ", UPDATE_PRICE_INTERVAL=" + UPDATE_PRICE_INTERVAL
				+ ", WRITE_OUTPUT_ITERATION=" + WRITE_OUTPUT_ITERATION + ", TOLERATED_AVERAGE_DELAY_SEC="
				+ TOLERATED_AVERAGE_DELAY_SEC + ", FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT="
				+ FRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT + ", FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT="
				+ FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT + "]";
	}

	public boolean isRUN_FINAL_ANALYSIS() {
		return RUN_FINAL_ANALYSIS;
	}

	public void setRUN_FINAL_ANALYSIS(boolean rUN_FINAL_ANALYSIS) {
		RUN_FINAL_ANALYSIS = rUN_FINAL_ANALYSIS;
	}

	public boolean isWRITE_LINK_INFO_CHARTS() {
		return WRITE_LINK_INFO_CHARTS;
	}

	public void setWRITE_LINK_INFO_CHARTS(boolean wRITE_CHARTS) {
		WRITE_LINK_INFO_CHARTS = wRITE_CHARTS;
	}
			
}

