/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.johannes.gsv.synPop.sim;

import java.util.Random;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * @author johannes
 *
 */
public class MutateHomeLocation implements Mutator {

	private PreparedGeometry bounds;
	
	private Random random;
	
	private GeometryFactory factory = new GeometryFactory();
	
	private Envelope env;
	
	private double deltaX;
	
	private double deltaY;
	
	public MutateHomeLocation(Geometry bounds, Random random) {
		this.bounds = PreparedGeometryFactory.prepare(bounds);
		this.random = random;
		
		env = bounds.getEnvelopeInternal();
		
		deltaX = env.getMaxX() - env.getMinX();
		deltaY = env.getMaxY() - env.getMinY();
	}
	
	@Override
	public boolean mutate(ProxyPerson original, ProxyPerson modified) {
		boolean hit = false;
		
		double x = 0;
		double y = 0;
		
		while(!hit) {
			x = env.getMinX() + random.nextDouble() * deltaX;
			y = env.getMinY() + random.nextDouble() * deltaY;
			
			Point p = factory.createPoint(new Coordinate(x, y));
			hit = bounds.contains(p);
		}
		
		modified.setAttribute(CommonKeys.PERSON_HOME_COORD_X, x);
		modified.setAttribute(CommonKeys.PERSON_HOME_COORD_Y, y);
		
		return true;

	}

}
