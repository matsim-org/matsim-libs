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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;

public class SubLinkV1 extends LinkImpl implements Link{

	private LinkImpl parentLink;

	public SubLinkV1(NetworkImpl network, Node from, Node to, LinkImpl link,
		final double length, final double freespeed, final double capacity, final double numLanes)
	{
		super(link.getId(), from, to, network, length, freespeed, capacity, numLanes);
		this.parentLink = link;
	}

	public LinkImpl getParentLink()
	{
		return parentLink;
	}

	@Override
	public SubNode getFromNode()
	{
		return (SubNode)this.from;
	}

	@Override
	public SubNode getToNode()
	{
		return (SubNode)this.to;
	}

	/**
	 * This method returns the capacity as set in the xml defining the network. Be aware
	 * that this capacity is not normalized in time, it depends on the period set
	 * in the network file (the capperiod attribute).
	 *
 	 * @param time - the current time
	 * @return the capacity per network's capperiod timestep
	 */
	@Override
	public double getCapacity(final double time)
	{
		return parentLink.getCapacity(time);
	}

	/**
	 * This method returns the freespeed velocity in meter per seconds.
	 *
	 * @param time - the current time
	 * @return freespeed
	 */
	@Override
	public double getFreespeed(final double time)
	{
		return parentLink.getFreespeed(time);
	}

	@Override
	public double getLength()
	{
		return parentLink.getLength();
	}

	@Override
	public double getNumberOfLanes(final double time)
	{
		return parentLink.getNumberOfLanes(time);
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