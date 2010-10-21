/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.network;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

//public class SubLink implements BasicLink, Link{
public class SubLink implements Link{

	private Network network;
	private Node from;
	private Node to;
	private Link parentLink;

	public SubLink(Network network, Node from, Node to, Link link)
	{
		this.network = network;
		this.from = from;
		this.to = to;
		this.parentLink = link;
	}

	public Link getParentLink()
	{
		return this.parentLink;
	}

	@Override
	public Node getFromNode()
	{
		return this.from;
	}

	@Override
	public Node getToNode()
	{
		return this.to;
	}

	@Override
	public double getCapacity() {
		return this.parentLink.getCapacity();
	}

	@Override
	public double getCapacity(final double time)
	{
		return this.parentLink.getCapacity(time);
	}

	@Override
	public double getFreespeed() {
		return this.parentLink.getFreespeed();
	}

	@Override
	public double getFreespeed(final double time)
	{
		return this.parentLink.getFreespeed(time);
	}

	@Override
	public double getLength()
	{
		return this.parentLink.getLength();
	}

	@Override
	public double getNumberOfLanes() {
		return this.parentLink.getNumberOfLanes();
	}

	@Override
	public double getNumberOfLanes(final double time)
	{
		return this.parentLink.getNumberOfLanes(time);
	}

	@Override
	public Set<String> getAllowedModes()
	{
		return this.parentLink.getAllowedModes();
	}

	@Override
	public void setAllowedModes(Set<String> modes)
	{
		// nothing to do...
	}

	@Override
	public void setCapacity(double capacity)
	{
		// nothing to do...
	}

	@Override
	public void setFreespeed(double freespeed)
	{
		// nothing to do...
	}

	@Override
	public boolean setFromNode(Node node)
	{
		this.from = node;
		return true;
	}

	@Override
	public void setLength(double length)
	{
		// nothing to do...
	}

	@Override
	public void setNumberOfLanes(double lanes)
	{
		// nothing to do...
	}

	@Override
	public boolean setToNode(Node node)
	{
		this.to = node;
		return true;
	}

	@Override
	public Id getId()
	{
		return this.parentLink.getId();
	}

	@Override
	public Coord getCoord()
	{
		return this.parentLink.getCoord();
	}

	@Override
	public boolean equals(final Object other)
	{
		if (other instanceof Link)
		{
			return this.getId().equals(((Link)other).getId());
		}
		return false;
	}
}