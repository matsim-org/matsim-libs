/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayLink.java
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

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.utils.vis.netvis.DisplayableLinkI;
import org.matsim.utils.vis.netvis.DrawableAgentI;
import org.matsim.utils.vis.netvis.drawableNet.DrawableLinkI;

/**
 * @author gunnar
 */
public class DisplayLink implements DisplayableLinkI, DrawableLinkI, BasicLink {

	private BasicNode fromNode;
	private BasicNode toNode;
	public static final double LANE_WIDTH = 4.0;
	private double length_m = 0;
	private double lanes = 0;
	private AffineTransform linear2PlaneTransform = null;
	private double nodeDist;
	private double[] displayValue = new double[1];
	private String displayLabel;
	private List<DrawableAgentI> agents = new ArrayList<DrawableAgentI>();
	protected Id id;

	// -------------------- CONSTRUCTION AND INITIALIZATION --------------------

	DisplayLink(Id id, DisplayNet network) {
		this.id = id;
	}

	public void setDisplValueCnt(int cnt) {
		if (this.displayValue.length != cnt)
			this.displayValue = new double[cnt];
	}

	public void setDisplayValue(double value, int index) {
		this.displayValue[index] = value;
	}

	public void setDisplayLabel(String label) {
		this.displayLabel = label;
	}

	public void build() {

		final double deltaNorthing = getEndNorthing() - getStartNorthing();
		final double deltaEasting = getEndEasting() - getStartEasting();
		double result = deltaNorthing * deltaNorthing;
		result += deltaEasting * deltaEasting;
		this.nodeDist = Math.sqrt(result);

		double offset_m = DisplayNode.RADIUS_M;
		double length_m = this.getNodeDist() - 2.0 * offset_m;
		if (length_m <= 0) {
			length_m = this.getNodeDist() / 2.0;
			offset_m = (this.getNodeDist() - length_m) / 2.0;
		}
		this.linear2PlaneTransform = newLinear2PlaneTransform(offset_m, length_m);
	}

	// -------------------- INTERNALS --------------------

	/*
	 * maps (x,y) onto world coordinates, where x is distance from this link's
	 * startPoint and y is a normal shift to this link. this transform allows to
	 * draw anything "from left to right" onto the link whithout having to worry
	 * about the link's true direction.
	 */
	private AffineTransform newLinear2PlaneTransform(double offset_m, double displayedLength_m) {

		// 3. translate link onto original position
		double tx = getStartEasting();
		double ty = getStartNorthing();
		AffineTransform result = AffineTransform.getTranslateInstance(tx, ty);

		// 2. rotate link into original direction
		double dx = getEndEasting() - getStartEasting();
		double dy = getEndNorthing() - getStartNorthing();
		double theta = Math.atan2(dy, dx);
		result.rotate(theta);

		// 1. scale link
		double sx = displayedLength_m / getLength_m();
		double sy = 1;
		result.scale(sx, sy);

		// 0. translate link by target offset
		tx = offset_m * getLength_m() / displayedLength_m;
		ty = 0;
		result.translate(tx, ty);

		// result = 3.translate o 2.rotate o 1.scale o 0.translate
		return result;
	}

	public double getStartEasting() {
		return ((DisplayNode) getFromNode()).getEasting();
	}

	public double getEndEasting() {
		return ((DisplayNode) getToNode()).getEasting();
	}

	public double getStartNorthing() {
		return ((DisplayNode) getFromNode()).getNorthing();
	}

	public double getEndNorthing() {
		return ((DisplayNode) getToNode()).getNorthing();
	}

	public double getNodeDist() {
		return this.nodeDist;
	}

	public AffineTransform getLinear2PlaneTransform() {
		return this.linear2PlaneTransform;
	}

	// -------------------- IMPLEMENTATION OF BasicLinkI --------------------

	public boolean setFromNode(BasicNode node) {
		this.fromNode = node;
		return true;
	}

	public boolean setToNode(BasicNode node) {
		this.toNode = node;
		return true;
	}

	public BasicNode getFromNode() {
		return this.fromNode;
	}

	public BasicNode getToNode() {
		return this.toNode;
	}

	public double getCapacity() {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	public double getFreespeed(final double time) {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	public double getLength() {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	public void setFreespeed(double freespeed) {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	public void setLength(double length) {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	public void setCapacity(double capacity) {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	public double getLanes() {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}
	

	// -------------------- IMPLEMENTATION OF TrafficLinkI --------------------

	public void setLength_m(double length_m) {
		this.length_m = length_m;
	}

	public void setLanes(double lanes) {
		this.lanes = lanes;
	}

	public double getLength_m() {
		return this.length_m;
	}

	public int getLanesAsInt() {
		return Math.round((float)Math.max(this.lanes,1.0d));
	}

	// ---------- IMPLEMENTATION OF DrawableLinkI ----------

	public int getDisplayValueCount() {
		return this.displayValue.length;
	}

	public double getDisplayValue(int index) {
		return this.displayValue[index];
	}

	public String getDisplayText() {
		return this.displayLabel;
	}

	public Collection<DrawableAgentI> getMovingAgents() {
		return this.agents;
	}

	public void setMovingAgents(List<DrawableAgentI> newAgents) {
		this.agents = newAgents;
	}


	public Id getId() {
		return this.id;
	}




}
