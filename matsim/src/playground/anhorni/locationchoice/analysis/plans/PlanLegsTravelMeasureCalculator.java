/* *********************************************************************** *
 * project: org.matsim.*
 * PlanLegsTravelDistanceCalculator.java
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


package playground.anhorni.locationchoice.analysis.plans;

import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.population.Plan;

public abstract class PlanLegsTravelMeasureCalculator {
	
	//private static final Logger log = Logger.getLogger(PlanLegsTravelMeasureCalculator.class);
	
	protected double sumLegsTravelMeasure = 0.0;	
	protected double nbrOfLegs;
	
	protected String mode = "all";
	protected String actType = "all";
	protected boolean crowFly = false;	
	protected List<Double> legTravelMeasures = new Vector<Double>();
	
	public abstract List<Double> handle(final Plan plan, boolean wayThere);
	
	
	public void reset() {
		this.legTravelMeasures.clear();
		this.sumLegsTravelMeasure = 0.0;
		this.nbrOfLegs = 0.0;
	}
		
	public double getSumLegsTravelMeasure() {
		return sumLegsTravelMeasure;
	}

	public double getNbrOfLegs() {
		return nbrOfLegs;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getActType() {
		return actType;
	}

	public void setActType(String actType) {
		this.actType = actType;
	}

	public boolean isCrowFly() {
		return crowFly;
	}

	public void setCrowFly(boolean crowFly) {
		this.crowFly = crowFly;
	}
}
