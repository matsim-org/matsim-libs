/* *********************************************************************** *
 * project: org.matsim.*
 * Act.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.plans;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.matsim.basic.v01.BasicAct;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;

public class Act extends BasicAct implements Serializable{

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final long serialVersionUID = 4319965778110179621L;

	private int refId = Integer.MIN_VALUE;
	private CoordI coord = null;
	private boolean isPrimary = false;

	protected double startTime = Gbl.UNDEFINED_TIME;
	protected double dur = Gbl.UNDEFINED_TIME;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Act(final String type, final Double x, final Double y, final String link,
						final String startTime, final String endTime, final String dur, final String isPrimary) {
		this.type = type.intern();
		// check if either coord and/or link are set
		if (((x==null) && (y!=null)) || ((x!=null) && (y==null))) {
			Gbl.errorMsg(this + "[either both or non of the coordinates must exist.]");
		} else if ((x!=null) && (y!=null)) {
			this.coord = new Coord(x.doubleValue(), y.doubleValue()); // set the coord, because they are defined
		} else if (link == null) { // both coords are == null, therefore link MUST exist
			Gbl.errorMsg(this + "[the coord AND the link is not defined! forbidden!]");
		}
		if (link != null) {
			setLinkFromString(link);
		}

		if (startTime != null) {
			this.startTime = Gbl.parseTime(startTime);
		}
		if (endTime != null) {
			this.endTime = Gbl.parseTime(endTime);
		}
		if (dur != null) {
			this.dur = Gbl.parseTime(dur);
		}
		if (isPrimary != null) {
			this.isPrimary = true;
		}
	}

	public Act(final String type, final String x, final String y, final String link,
						 final String startTime, final String endTime, final String dur, final String isPrimary) {
		this(type, (x == null) ? null : Double.valueOf(x),
							(y == null) ? null : Double.valueOf(y),
							link, startTime, endTime, dur, isPrimary);
	}

	public Act(final String type, final double x, final double y, final Link link,
			final double startTime, final double endTime, final double dur, final boolean isPrimary) {
		this.type = type.intern();
		this.coord = new Coord(x, y);
		this.link = link;
		this.startTime = startTime;
		this.endTime = endTime;
		this.dur = dur;
		this.isPrimary = isPrimary;
	}

	public Act(final Act act) {
		this.type = act.type;
		// Act coord could be null according to first c'tor!
		this.coord = act.coord == null ? null : new Coord(act.coord);
		this.link = act.link;
		this.startTime = act.startTime;
		this.endTime = act.endTime;
		this.dur = act.dur;
		this.isPrimary = act.isPrimary;
		this.refId = act.refId;
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	public final boolean isPrimary() {
		return this.isPrimary;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setCoord(final CoordI coord) {
		this.coord = coord;
	}

	public final void setRefId(final int refId) {
		this.refId = refId;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final CoordI getCoord() {
		return this.coord;
	}

	public final int getRefId() {
		return this.refId;
	}
	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	protected void setLinkFromString(final String link)
	{
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		if (network == null) {
			Gbl.errorMsg(this + "[link=" + link +" network layer does not exist]");
		}
		this.link = (Link)network.getLocation(link);
		if (this.link == null) {
			Gbl.errorMsg(this + "[link=" + link +" link does not exist]");
		}
	}

	@Override
	public final String toString() {
		return "[type=" + this.type + "]" +
				"[coord=" + this.coord + "]" +
				"[link=" + this.link + "]" +
				"[startTime=" + Gbl.writeTime(this.startTime) + "]" +
				"[endTime=" + Gbl.writeTime(this.endTime) + "]" +
				"[dur=" + Gbl.writeTime(this.dur) + "]" +
				"[isPrimary=" + this.isPrimary + "]";
	}


	// BasicAct is not yet serializable, so we have to serialize it by hand
	// plus Link Reference serialized as ID int
	private void writeObject(final ObjectOutputStream s) throws IOException
	{
	    // The standard non-transient fields.
	  s.defaultWriteObject();
	  s.writeDouble(getStartTime());
	  s.writeDouble(getDur());
	  s.writeDouble(getEndTime());
	  s.writeObject(getType());
	  s.writeObject(this.link.getId().toString());
	}

	// This routebuilder could be exchanged for suppliing other
	//kinds of network e.g. in OnTheFlyClient
	public static class LinkBuilder {
		private static NetworkLayer network = null;

		public void addLink(final Act act, final String linkId) {
			network = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
			if (network == null) {
				Gbl.errorMsg(this + "[link=" + linkId +" network layer does not exist]");
			}
			act.link = (Link)network.getLocation(linkId);
			if (act.link == null) {
				Gbl.errorMsg(this + "[link=" + linkId +" link does not exist]");
			}
		}
	}

	private static LinkBuilder linkBuilder = new LinkBuilder();

	public static void setLinkBuilder(final LinkBuilder builder) {
		linkBuilder = builder;
	}

	private void readObject(final ObjectInputStream s)
	  throws IOException, ClassNotFoundException
	{
	  // the `size' field.
	  s.defaultReadObject();
	  setStartTime(s.readDouble());
	  setDur(s.readDouble());
	  setEndTime(s.readDouble());
	  setType((String)s.readObject());
	  linkBuilder.addLink(this,(String)s.readObject());
	}

	public final double getStartTime() {
		return this.startTime;
	}

	public final double getDur() {
		return this.dur;
	}

	public final void setStartTime(final double startTime) {
		this.startTime = startTime;
	}

	public final void setDur(final double dur) {
		this.dur = dur;
	}

	@Override // here to return correct link type
	public final Link getLink() {
		return (Link)this.link;
	}


	public final String getLinkId() { // convenience method
		return this.link.getId().toString();
	}

}
