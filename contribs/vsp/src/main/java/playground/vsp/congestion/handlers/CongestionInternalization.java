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

package playground.vsp.congestion.handlers;

import org.matsim.api.core.v01.events.LinkLeaveEvent;

import playground.vsp.congestion.DelayInfo;

/**
 * @author ikaddoura
 *
 */
public interface CongestionInternalization {
	
	/**
	 * <p> This is the core method which can be implemented in different ways
	 * in order to change the logic how to internalize delays.
	 */
	public void calculateCongestion(LinkLeaveEvent event, DelayInfo delayInfo);

	/**
	 * <p> The total delay calculated as 'link leave time minus freespeed leave time'
	 */
	public double getTotalDelay();
	
	/**
	 * The total delay which is internalized, i.e. allocated to causing agents
	 */
	public double getTotalInternalizedDelay();
	
	/**
	 * Total rounding error delay which is not internalized.
	 */
	public double getTotalRoundingErrorDelay();
	
	/**
	 * Writes the basic information to a file
	 */
	public void writeCongestionStats(String fileName);
	
}
