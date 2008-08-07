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

package org.matsim.utils.vis.netvis.visNet;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.vis.netvis.drawableNet.DrawableNodeI;

/**
 * @author gunnar
 */
public class DisplayNode implements DrawableNodeI, BasicNode {

	// -------------------- CLASS VARIABLES --------------------

	public static final double RADIUS_M = 5;

	// -------------------- MEMBER VARIABLES --------------------

	private final Map<Id, BasicLink> inLinks;
	private final Map<Id, BasicLink> outLinks;
	private Coord coord = null;
	private double displayValue;
	private String displayLabel;
	protected Id id;

	// -------------------- CONSTRUCTION --------------------

	public DisplayNode(Id id, DisplayNet network) {
		this.id = id;
		inLinks = new TreeMap<Id, BasicLink>();
		outLinks = new TreeMap<Id, BasicLink>();
	}

	// -------------------- SETTERS --------------------

	public void setDisplayValue(double value) {
		this.displayValue = value;
	}

	public void setDisplayText(String label) {
		this.displayLabel = label;
	}

	// -------------------- IMPLEMENTATION of BasicNodeI --------------------

	public boolean addInLink(BasicLink link) {
		inLinks.put(link.getId(), link);
		return true;
	}

	public boolean addOutLink(BasicLink link) {
		outLinks.put(link.getId(), link);
		return true;
	}

	public Map<Id, ? extends BasicLink> getInLinks() {
		return inLinks;
	}

	public Map<Id, ? extends BasicLink> getOutLinks() {
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