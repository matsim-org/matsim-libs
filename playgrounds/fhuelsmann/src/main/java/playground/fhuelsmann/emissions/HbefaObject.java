/* *********************************************************************** *
 * project: org.matsim.*
 * BKickControler
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

package playground.fhuelsmann.emissions;
public class HbefaObject {

	private int Road_Category ;
	private String IDTS;
	private double velocity;
	private double RPA;
	private double stop;
	private double emission_factor;

	public HbefaObject(int road_Category, String iDTS, double velocity,
			double rPA, double stop, double emission_factor) {
		super();
		Road_Category = road_Category;
		IDTS = iDTS;
		this.velocity = velocity;
		RPA = rPA;
		this.stop = stop;
		this.emission_factor = emission_factor;
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
	}
	public double getEmission_factor() {
		return emission_factor;
	}
	public void setEmission_factor(double emission_factor) {
		this.emission_factor = emission_factor;
	}
}
