/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.data;

import org.matsim.api.core.v01.network.Link;

/**
* @author ikaddoura
*/

public class NetworkIncident {

	private double startTime = 0.;
	private double endTime = 0.;
	private Link link = null;
	private Link incidentLink = null;
	
	public NetworkIncident(double startTime, double endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public Link getLink() {
		return link;
	}

	public void setLink(Link link) {
		this.link = link;
	}

	public Link getIncidentLink() {
		return incidentLink;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	@Override
	public String toString() {
		return "NetworkIncident [startTime=" + startTime + ", endTime=" + endTime + ", incidentLink=" + incidentLink.getCapacity() + "-" + incidentLink.getFreespeed() + "-" + incidentLink.getNumberOfLanes()
				+ ", link=" + link.getCapacity() + "-" + link.getFreespeed() + "-" + link.getNumberOfLanes() + "]";
	}

	public void setIncidentLink(Link trafficIncidentLink) {
		this.incidentLink = trafficIncidentLink;
	}

}

