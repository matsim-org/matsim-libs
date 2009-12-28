/******************************************************************************
 *project: org.matsim.*
 * FlatLink.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.graph.flatnetwork;

import java.awt.geom.Line2D;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

public class FlatLink {
	public Node fromNode;
	public Node toNode;
	
	public double fromX,fromY,toX,toY;
	
	public Line2D.Double line2d;
	public Link link;
	
	public Id reverseId = null;
	
	protected boolean hasReverseLink = false;
	
	public double length = 0;
	
	public boolean isNewLink = false;
	
	public FlatLink(Link link)
	{
		this.link = link;
		this.length = link.getLength();
		this.fromNode = link.getFromNode();
		this.toNode = link.getToNode();
		
		this.fromX = fromNode.getCoord().getX();
		this.fromY = fromNode.getCoord().getY();
		
		this.toX = toNode.getCoord().getX();
		this.toY = toNode.getCoord().getY();
		
		this.line2d = new Line2D.Double( fromX,  fromY,  toX,  toY);
	}
	
	public FlatLink(Link link, Node fromNode, Node toNode)
	{
		this.link = link;
		
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.fromX = fromNode.getCoord().getX();
		this.fromY = fromNode.getCoord().getY();
		
		this.toX = toNode.getCoord().getX();
		this.toY = toNode.getCoord().getY();
		
		this.line2d = new Line2D.Double( fromX,  fromY,  toX,  toY);
	}
	
	public void setReverseId(Id reverseId)
	{
		this.reverseId = reverseId;
		this.hasReverseLink = true;
	}
	
	public boolean hasReverseLink()
	{
		return hasReverseLink;
	}
	
	public void setReverseLinkTrue()
	{
		hasReverseLink = true;	
	}
	
}
