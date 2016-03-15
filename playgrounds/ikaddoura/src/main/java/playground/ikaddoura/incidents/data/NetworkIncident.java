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

	private String id = null;
	private double startTime = 0.;
	private double endTime = 0.;
	private Link link = null;
	private Link incidentLink = null;
	
	public NetworkIncident(String id, double startTime, double endTime) {
		this.id = id;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public String getId() {
		return id;
	}

	public void setLink(Link link) {
		this.link = link;
	}
	
	public Link getLink() {
		return link;
	}
	
	public void setIncidentLink(Link trafficIncidentLink) {
		this.incidentLink = trafficIncidentLink;
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

	public String parametersToString() {
		return "NetworkIncident [startTime=" + startTime + ", endTime=" + endTime
				+ ", incidentLink=" + incidentLink.getCapacity() + "-" + incidentLink.getFreespeed() + "-" + incidentLink.getNumberOfLanes() + "-" + incidentLink.getAllowedModes()
				+ ", link=" + link.getCapacity() + "-" + link.getFreespeed() + "-" + link.getNumberOfLanes() + "-" + link.getAllowedModes() + "]";
	}

	@Override
	public String toString() {
		return "NetworkIncident [id=" + id + ", startTime=" + startTime + ", endTime=" + endTime
				+ ", link=" + link.getCapacity() + "-" + link.getFreespeed() + "-" + link.getNumberOfLanes() + "-" + link.getAllowedModes()
				+ ", incidentLink=" + incidentLink.getCapacity() + "-" + incidentLink.getFreespeed() + "-" + incidentLink.getNumberOfLanes() + "-" + incidentLink.getAllowedModes() + "]";
	}

}

