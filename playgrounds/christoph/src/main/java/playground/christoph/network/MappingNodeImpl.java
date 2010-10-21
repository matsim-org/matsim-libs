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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.christoph.network.mapping.Mapping;

public class MappingNodeImpl extends MappingNode {

	private Id id;
	private Coord coord;

	private Mapping downMapping;
	private Mapping upMapping;

	private Map<Id, Link> inLinks;
	private Map<Id, Link> outLinks;

	public MappingNodeImpl(Id id, Coord coord)
	{
		this.id = id;

		this.coord = coord;

		/*
		 * We have to sort the Maps to get deterministic Results
		 * when doing the NetworkThinning.
		 */
		this.inLinks = new TreeMap<Id, Link>(new IdComparator());
		this.outLinks = new TreeMap<Id, Link>(new IdComparator());
	}

	@Override
	public Map<Id, ? extends Link> getInLinks()
	{
		return this.inLinks;
	}

	@Override
	public Map<Id, ? extends Link> getOutLinks()
	{
		return this.outLinks;
	}

	@Override
	public boolean addInLink(Link link)
	{
		return (this.inLinks.put(link.getId(), link) != null);
	}

	@Override
	public boolean addOutLink(Link link)
	{
		return (this.outLinks.put(link.getId(), link) != null);
	}

	@Override
	public Coord getCoord()
	{
		return this.coord;
	}

	@Override
	public Id getId()
	{
		return this.id;
	}

	@Override
	public Mapping getDownMapping()
	{
		return this.downMapping;
	}

	@Override
	public void setDownMapping(Mapping mapping)
	{
		this.downMapping = mapping;
	}

	@Override
	public Mapping getUpMapping()
	{
		return this.upMapping;
	}

	@Override
	public void setUpMapping(Mapping mapping)
	{
		this.upMapping = mapping;
	}

	protected static class IdComparator implements Comparator<Id>, Serializable
	{
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Id l1, final Id l2)
		{
			return l1.compareTo(l2);
		}
	}
}