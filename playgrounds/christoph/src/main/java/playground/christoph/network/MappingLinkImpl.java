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

package playground.christoph.network;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.christoph.network.mapping.Mapping;

public class MappingLinkImpl extends MappingLink {

	private static final Logger log = Logger.getLogger(MappingLinkImpl.class);

	private Node fromNode;
	private Node toNode;
	private Id id;
	private Coord coord;

	private double length = 0.0;

	private Mapping downMapping;
	private Mapping upMapping;

	public MappingLinkImpl(Id id, Node fromNode, Node toNode)
	{
		this.id = id;
		this.fromNode = fromNode;
		this.toNode = toNode;

		double x = (fromNode.getCoord().getX() + toNode.getCoord().getX()) / 2;
		double y = (fromNode.getCoord().getY() + toNode.getCoord().getY()) / 2;
		this.coord = new CoordImpl(x, y);
	}

	@Override
	public Node getFromNode()
	{
		return this.fromNode;
	}

	@Override
	public Node getToNode()
	{
		return this.toNode;
	}

	@Override
	public Set<String> getAllowedModes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getCapacity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getCapacity(double time) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFreespeed() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFreespeed(double time) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getLength()
	{
//		return this.mapping.getLength();
		return this.length;
	}

	@Override
	public double getNumberOfLanes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getNumberOfLanes(double time) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setAllowedModes(Set<String> modes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCapacity(double capacity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFreespeed(double freespeed) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean setFromNode(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setLength(double length) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setNumberOfLanes(double lanes) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean setToNode(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Id getId()
	{
		return this.id;
	}

	@Override
	public Coord getCoord()
	{
		return this.coord;
	}

	@Override
	public Mapping getDownMapping()
	{
		return this.downMapping;
	}

	@Override
	public void setDownMapping(Mapping mapping)
	{
		this.downMapping = mapping;

		this.length = mapping.getLength();

//		Object object = mapping.getInput();
//		if (object instanceof List)
//		{
//			List<Object> list = (List) object;
//
//			for (Object element : list)
//			{
//				if (element instanceof Link)
//				{
//					Link link = (Link) element;
//					this.length = this.length + link.getLength();
//				}
//			}
//		}
//		else if (object instanceof Link)
//		{
//			Link link = (Link) object;
//			this.length = this.length + link.getLength();
//		}
//		else
//		{
//			log.warn("Could not map the Length of the MappingLink.");
//		}
	}

	@Override
	public Mapping getUpMapping()
	{
		return this.upMapping;
	}

	@Override
	public void setUpMapping(Mapping mapping)
	{
		this.upMapping = mapping;
	}
}
