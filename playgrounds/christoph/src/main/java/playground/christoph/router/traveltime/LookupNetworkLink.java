/* *********************************************************************** *
 * project: org.matsim.*
 * LookupNetworkLink.java
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

package playground.christoph.router.traveltime;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.misc.Time;

public class LookupNetworkLink implements Link {

	private final Link link;
	private double linkTravelTime = Time.UNDEFINED_TIME;
	
	/*package*/ LookupNetworkLink(Link link) {
		this.link = link;
	}
	
	public Link getLink() {
		return link;
	}

	public double getLinkTravelTime() {
		return linkTravelTime;
	}
	
	public void setLinkTravelTime(double linkTravelTime) {
		this.linkTravelTime = linkTravelTime;
	}
	
	@Override
	public Id getId() {
		return link.getId();
	}

	@Override
	public Node getFromNode() {
		return link.getFromNode();
	}
	
	@Override
	public Node getToNode() {
		return link.getToNode();
	}

	@Override
	public Set<String> getAllowedModes() {
		return link.getAllowedModes();
	}

	@Override
	public double getCapacity() {
		return link.getCapacity();
	}

	@Override
	public double getCapacity(double time) {
		return link.getCapacity(time);
	}

	@Override
	public double getFreespeed() {
		return link.getFreespeed();
	}

	@Override
	public double getFreespeed(double time) {
		return link.getFreespeed(time);
	}

	@Override
	public double getLength() {
		return link.getLength();
	}

	@Override
	public double getNumberOfLanes() {
		return link.getNumberOfLanes();
	}

	@Override
	public double getNumberOfLanes(double time) {
		return link.getNumberOfLanes(time);
	}

	@Override
	public void setAllowedModes(Set<String> modes) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public void setCapacity(double capacity) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public void setFreespeed(double freespeed) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public boolean setFromNode(Node node) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public void setLength(double length) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public void setNumberOfLanes(double lanes) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public boolean setToNode(Node node) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Coord getCoord() {
		return this.link.getCoord();
	}

}