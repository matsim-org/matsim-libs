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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import com.vividsolutions.jts.geom.Envelope;
/**
 * This Class provides a data object to compare to iterations. 
 * @author dgrether
 * 
 */
public class DgAnalysisPopulation {
	
	private static final Logger log = Logger.getLogger(DgAnalysisPopulation.class);
	
	private Double minIncome = null; 
	private Double maxIncome = null; 
	private Double totalIncome = null;

	private Envelope boundingBox = null;
	
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
	
	public List<DgAnalysisPopulation> getQuantiles(int numberOfQuantiles, Comparator<DgPersonData> comparator){
		List<DgPersonData> list = new ArrayList<DgPersonData>(this.table.values());
		Collections.sort(list, comparator);
		int personsPerQuantile = list.size() / numberOfQuantiles;
		List<DgAnalysisPopulation> result = new ArrayList<DgAnalysisPopulation>();
		DgAnalysisPopulation quantilePop = null;
		for (int i = 0; i < numberOfQuantiles; i++){
			quantilePop = new DgAnalysisPopulation();
			result.add(quantilePop);
			for (int j = 0; j < personsPerQuantile; j++) {
				DgPersonData p = list.remove(0);
				quantilePop.getPersonData().put(p.getPersonId(), p);
			}
		}
		if (!list.isEmpty()){
			log.warn("base list after creating quantiles still has " + list.size() + " entries, adding to last quantile");
			for (DgPersonData p : list){
				quantilePop.getPersonData().put(p.getPersonId(), p);
			}
		}
		return result;
	}
	
	public int calculateNumberOfCarPlans(Id runId) {
		int carplans = 0;
		for (DgPersonData d : table.values()) {
			if (((PlanImpl) d.getPlanData().get(runId).getPlan()).getType().equals(PlanImpl.DeprecatedConstants.CAR)){
				carplans++;
			}
		}
		return carplans;
	}
	
	public void calculateIncomeData() {
		this.calculateMinMaxIncome();
		this.calculateTotalIncome();
	}


	public void calculateTotalIncome() {
		double i = 0;
		for (DgPersonData d : this.getPersonData().values()){
			i += d.getIncome().getIncome();
		}
		this.totalIncome = i;
	}

	public void calculateMinMaxIncome() {
		this.minIncome = Double.POSITIVE_INFINITY;
		this.maxIncome = Double.NEGATIVE_INFINITY;
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

	public Double calcAverageScoreDifference(Id runId1, Id runId2) {
		Double deltaScoreSum = 0.0;
		for (DgPersonData d : this.getPersonData().values()){
			DgPlanData planDataRun1 = d.getPlanData().get(runId1);
			DgPlanData planDataRun2 = d.getPlanData().get(runId2);
			deltaScoreSum += (planDataRun2.getScore() - planDataRun1.getScore());
		}
		Double avg = null;
		avg = deltaScoreSum/this.getPersonData().size();
		return avg;
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
		this.boundingBox = new Envelope();
		this.boundingBox.init(minNW.getX(), maxSE.getX(), minNW.getY(), maxSE.getY());
	}
	
	public Double getMinIncome() {
		return minIncome;
	}
	
	public Double getMaxIncome() {
		return maxIncome;
	}
	
	public Double getTotalIncome(){
		return this.totalIncome;
	}

	public Envelope getBoundingBox() {
		if (this.boundingBox == null) {
			this.calculateBoundingBox();
		}
		return this.boundingBox;
	}

	
	
	
	
}
