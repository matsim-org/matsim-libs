/* *********************************************************************** *
 * project: org.matsim.*
 * PlanComparison.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.dgrether.analysis.population;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
/**
 * This Class provides a data object to compare to iterations. 
 * @author dgrether
 * 
 */
public class DgAnalysisPopulation {
	
	private static final Logger log = Logger.getLogger(DgAnalysisPopulation.class);
	
	public static final Id RUNID1 = new IdImpl("run1");
	public static final Id RUNID2 = new IdImpl("run2");
	
	private double minIncome = Double.POSITIVE_INFINITY;
	private double maxIncome = Double.NEGATIVE_INFINITY;

	private Tuple<Coord, Coord> boundingBox = null;
	
	private Map<Id, DgPersonData> table;
	/**
	 * Creates a PlanComparison Object with the initial size
	 * @param size
	 */
	public DgAnalysisPopulation() {
     table = new LinkedHashMap<Id, DgPersonData>();
	}
	
	public Map<Id, DgPersonData> getPersonData() {
		return table;
	}
	
	public int calculateNumberOfCarPlans(Id runId) {
		int carplans = 0;
		for (DgPersonData d : table.values()) {
			if (d.getPlanData().get(runId).getPlan().getType().equals(PlanImpl.Type.CAR)){
				carplans++;
			}
		}
		return carplans;
	}

	public void calculateMinMaxIncome() {
		double y;
		for (DgPersonData d : this.getPersonData().values()) {
			y = d.getIncome().getIncome();
			if (y < this.minIncome) {
				this.minIncome = y;
			}
			if (y > this.maxIncome) {
				this.maxIncome = y;
			}
		}
	}

	private void calculateBoundingBox(){
		Coord minNW = new CoordImpl(Double.MAX_VALUE, Double.MAX_VALUE);
		Coord maxSE = new CoordImpl(Double.MIN_VALUE, Double.MIN_VALUE);
		for (DgPersonData pers : this.getPersonData().values()){
			Coord current = pers.getFirstActivity().getCoord();
			if (current == null) {
				throw new IllegalStateException("Person id " + pers.getPersonId() + " has no coord for home activity!");
			}
			if (current.getX() < minNW.getX()){
				minNW.setX(current.getX());
			}
			if (current.getY() < minNW.getY()){
				minNW.setY(current.getY());
			}
			if (current.getX() > maxSE.getX()){
				maxSE.setX(current.getX());
			}
			if (current.getY() > maxSE.getY()){
				maxSE.setY(current.getY());
			}
		}
		this.boundingBox = new Tuple<Coord, Coord>(minNW, maxSE);
	}
	
	/**
	 * first entry of Tuple is min north west coord, second max south east
	 */
	public Tuple<Coord, Coord> getBoundingBox(){
		if (this.boundingBox == null) {
			this.calculateBoundingBox();
		}
		return this.boundingBox;
	}
	
	
	public double getMinIncome() {
		return minIncome;
	}
	
	public double getMaxIncome() {
		return maxIncome;
	}
	
	
	
	
}
