/* *********************************************************************** *
 * project: org.matsim.*
 * LinkMock.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.testcases.fakes;

import java.util.EnumSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.world.Layer;

/**
 * A very simple Mock for {@link Link} to be used in tests.
 * Only stores an Id and from- and toNode (which both can be
 * <code>null</code>). Some getters return hard-coded defaults,
 * while all remaining methods just throw
 * {@link UnsupportedOperationException}s.
 *
 * @author mrieser
 */
public class FakeLink implements Link {

	private static final long serialVersionUID = 1L;

	private final Id id;
	private final Node fromNode;
	private final Node toNode;


	/**
	 * Creates a new link with the specified id and <code>null</code> for the
	 * fromNode and toNode
	 *
	 * @param id
	 */
	public FakeLink(final Id id) {
		this(id, null, null);
	}

	public FakeLink(final Id id, final Node fromNode, final Node toNode) {
		this.id = id;
		this.fromNode = fromNode;
		this.toNode = toNode;
	}

	public Node getFromNode() {
		return this.fromNode;
	}

	public Node getToNode() {
		return this.toNode;
	}

	public Set<TransportMode> getAllowedModes() {
		return EnumSet.of(TransportMode.car);
	}

	public double getCapacity(final double time) {
		return 2000.0;
	}

	public double getFreespeed(final double time) {
		return 15.0;
	}

	public double getLength() {
		return 300.0;
	}

	public double getNumberOfLanes(final double time) {
		return 1.0;
	}

	public void setAllowedModes(final Set<TransportMode> modes) {
		throw new UnsupportedOperationException();
	}

	public void setCapacity(final double capacity) {
		throw new UnsupportedOperationException();
	}

	public void setFreespeed(final double freespeed) {
		throw new UnsupportedOperationException();
	}

	public boolean setFromNode(final Node node) {
		throw new UnsupportedOperationException();
	}

	public void setLength(final double length) {
		throw new UnsupportedOperationException();
	}

	public void setNumberOfLanes(final double lanes) {
		throw new UnsupportedOperationException();
	}

	public boolean setToNode(final Node node) {
		throw new UnsupportedOperationException();
	}

	public Id getId() {
		return this.id;
	}

	@Deprecated
	public Layer getLayer() {
		throw new UnsupportedOperationException();
	}

	public Coord getCoord() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "FakeLink_" + this.id.toString();
	}
}
