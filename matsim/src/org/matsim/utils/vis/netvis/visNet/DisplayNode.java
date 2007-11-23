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

import org.matsim.basic.v01.BasicIdentified;
import org.matsim.basic.v01.BasicLinkSet;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicLinkSetI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.vis.netvis.drawableNet.DrawableNodeI;

/**
 * @author gunnar
 */
public class DisplayNode extends BasicIdentified implements DrawableNodeI, BasicNodeI {

	// -------------------- CLASS VARIABLES --------------------

	public static final double RADIUS_M = 5;

	// -------------------- MEMBER VARIABLES --------------------

	private final BasicLinkSet inLinks;
	private final BasicLinkSet outLinks;
	private CoordI coord = null;
	private double displayValue;
	private String displayLabel;

	// -------------------- CONSTRUCTION --------------------

	public DisplayNode(IdI id, DisplayNet network) {
		super(id);
		inLinks = new BasicLinkSet();
		outLinks = new BasicLinkSet();
	}

	// -------------------- SETTERS --------------------

	public void setDisplayValue(double value) {
		this.displayValue = value;
	}

	public void setDisplayText(String label) {
		this.displayLabel = label;
	}

	// -------------------- IMPLEMENTATION of BasicNodeI --------------------

	public boolean addInLink(BasicLinkI link) {
		return inLinks.add(link);
	}

	public boolean addOutLink(BasicLinkI link) {
		return outLinks.add(link);
	}

	public BasicLinkSetI getInLinks() {
		return inLinks;
	}

	public BasicLinkSetI getOutLinks() {
		return outLinks;
	}

	// -------------------- IMPLEMENTATION OF TrafficNodeI --------------------

	public void setCoord(CoordI coord) {
		this.coord = coord;
	}

	public CoordI getCoord() {
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

	public String toString() {
		return super.toString() + " at (" + getEasting() + "/" + getNorthing() + ")";
	}

}