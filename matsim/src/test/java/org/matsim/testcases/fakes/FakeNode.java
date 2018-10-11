/* *********************************************************************** *
 * project: org.matsim.*
 * NodeMock.java
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

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * A very simple fake implementation of {@link Node} to be used in tests.
 * Only stores an Id and returns it in {@link #getId()}, all other
 * operations throw {@link UnsupportedOperationException}.
 *
 * @author mrieser
 */
public class FakeNode implements Node {

	private final Id<Node> id;

	public FakeNode(final Id<Node> id) {
		this.id = id;
	}

	@Override
	public Map<Id<Link>, ? extends Link> getInLinks() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Id<Link>, ? extends Link> getOutLinks() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addInLink(final Link link) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addOutLink(final Link link) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Coord getCoord() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Id<Node> getId() {
		return this.id;
	}

	@Override
	public Link removeInLink(Id<Link> linkId) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Link removeOutLink(Id<Link> outLinkId) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setCoord(Coord coord) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}
}
