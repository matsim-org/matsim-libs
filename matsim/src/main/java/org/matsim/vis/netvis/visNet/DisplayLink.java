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

package org.matsim.vis.netvis.visNet;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.netvis.DisplayableLinkI;
import org.matsim.vis.netvis.DrawableAgentI;
import org.matsim.vis.netvis.drawableNet.DrawableLinkI;
import org.matsim.world.Layer;

/**
 * @author gunnar
 */
public class DisplayLink implements DisplayableLinkI, DrawableLinkI, Link {

	private Node fromNode;
	private Node toNode;
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

	@Override
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

	@Override
	public double getStartEasting() {
		return ((DisplayNode) getFromNode()).getEasting();
	}

	@Override
	public double getEndEasting() {
		return ((DisplayNode) getToNode()).getEasting();
	}

	@Override
	public double getStartNorthing() {
		return ((DisplayNode) getFromNode()).getNorthing();
	}

	@Override
	public double getEndNorthing() {
		return ((DisplayNode) getToNode()).getNorthing();
	}

	private double getNodeDist() {
		return this.nodeDist;
	}

	@Override
	public AffineTransform getLinear2PlaneTransform() {
		return this.linear2PlaneTransform;
	}

	// -------------------- IMPLEMENTATION OF BasicLinkI --------------------

	@Override
	public boolean setFromNode(Node node) {
		this.fromNode = node;
		return true;
	}

	@Override
	public boolean setToNode(Node node) {
		this.toNode = node;
		return true;
	}

	@Override
	public Node getFromNode() {
		return this.fromNode;
	}

	@Override
	public Node getToNode() {
		return this.toNode;
	}

	@Override
	public double getCapacity() {
		return getCapacity(Time.UNDEFINED_TIME);
	}

	@Override
	public double getCapacity(double time) {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	@Override
	public double getFreespeed() {
		return getFreespeed(Time.UNDEFINED_TIME);
	}

	@Override
	public double getFreespeed(final double time) {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	@Override
	public double getLength() {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	@Override
	public void setFreespeed(double freespeed) {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	@Override
	public void setLength(double length) {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	@Override
	public void setCapacity(double capacity) {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	@Override
	public double getNumberOfLanes() {
		return getNumberOfLanes(Time.UNDEFINED_TIME);
	}

	@Override
	public double getNumberOfLanes(double time) {
		throw new UnsupportedOperationException("Method only implemented to fullfill requirements of BasicLinkI, which was extended after this class was written!");
	}

	@Override
	public Set<TransportMode> getAllowedModes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAllowedModes(Set<TransportMode> modes) {
		throw new UnsupportedOperationException();
	}


	// -------------------- IMPLEMENTATION OF TrafficLinkI --------------------

	public void setLength_m(double length_m) {
		this.length_m = length_m;
	}

	@Override
	public void setNumberOfLanes(double lanes) {
		this.lanes = lanes;
	}

	@Override
	public double getLength_m() {
		return this.length_m;
	}

	@Override
	public int getLanesAsInt(double time) {
		return Math.round((float)Math.max(this.lanes,1.0d));
	}

	// ---------- IMPLEMENTATION OF DrawableLinkI ----------

	@Override
	public int getDisplayValueCount() {
		return this.displayValue.length;
	}

	@Override
	public double getDisplayValue(int index) {
		return this.displayValue[index];
	}

	@Override
	public String getDisplayText() {
		return this.displayLabel;
	}

	@Override
	public Collection<DrawableAgentI> getMovingAgents() {
		return this.agents;
	}

	public void setMovingAgents(List<DrawableAgentI> newAgents) {
		this.agents = newAgents;
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public Layer getLayer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Coord getCoord() {
		throw new UnsupportedOperationException();
	}

}
