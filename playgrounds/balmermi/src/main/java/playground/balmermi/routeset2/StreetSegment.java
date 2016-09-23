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

package playground.balmermi.routeset2;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class StreetSegment implements Link {
	
	public final Set<Link> links = new HashSet<>();
	
	Link delegate ;

	public StreetSegment(Id<Link> id, Node from, Node to, Network network, double length, double freespeed, double capacity, double lanes) {
		delegate = NetworkUtils.createLink(id, from, to, network, length, freespeed, capacity, lanes);
	}

	@Override
	public Id<Link> getId() {
		return this.delegate.getId();
	}

	@Override
	public Coord getCoord() {
		return this.delegate.getCoord();
	}

	@Override
	public boolean setFromNode(Node node) {
		return this.delegate.setFromNode(node);
	}

	@Override
	public boolean setToNode(Node node) {
		return this.delegate.setToNode(node);
	}

	@Override
	public Node getToNode() {
		return this.delegate.getToNode();
	}

	@Override
	public Node getFromNode() {
		return this.delegate.getFromNode();
	}

	@Override
	public double getLength() {
		return this.delegate.getLength();
	}

	@Override
	public double getNumberOfLanes() {
		return this.delegate.getNumberOfLanes();
	}

	@Override
	public double getNumberOfLanes(double time) {
		return this.delegate.getNumberOfLanes(time);
	}

	@Override
	public double getFreespeed() {
		return this.delegate.getFreespeed();
	}

	@Override
	public double getFreespeed(double time) {
		return this.delegate.getFreespeed(time);
	}

	@Override
	public double getCapacity() {
		return this.delegate.getCapacity();
	}

	@Override
	public double getCapacity(double time) {
		return this.delegate.getCapacity(time);
	}

	@Override
	public void setFreespeed(double freespeed) {
		this.delegate.setFreespeed(freespeed);
	}

	@Override
	public void setLength(double length) {
		this.delegate.setLength(length);
	}

	@Override
	public void setNumberOfLanes(double lanes) {
		this.delegate.setNumberOfLanes(lanes);
	}

	@Override
	public void setCapacity(double capacity) {
		this.delegate.setCapacity(capacity);
	}

	@Override
	public void setAllowedModes(Set<String> modes) {
		this.delegate.setAllowedModes(modes);
	}

	@Override
	public Set<String> getAllowedModes() {
		return this.delegate.getAllowedModes();
	}

	@Override
	public double getFlowCapacityPerSec() {
		return this.delegate.getFlowCapacityPerSec();
	}

	@Override
	public double getFlowCapacityPerSec(double time) {
		return this.delegate.getFlowCapacityPerSec(time);
	}

	@Override
	public Attributes getAttributes() {
		return this.delegate.getAttributes();
	}
}
