/* *********************************************************************** *
 * project: org.matsim.*
 * LQTest.java
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

package playground.gregor.sim2d_v4.experimental;

import java.util.ArrayList;
import java.util.List;

import playground.gregor.sim2d_v4.cgal.LinearQuadTreeLD;
import playground.gregor.sim2d_v4.cgal.LinearQuadTreeLD.Quad;
import playground.gregor.sim2d_v4.cgal.TwoDObject;

import com.vividsolutions.jts.geom.Envelope;

public class LQTest {

	public static void main(String [] args) {
		Envelope e = new Envelope(0,8,0,8);
		List<TwoDObject> objs = new ArrayList<TwoDObject>();
		objs.add(new Obj(2,6));
		objs.add(new Obj(4.2,7.1));
		objs.add(new Obj(4.2,6.2));
		objs.add(new Obj(4.2,4.2));
		objs.add(new Obj(4.2,3.8));
		objs.add(new Obj(6.2,3.8));
		LinearQuadTreeLD qt = new LinearQuadTreeLD(objs, e,null);
		
		Envelope q = new Envelope(3,5,3,5);
		System.out.println();
		List<Quad> resp = qt.query(q);
		for (Quad quad : resp) {
			System.out.println(quad);
		}
		
		
	}
	
	private static final class Obj implements TwoDObject {

		double x;
		double y;
		
		public Obj(double x, double y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public double getX() {
			return this.x;
		}

		@Override
		public double getY() {
			return this.y;
		}
		
	}
	
}
