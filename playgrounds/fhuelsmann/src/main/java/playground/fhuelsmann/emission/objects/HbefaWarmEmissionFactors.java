/* *********************************************************************** *
 * project: org.matsim.*
 * FhMain.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.fhuelsmann.emission.objects;

import java.util.HashMap;

import playground.benjamin.events.emissions.WarmPollutant;

public class HbefaWarmEmissionFactors {

	private final int road_Category ;
	private final String IDTS;
	private final double velocity;
	private final double RPA;
	private final double stop;

	private final HashMap<WarmPollutant, Double> emissionFactors;

	public HbefaWarmEmissionFactors(
			int road_Category, 
			String iDTS, 
			double velocity,
			double rPA,
			double stop, 
			double emissionFactorFC,
			double emissionFactorNOx,
			double emissionFactorCo2Rep,
			double emissionFactorCo2Total,
			double emissionFactorNO2, 
			double emissionFactorPM){

		this.road_Category= road_Category;
		this.IDTS = iDTS;
		this.velocity = velocity;
		this.RPA = rPA;
		this.stop = stop;

		this.emissionFactors = new HashMap<WarmPollutant, Double>();
		this.emissionFactors.put(WarmPollutant.FC, emissionFactorFC);
		this.emissionFactors.put(WarmPollutant.NOx, emissionFactorNOx);
		this.emissionFactors.put(WarmPollutant.NO2, emissionFactorNO2);
		this.emissionFactors.put(WarmPollutant.PM, emissionFactorPM);
		this.emissionFactors.put(WarmPollutant.CO2_TOTAL, emissionFactorCo2Total);
	}

	public double getVelocity() {
		return velocity;
	}
//	public int getRoad_Category() {
//		return Road_Category;
//	}
//	public String getIDTS() {
//		return IDTS;
//	}
//	public double getRPA() {
//		return RPA;
//	}
//	public double getStop() {
//		return stop;
//	}
	
	public Double getEf(WarmPollutant warmPollutant) {
		return this.emissionFactors.get(warmPollutant);
	}
}