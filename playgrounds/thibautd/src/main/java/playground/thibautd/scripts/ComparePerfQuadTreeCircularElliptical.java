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

import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;

import org.matsim.core.utils.collections.QuadTree;

/**
 * @author thibautd
 */
public class ComparePerfQuadTreeCircularElliptical {
	private static final Logger log =
		Logger.getLogger(ComparePerfQuadTreeCircularElliptical.class);

	public static void main(final String[] args) {
		for ( Settings settings : Arrays.asList(
					new Settings(
						// few points
						0, 0, 1,
						-0.5, 0, 0.5, 0, 2,
						0.1 ),
					new Settings(
						// lots points
						0, 0, 1,
						-0.5, 0, 0.5, 0, 2,
						0.001 ),
					new Settings(
						// with useless space
						0, 0, 0.25,
						-0.2, 0, 0.2, 0, .45,
						0.0003 ),
					new Settings(
						// with lots ofuseless space
						0, 0, 0.025,
						-0.02, 0, 0.02, 0, .045,
						0.0003 )
					) ) {
			log.info( "######################################################################" );
			log.info( "start new test: "+settings );
			log.info( "build quad tree" );
			final QuadTree<Object> qt = new QuadTree<Object>( -1 , -1 , 1 , 1 );

			for ( double x = 0; x <= 1; x += settings.pointStep ) {
				for ( double y = 0; y <= 1; y += settings.pointStep ) {
					qt.put( x , y , new Object() );
				}
			}
			log.info( "build quad tree: DONE" );

			{
			final double startCircular = System.currentTimeMillis();
				final Collection<Object> circle = qt.getDisk(
						settings.centerCircleX,
						settings.centerCircleY,
						settings.radiusCircle);
				final double endCircular = System.currentTimeMillis();
				log.info( "time circular: "+(endCircular - startCircular)+"ms" );
				log.info( circle.size()+" elements in disk" );
			}

			{
			final double startElliptical = System.currentTimeMillis();
				final Collection<Object> ellipse = qt.getElliptical(
						settings.ellipseX1 , settings.ellipseY1 ,
						settings.ellipseX2 , settings.ellipseY2 ,
						settings.ellipseDiameter );
				final double endElliptical = System.currentTimeMillis();
				log.info( "time elliptical: "+(endElliptical - startElliptical)+"ms" );
				log.info( ellipse.size()+" elements in ellipse" );
			}
		}
	}

	private static final class Settings {
		public final double centerCircleX, centerCircleY, radiusCircle;
		public final double ellipseX1, ellipseY1;
		public final double ellipseX2, ellipseY2;
		public final double ellipseDiameter;
		public final double pointStep;
		
		public Settings(double centerCircleX, double centerCircleY,
				double radiusCircle, double ellipseX1, double ellipseY1,
				double ellipseX2, double ellipseY2, double ellipseDiameter,
				double pointStep) {
			this.centerCircleX = centerCircleX;
			this.centerCircleY = centerCircleY;
			this.radiusCircle = radiusCircle;
			this.ellipseX1 = ellipseX1;
			this.ellipseY1 = ellipseY1;
			this.ellipseX2 = ellipseX2;
			this.ellipseY2 = ellipseY2;
			this.ellipseDiameter = ellipseDiameter;
			this.pointStep = pointStep;
		}

		@Override
		public String toString() {
			return "center( "+centerCircleX+" , "+centerCircleY+" , "+radiusCircle+" ) "+
				"ellipse( "+ellipseX1+" , "+ellipseY1+" , "+ellipseX2+" , "+ellipseY2+" , "+ellipseDiameter+" ) "+
				"step="+pointStep;
		}
	}
}

