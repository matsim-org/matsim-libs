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

import org.matsim.basic.v01.BasicActImpl;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.misc.Time;

public class Act extends BasicActImpl implements Serializable {

	private static final long serialVersionUID = 1L;

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	private boolean isPrimary = false;

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
			this.setCoord(new Coord(x.doubleValue(), y.doubleValue())); // set the coord, because they are defined
		} else if (link == null) { // both coords are == null, therefore link MUST exist
			Gbl.errorMsg(this + "[the coord AND the link is not defined! forbidden!]");
		}
		if (link != null) {
			setLinkFromString(link);
		}

		if (startTime != null) {
			this.startTime = Time.parseTime(startTime);
		}
		if (endTime != null) {
			this.endTime = Time.parseTime(endTime);
		}
		if (dur != null) {
			this.dur = Time.parseTime(dur);
		}
		if (isPrimary != null) {
			this.isPrimary = true;
		}
	}

	public Act(final String type, final double x, final double y, final Link link,
			final double startTime, final double endTime, final double dur, final boolean isPrimary) {
		this.type = type.intern();
		this.setCoord(new Coord(x, y));
		this.link = link;
		this.startTime = startTime;
		this.endTime = endTime;
		this.dur = dur;
		this.isPrimary = isPrimary;
	}

	public Act(final Act act) {
		this.type = act.type;
		// Act coord could be null according to first c'tor!
		CoordI c = act.getCoord() == null ? null : new Coord(act.getCoord());
		this.setCoord(c);
		this.link = act.link;
		this.startTime = act.startTime;
		this.endTime = act.endTime;
		this.dur = act.dur;
		this.isPrimary = act.isPrimary;
		this.setFacility(act.getFacility());
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

	protected void setFacility(final String f_id) {
		Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
		if (facilities == null) { throw new RuntimeException("Facilities Layer does not exist!"); }
		this.facility = (Facility)facilities.getLocation(f_id);
		if (this.facility == null) { throw new RuntimeException("facility id="+f_id+" does not exist"); }
	}

	protected void setLinkFromString(final String link) {
		NetworkLayer network = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		if (network == null) {
			throw new RuntimeException("Network layer does not exist");
		}
		this.link = (Link)network.getLocation(link);

		if (this.link == null) {
			throw new RuntimeException("link=" + link +" does not exist");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[type=" + this.type + "]" +
				"[coord=" + this.getCoord() + "]" +
				"[link=" + this.link + "]" +
				"[startTime=" + Time.writeTime(this.startTime) + "]" +
				"[endTime=" + Time.writeTime(this.endTime) + "]" +
				"[dur=" + Time.writeTime(this.dur) + "]" +
				"[isPrimary=" + this.isPrimary + "]";
	}


	// BasicAct is not yet serializable, so we have to serialize it by hand
	// plus Link Reference serialized as ID int
	private void writeObject(final ObjectOutputStream s) throws IOException {
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
		private NetworkLayer network = null;

		public void addLink(final Act act, final String linkId) {
			if (this.network == null) {
				this.network = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
				if (this.network == null) {
					throw new RuntimeException("Network layer does not exist.");
				}
			}
			act.link = (Link)this.network.getLocation(linkId);
			if (act.link == null) {
				throw new RuntimeException("link=" + linkId +" does not exist.");
			}
		}
	}

	private static LinkBuilder linkBuilder = new LinkBuilder();

	public static void setLinkBuilder(final LinkBuilder builder) {
		linkBuilder = builder;
	}

	private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
	  // the `size' field.
	  s.defaultReadObject();
	  setStartTime(s.readDouble());
	  setDur(s.readDouble());
	  setEndTime(s.readDouble());
	  setType((String)s.readObject());
	  linkBuilder.addLink(this,(String)s.readObject());
	}


	public final double getDur() {
		return this.dur;
	}


	public final void setDur(final double dur) {
		this.dur = dur;
	}

	@Override // here to return correct link type
	public final Link getLink() {
		return (Link)this.link;
	}


	public final Id getLinkId() { // convenience method
		return this.link.getId();
	}

}
