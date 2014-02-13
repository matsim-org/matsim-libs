/* *********************************************************************** *
 * project: org.matsim.*
 * ComparePerfQuadTreeCircularElliptical.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.scripts;

import org.apache.log4j.Logger;

import org.matsim.core.utils.collections.QuadTree;

/**
 * @author thibautd
 */
public class ComparePerfQuadTreeCircularElliptical {
	private static final Logger log =
		Logger.getLogger(ComparePerfQuadTreeCircularElliptical.class);

	public static void main(final String[] args) {
		final QuadTree<Object> qt = new QuadTree<Object>( 0 , 0 , 1 , 1 );

		for ( double x = 0; x <= 1; x += 0.001 ) {
			for ( double y = 0; y <= 1; y += 0.001 ) {
				qt.put( x , y , new Object() );
			}
		}

		final double startCircular = System.currentTimeMillis();
		qt.get( 0.5 , 0.5 , 0.5 );
		final double endCircular = System.currentTimeMillis();
		log.info( "time circular: "+(endCircular - startCircular)+"ms" );

		final double startElliptical = System.currentTimeMillis();
		qt.getElliptical(
				0.25 , 0.5 ,
				0.75 , 0.5 ,
				1 );
		final double endElliptical = System.currentTimeMillis();
		log.info( "time elliptical: "+(endElliptical - startElliptical)+"ms" );
	}
}

