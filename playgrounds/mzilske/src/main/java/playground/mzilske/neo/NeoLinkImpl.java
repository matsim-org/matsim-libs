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

package playground.mzilske.neo;

import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.neo4j.graphdb.Direction;

public class NeoLinkImpl implements Link {

	static final String KEY_ID = "link_id";
	static final String KEY_LENGTH = "length";
	static final String KEY_CAPACITY = "capacity";
	static final String KEY_FREESPEED = "freespeed";
	static final String KEY_LANES = "lanes";

	private org.neo4j.graphdb.Node underlyingNode;

	public NeoLinkImpl(org.neo4j.graphdb.Node nextNode) {
		this.underlyingNode = nextNode;
	}

	@Override
	public Set<String> getAllowedModes() {
		throw new RuntimeException();
	}

	@Override
	public double getCapacity() {
		return (Double) underlyingNode.getProperty(KEY_CAPACITY);
	}

	@Override
	public double getCapacity(double time) {
		return getCapacity();
	}

	@Override
	public double getFreespeed() {
		return (Double) underlyingNode.getProperty(KEY_FREESPEED);
	}

	@Override
	public double getFreespeed(double time) {
		return getFreespeed();
	}

	@Override
	public Node getFromNode() {
		org.neo4j.graphdb.Node n = underlyingNode.getSingleRelationship(RelationshipTypes.LINK_TO, Direction.INCOMING).getStartNode();
		return new NeoNodeImpl(n);
	}

	@Override
	public double getLength() {
		return (Double) underlyingNode.getProperty(KEY_LENGTH);
	}

	@Override
	public double getNumberOfLanes() {
		return (Double) underlyingNode.getProperty(KEY_LANES);
	}

	@Override
	public double getNumberOfLanes(double time) {
		return getNumberOfLanes();
	}

	@Override
	public Node getToNode() {
		org.neo4j.graphdb.Node n = underlyingNode.getSingleRelationship(RelationshipTypes.LINK_TO, Direction.OUTGOING).getEndNode();
		return new NeoNodeImpl(n);
	}

	@Override
	public void setAllowedModes(Set<String> modes) {
		throw new RuntimeException();
	}

	@Override
	public void setCapacity(double capacity) {
		throw new RuntimeException();
	}

	@Override
	public void setFreespeed(double freespeed) {
		throw new RuntimeException();
	}

	@Override
	public boolean setFromNode(Node node) {
		throw new RuntimeException();
	}

	@Override
	public void setLength(double length) {
		throw new RuntimeException();
	}

	@Override
	public void setNumberOfLanes(double lanes) {
		throw new RuntimeException();
	}

	@Override
	public boolean setToNode(Node node) {
		throw new RuntimeException();
	}

	@Override
	public Id getId() {
		return new IdImpl((String) underlyingNode.getProperty(KEY_ID));
	}

	@Override
	public Coord getCoord() {
		return getToNode().getCoord();
	}

	public org.neo4j.graphdb.Node getUnderlyingNode() {
		return underlyingNode;
	}

}
