/* *********************************************************************** *
 * project: org.matsim.*
 * QuadTreeRebuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Helps working with quad trees, by re-building a new quad tree
 * if necessary (avoids having to care about bounds).
 * @author thibautd
 */
public class QuadTreeRebuilder<T> {
	private static final double EPSILON = 1E-7;
	private double minX = Double.POSITIVE_INFINITY;
	private double minY = Double.POSITIVE_INFINITY;
	private double maxX = Double.NEGATIVE_INFINITY;
	private double maxY = Double.NEGATIVE_INFINITY;

	private final List< Tuple<Coord, T> > elements = new ArrayList< Tuple<Coord, T> >();
	private QuadTree<T> quadTree = null;

	public synchronized QuadTree<T> getQuadTree() {
		if ( quadTree == null ) buildQuadTree();
		return quadTree;
	}

	private void buildQuadTree() {
		assert minX <= maxX : minX +" > "+maxX;
		assert minY <= maxY : minY +" > "+maxY;
		this.quadTree = new QuadTree<T>(
				minX - EPSILON ,
				minY - EPSILON ,
				maxX + EPSILON ,
				maxY + EPSILON );

		for ( Tuple< Coord , T > element : elements ) {
			try {
				quadTree.put(
						element.getFirst().getX(),
						element.getFirst().getY(),
						element.getSecond() );
			}
			catch ( Exception e ) {
				// if insertion fail, give more informative error message.
				throw new RuntimeException( "problem at inserting element "
						+elements.indexOf( element )+" of "
						+elements.size()+": "
						+element+" with bounds"
						+" minX="+minX
						+" minY="+minY
						+" maxX="+maxX
						+" maxY="+maxY ,
						e );
			}
		}
	}

	public void put( final double x , final double y , final T element ) {
		put( new CoordImpl( x , y ) , element );
	}

	public void put( final Coord coord , final T element ) {
		elements.add( new Tuple< Coord , T >( coord , element ) );
		if ( quadTree != null && inBounds( coord ) ) {
			quadTree.put( coord.getX() , coord.getY() , element );
		}
		else {
			minX = Math.min( coord.getX() , minX );
			minY = Math.min( coord.getY() , minY );
			maxX = Math.max( coord.getX() , maxX );
			maxY = Math.max( coord.getY() , maxY );
			quadTree = null;
		}
	}

	private boolean inBounds(final Coord coord) {
		return coord.getX() > minX + EPSILON &&
			coord.getX() < maxX - EPSILON &&
			coord.getY() > minY + EPSILON &&
			coord.getY() < maxY - EPSILON ;
	}
}

