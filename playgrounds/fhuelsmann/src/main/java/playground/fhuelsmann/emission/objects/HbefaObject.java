package playground.fhuelsmann.emission.objects;
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

/**


VehCat;Road_Category;IDTS;TS;S (speed);RPA;%stop;mKr;EF_Nox;EF_CO2(rep.);EF_CO2(total);NO2;PM

**/
public class HbefaObject {

	private static int Road_Category ;
	private String IDTS;
	private double velocity;
	private double RPA;
	private double stop;
	
	
	public double getMkr() {
		return mkr;
	}
	public void setMkr(double mkr) {
		this.mkr = mkr;
	}
	public double getNo2() {
		return no2;
	}
	public void setNo2(double no2) {
		this.no2 = no2;
	}
	public double getPm() {
		return pm;
	}
	public void setPm(double pm) {
		this.pm = pm;
	}

	public double getEmissionFactorCo2Rep() {
		return emissionFactorCo2Rep;
	}
	public void setEmissionFactorCo2Rep(double emissionFactorCo2Rep) {
		this.emissionFactorCo2Rep = emissionFactorCo2Rep;
	}
	public double getEmissionFactorCo2Total() {
		return emissionFactorCo2Total;
	}
	public void setEmissionFactorCo2Total(double emissionFactorCo2Total) {
		this.emissionFactorCo2Total = emissionFactorCo2Total;
	}
	
	private double emissionFactorNox;
	private double mkr;
	private double emissionFactorCo2Rep;
	private double emissionFactorCo2Total;
	private double no2;
	private double pm;
	

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
		this.IDTS= iDTS;
		this.velocity= velocity;
		this.RPA=rPA;
		this.stop= stop;
		this.mkr= mkr;
		this.emissionFactorNox = emissionFactorNox;
		this.emissionFactorCo2Rep = emissionFactorCo2Rep;
		this.emissionFactorCo2Total = emissionFactorCo2Total;
		this.no2 =NO2;
		this.pm= PM;
		
	}
	public double getEmissionFactorNox() {
		return emissionFactorNox;
	}
	public void setEmissionFactorNox(double emissionFactorNox) {
		this.emissionFactorNox = emissionFactorNox;
	}
	public int getRoad_Category() {
		return Road_Category;
	}
	public void setRoad_Category(int road_Category) {
		Road_Category = road_Category;
	}
	public String getIDTS() {
		return IDTS;
	}
	public void setIDTS(String iDTS) {
		IDTS = iDTS;
	}
	public double getVelocity() {
		return velocity;
	}
	public void setVelocity(double velocity) {
		this.velocity = velocity;
	}
	public double getRPA() {
		return RPA;
	}
	public void setRPA(double rPA) {
		RPA = rPA;
	}
	public double getStop() {
		return stop;
	}
	public void setStop(double stop) {
		this.stop = stop;
	}}
