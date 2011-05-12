/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.Analysis.Trips;

import java.util.ArrayList;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author droeder
 *
 */
public abstract class AbstractAnalysisTrip {
	protected Coordinate start;
	protected Coordinate end;
	protected String mode = null;

	//all modes
	public double tripTTime = 0.0;
	
	// pt only
	public int accesWalkCnt = 0;
	public int accesWaitCnt = 0;
	public int egressWalkCnt = 0;
	public int switchWalkCnt= 0;
	public int switchWaitCnt = 0;
	public int lineCnt = 0;
	
	public double accesWalkTTime = 0.0;
	public double accesWaitTime = 0.0;
	public double egressWalkTTime = 0.0;
	public double switchWalkTTime = 0.0;
	public double switchWaitTime = 0.0;
	public double lineTTime = 0.0;
	
	public void addElements(ArrayList<PlanElement> elements){
		this.analyzeElements(elements);
	}
	
	private void analyzeElements(ArrayList<PlanElement> elements) {
		this.findMode(elements);
		//if no zones in TripSet are defined, coords not necessary
		if(!(((Activity) elements.get(0)).getCoord() == null) && !(((Activity) elements.get(elements.size() - 1)).getCoord() == null)){
			this.start = new Coordinate(((Activity) elements.get(0)).getCoord().getX(), 
					((Activity) elements.get(0)).getCoord().getY());
			this.end = new Coordinate(((Activity) elements.get(elements.size() - 1)).getCoord().getX(), 
					((Activity) elements.get(elements.size() - 1)).getCoord().getY());
		}
	}
	
	// not essential but good to prevent mixing up different modes
	private void findMode(ArrayList<PlanElement> elements) {
		for(PlanElement p : elements){
			if(p instanceof Leg){
				if(((Leg) p).getMode().equals(TransportMode.transit_walk)){
					this.mode = TransportMode.transit_walk;
				}else{
					this.mode = ((Leg) p).getMode();
					return;
				}
			}
		}
	}
	
	public String getMode(){
		return this.mode;
	}
	
	/**
	 * @return
	 */
	public Geometry getStart() {
		return new GeometryFactory().createPoint(this.start);
	}

	/**
	 * @return
	 */
	public Geometry getEnd() {
		return new GeometryFactory().createPoint(this.end);
	}
	
	/**
	 * @return the tripTTime
	 */
	public Double getTripTTime() {
		return tripTTime;
	}

	/**
	 * @return the accesWalkCnt
	 */
	public int getAccesWalkCnt() {
		return accesWalkCnt;
	}

	/**
	 * @return the accesWaitCnt
	 */
	public int getAccesWaitCnt() {
		return accesWaitCnt;
	}

	/**
	 * @return the egressWalkCnt
	 */
	public int getEgressWalkCnt() {
		return egressWalkCnt;
	}

	/**
	 * @return the switchWalkCnt
	 */
	public int getSwitchWalkCnt() {
		return switchWalkCnt;
	}

	/**
	 * @return the switchWaitCnt
	 */
	public int getSwitchWaitCnt() {
		return switchWaitCnt;
	}

	/**
	 * @return the lineCnt
	 */
	public int getLineCnt() {
		return lineCnt;
	}

	/**
	 * @return the accesWalkTTime
	 */
	public double getAccesWalkTTime() {
		return accesWalkTTime;
	}

	/**
	 * @return the accesWaitTime
	 */
	public double getAccesWaitTime() {
		return accesWaitTime;
	}

	/**
	 * @return the egressWalkTTime
	 */
	public double getEgressWalkTTime() {
		return egressWalkTTime;
	}

	/**
	 * @return the switchWalkTTime
	 */
	public double getSwitchWalkTTime() {
		return switchWalkTTime;
	}

	/**
	 * @return the switchWaitTime
	 */
	public double getSwitchWaitTime() {
		return switchWaitTime;
	}

	/**
	 * @return the lineTTime
	 */
	public double getLineTTime() {
		return lineTTime;
	}


}
