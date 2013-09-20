/* *********************************************************************** *
 * project: org.matsim.*
 * PSLPlanDataData
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package air.pathsize;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
/**
 * Parts of this data container can be removed when model is stable. Meanwhile they may help for debugging.
 * @author dgrether
 */
public class PSLPlanData {

	private int id;
	private Plan plan;
	private String mainMode;
	private List<Leg> mainModeLegs = new ArrayList<Leg>();
	private double pslValue;
	private double length;
	private double weight;

	public PSLPlanData(int id, Plan plan) {
		this.id = id;
		this.plan = plan;
	}

	public int getId() {
		return id;
	}

	public Plan getPlan() {
		return plan;
	}

	public String getMainMode() {
		return mainMode;
	}

	public void setMainMode(String mainMode) {
		this.mainMode = mainMode;
	}

	public List<Leg> getLegsOfMainMode() {
		return mainModeLegs;
	}

	public double getPslValue() {
		return pslValue;
	}

	public void setPslValue(double pslValue) {
		this.pslValue = pslValue;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
}