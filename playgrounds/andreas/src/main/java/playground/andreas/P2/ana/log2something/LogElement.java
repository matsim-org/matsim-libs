/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.P2.ana.log2something;

import org.apache.log4j.Logger;

/**
 * Simple storage class for one log entry
 * 
 * @author aneumann
 *
 */
public class LogElement {
	
	private static final Logger log = Logger.getLogger(LogTex.class);

	private int iteration;
	private String coopId;
	private String status;
	private String planId;
	private String creatorId;
	private int nVeh;
	private int nPax;
	private double score;
	private double budget;
	private double startTime;
	private double endTime;
	private String[] stopsToBeServed;
	
	public String getUniquePlanIdentifier() {
		return this.coopId + "_" + this.planId;
	}
	
	public int getIteration() {
		return iteration;
	}
	public void setIteration(int iteration) {
		this.iteration = iteration;
	}
	public String getCoopId() {
		return coopId;
	}
	public void setCoopId(String coopId) {
		this.coopId = coopId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getPlanId() {
		return planId;
	}
	public void setPlanId(String planId) {
		this.planId = planId;
	}
	public String getCreatorId() {
		return creatorId;
	}
	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}
	public int getnVeh() {
		return nVeh;
	}
	public void setnVeh(int nVeh) {
		this.nVeh = nVeh;
	}
	public int getnPax() {
		return nPax;
	}
	public void setnPax(int nPax) {
		this.nPax = nPax;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public double getBudget() {
		return budget;
	}
	public void setBudget(double budget) {
		this.budget = budget;
	}
	public double getStartTime() {
		return startTime;
	}
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}
	public double getEndTime() {
		return endTime;
	}
	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}
	public String[] getStopsToBeServed() {
		return stopsToBeServed;
	}
	public void setStopsToBeServed(String[] stopsToBeServed) {
		this.stopsToBeServed = stopsToBeServed;
	}
	
	
	
	
}
