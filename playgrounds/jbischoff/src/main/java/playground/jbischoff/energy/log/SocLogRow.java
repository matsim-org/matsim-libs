/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.energy.log;

import java.util.Comparator;

import org.matsim.api.core.v01.Id;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */
public class SocLogRow implements Comparable<SocLogRow> {
	private Id agentId;


	private double time;
	private double absoluteLOC;
	private double relativeLOC;
	

	public SocLogRow(Id agentId, double time, double soc, double rsoc) {
		this.agentId = agentId;
		this.time = time;
		this.absoluteLOC = soc;
		this.relativeLOC = rsoc;
	}

	public Id getAgentId() {
		return agentId;
	}


	public double getTime() {
		return time;
	}

	public double getAbsoluteLOC() {
		return absoluteLOC;
	}

	public double getRelativeLOC() {
		return relativeLOC;
	}

	@Override
	public int compareTo(SocLogRow arg0) {
		return getAgentId().compareTo(arg0.getAgentId());
	}
 
}


