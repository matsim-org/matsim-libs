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
	
	private TollingApproach TOLLING_APPROACH = TollingApproach.V8;
	private double TOLL_ADJUSTMENT = 0.1;
	private int UPDATE_PRICE_INTERVAL = 1;
	
	private double INITIAL_TOLL = -1.0; // relevant for V6 (Set to a negative value to use the average delay cost as initial toll!)
	private double TOLL_BLEND_FACTOR = 1.0; // relevant for: V4, V8 (Set to 1.0 if tolls in previous iterations should be ignored!)
	
	private int WRITE_OUTPUT_ITERATION = 10;
	private double TOLERATED_AVERAGE_DELAY_SEC = 1.0;
	private double FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT = 1.0;
	
	public enum TollingApproach {
        NoPricing, V0, V1, V2, V3, V4, V5, V6, V7, V8
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
		
}

