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

/**
VehCat;Road_Category;IDTS;TS;S (speed);RPA;%stop;mKr;EF_Nox;EF_CO2(rep.);EF_CO2(total);NO2;PM
 **/
public class HbefaObject {

	private static int Road_Category ;
	private final String IDTS;
	private final double velocity;
	private final double RPA;
	private final double stop;

	private final double emissionFactorNox;
	private final double mkr;
	private final double emissionFactorCo2Rep;
	private final double emissionFactorCo2Total;
	private final double no2;
	private final double pm;

	public HbefaObject(
			int road_Category, 
			String iDTS, 
			double velocity,
			double rPA,
			double stop, 
			double mkr,
			double emissionFactorNox,
			double emissionFactorCo2Rep,
			double emissionFactorCo2Total,
			double NO2, 
			double PM){

		this.Road_Category= road_Category;
		this.IDTS = iDTS;
		this.velocity = velocity;
		this.RPA = rPA;
		this.stop = stop;
		this.mkr = mkr;
		this.emissionFactorNox = emissionFactorNox;
		this.emissionFactorCo2Rep = emissionFactorCo2Rep;
		this.emissionFactorCo2Total = emissionFactorCo2Total;
		this.no2 = NO2;
		this.pm = PM;

	}

	public double getMkr() {
		return mkr;
	}
	public double getNo2() {
		return no2;
	}
	public double getPm() {
		return pm;
	}
	public double getEmissionFactorCo2Rep() {
		return emissionFactorCo2Rep;
	}
	public double getEmissionFactorCo2Total() {
		return emissionFactorCo2Total;
	}
	public double getEmissionFactorNox() {
		return emissionFactorNox;
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
	
	// TODO: change data format of HbefaTable!
	public Double getEf(String pollutant) {
		Double efFreeFlow = null;
		
		if(pollutant.equals("FC")) efFreeFlow = getMkr();
		else if (pollutant.equals("NOx")) efFreeFlow = getEmissionFactorNox();
		else if (pollutant.equals("CO2(total)")) efFreeFlow = getEmissionFactorCo2Total();
		else if (pollutant.equals("NO2")) efFreeFlow = getNo2();
		else if (pollutant.equals("PM")) efFreeFlow = getPm();
		
		return efFreeFlow;
	}
}