/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingSpot.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.hitchiking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;

/**
 * @author thibautd
 */
public class HitchHikingSpots {
	public static final String ELEMENT_NAME = "histchHikingSpots";
	private final QuadTree<Link> quadTree;

	public HitchHikingSpots(
			final Collection<Id<Link>> hitchHikableLinks,
			final Network network) {
		this( getHitchHikableLinks( hitchHikableLinks , network ) );
	}

	public HitchHikingSpots(
			final Collection<Link> hitchHikableLinks) {
		this.quadTree = buildQt( hitchHikableLinks );
	}

	private static Collection<Link> getHitchHikableLinks(
			final Collection<Id<Link>> hitchHikableLinkIds,
			final Network network) {
		List<Link> hitchHikableLinks = new ArrayList<Link>(); 
		for (Id<Link> id : hitchHikableLinkIds) {
			Link l = network.getLinks().get( id );
			hitchHikableLinks.add( l );
		}
		return hitchHikableLinks;
	}

	private static QuadTree<Link> buildQt(
			final Collection<Link> hitchHikableLinks) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (Link l : hitchHikableLinks) {
			Coord c = l.getCoord();
			double x = c.getX();
			double y = c.getY();
			minX = x < minX ? x : minX;
			minY = y < minY ? y : minY;
			maxX = x > maxX ? x : maxX;
			maxY = y > maxY ? y : maxY;
		}

		QuadTree<Link> qt = new QuadTree<Link>( minX , minY , maxX , maxY );

		for (Link l : hitchHikableLinks) {
			qt.put( l.getCoord().getX() , l.getCoord().getY() , l );
		}

		return qt;
	}

	public Collection<Link> getSpots() {
		return quadTree.values();
	}

	public Link getNearestSpot(final Coord c) {
		return quadTree.getClosest(c.getX(), c.getY());
	}

	public Collection<Link> getSpots(final Coord c , final double distance) {
		return getSpots( c.getX() , c.getY() , distance );
	}

	public Collection<Link> getSpots(final double x , final double y , final double distance) {
		return quadTree.getDisk(x, y, distance);
	}
}

