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
package playground.droeder.Analysis.distance;

/**
 * @author droeder
 *
 */
public class DistAnalysisTrip {
	
	private int nrOfPlanElements;
	private int nrOfExpEvents;
	private boolean finished = false;
	
//	private double tripDist = 0; 
	private double inVehDist = 0;
	private double accesWalkDist = 0;
	private double switchWalkDist = 0;
	private double egressWalkDist = 0;
	
	private int lineCnt = 0;
	private int accesWalkCnt = 0;
	private int switchWalkCnt = 0;
	private int egressWalkCnt = 0;
	
	
	/**
	 * @return the nrOfPlanElements
	 */
	public int getNrOfPlanElements() {
		return nrOfPlanElements;
	}
	/**
	 * @return the nrOfExpEvents
	 */
	public int getNrOfExpEvents() {
		return nrOfExpEvents;
	}
	/**
	 * @return if trip is finished
	 */
	public boolean isFinished() {
		return finished;
	}
	/**
	 * @return the inVehDist
	 */
	public double getInVehDist() {
		return inVehDist;
	}
	/**
	 * @return the accesWalkDist
	 */
	public double getAccesWalkDist() {
		return accesWalkDist;
	}
	/**
	 * @return the switchWalkDist
	 */
	public double getSwitchWalkDist() {
		return switchWalkDist;
	}
	/**
	 * @return the egressWalkDist
	 */
	public double getEgressWalkDist() {
		return egressWalkDist;
	}
	/**
	 * @return the lineCnt
	 */
	public int getLineCnt() {
		return lineCnt;
	}
	/**
	 * @return the accesWalkCnt
	 */
	public int getAccesWalkCnt() {
		return accesWalkCnt;
	}
	/**
	 * @return the switchWalkCnt
	 */
	public int getSwitchWalkCnt() {
		return switchWalkCnt;
	}
	/**
	 * @return the egressWalkCnt
	 */
	public int getEgressWalkCnt() {
		return egressWalkCnt;
	}
	
}
