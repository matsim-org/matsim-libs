/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.data.vehtrajectories;

/**
 * @author mrieser
 */
public class VehicleTrajectory {

	private final int vehNr;
	private final int tag;
	private final String origZ;
	private final String destZ;
	private final double startTime;
	private final double travelTime;
	private int upstreamNode = -1;
	private int[] travelledNodes = null;
	private double[] timeStamps = null;
	private double[] travelledNodeTimes = null;
	private double[] jamTimes = null;

	/**
	 * @param vehNr
	 * @param tag
	 * @param origZ
	 * @param destZ
	 * @param startTime departure time in seconds since midnight
	 * @param travelTime travel time in seconds
	 */
	public VehicleTrajectory(final int vehNr, final int tag, final String origZ, final String destZ, final double startTime, final double travelTime) {
		this.vehNr = vehNr;
		this.tag = tag;
		this.origZ = origZ;
		this.destZ = destZ;
		this.startTime = startTime;
		this.travelTime = travelTime;
	}

	public void setTravelledNodes(final int upstreamNode, final int[] nodes) {
		this.upstreamNode = upstreamNode;
		this.travelledNodes = nodes;
	}

	public int[] getTravelledNodes() {
		return this.travelledNodes;
	}
	
	public int getUpstreamNode() {
		return this.upstreamNode;
	}
	
	public void setTimeStamps(double[] timeStamps) {
		this.timeStamps = timeStamps;
	}
	
	public double[] getTimeStamps() {
		return timeStamps;
	}
	
	public void setTravelledNodeTimes(final double[] times) {
		this.travelledNodeTimes = times;
	}

	public double[] getTravelledNodeTimes() {
		return this.travelledNodeTimes;
	}
	
	public void setJamTimes(double[] jamTimes) {
		this.jamTimes = jamTimes;
	}
	
	public double[] getJamTimes() {
		return jamTimes;
	}
	
	public int getVehNr() {
		return this.vehNr;
	}

	public int getTag() {
		return this.tag;
	}

	public String getOrigZone() {
		return this.origZ;
	}

	public String getDestZone() {
		return this.destZ;
	}

	/**
	 * @return departure time in seconds since midnight
	 */
	public double getStartTime() {
		return this.startTime;
	}

	/**
	 * @return travel time in seconds
	 */
	public double getTravelTime() {
		return this.travelTime;
	}
}
