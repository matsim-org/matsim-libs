/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayNode.java
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

package org.matsim.vis.netvis.visNet;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vis.netvis.drawableNet.DrawableNodeI;

/**
 * @author gunnar
 */
public class DisplayNode implements DrawableNodeI, Node {

	// -------------------- CLASS VARIABLES --------------------

	public static final double RADIUS_M = 5;

	// -------------------- MEMBER VARIABLES --------------------

	private final Map<Id, Link> inLinks;
	private final Map<Id, Link> outLinks;
	private Coord coord = null;
	private double displayValue;
	private String displayLabel;
	protected Id id;

	// -------------------- CONSTRUCTION --------------------

	public DisplayNode(Id id, DisplayNet network) {
		this.id = id;
		inLinks = new TreeMap<Id, Link>();
		outLinks = new TreeMap<Id, Link>();
	}

	// -------------------- SETTERS --------------------

	public void setDisplayValue(double value) {
		this.displayValue = value;
	}

	public void setDisplayText(String label) {
		this.displayLabel = label;
	}

	// -------------------- IMPLEMENTATION of BasicNodeI --------------------

	public boolean addInLink(Link link) {
		inLinks.put(link.getId(), link);
		return true;
	}

	public boolean addOutLink(Link link) {
		outLinks.put(link.getId(), link);
		return true;
	}

	public Map<Id, ? extends Link> getInLinks() {
		return inLinks;
	}

	public Map<Id, ? extends Link> getOutLinks() {
		return outLinks;
	}

	// -------------------- IMPLEMENTATION OF TrafficNodeI --------------------

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	public Coord getCoord() {
		return coord;
	}

	// ---------- IMPLEMENTATION OF DrawableNodeI ----------

	public double getDisplayValue() {
		return displayValue;
	}

	public String getDisplayText() {
		return displayLabel;
	}

	public double getNorthing() {
		return getCoord().getY();
	}

	public double getEasting() {
		return getCoord().getX();
	}

	// -------------------- MISC --------------------

	@Override
	public String toString() {
		return super.toString() + " at (" + getEasting() + "/" + getNorthing() + ")";
	}


	public Id getId() {
		return this.id;
	}

}