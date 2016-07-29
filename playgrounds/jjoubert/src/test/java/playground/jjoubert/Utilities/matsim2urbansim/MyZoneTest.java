/* *********************************************************************** *
 * project: org.matsim.*
 * MyZoneTest.java
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

package playground.jjoubert.Utilities.matsim2urbansim;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class MyZoneTest{
	private GeometryFactory gf = new GeometryFactory();
	private Polygon[] ps = setupPolygon();
	private Id<MyZone> id = Id.create("0", MyZone.class);

	@Test
	public void testMyZoneConstructor(){
		MyZone mz = new MyZone(ps, gf, id);
		Assert.assertEquals("MyZone not created.", true, mz != null);
	}
	
	@Test
	public void testGetId(){
		MyZone mz = new MyZone(ps, gf, id);
		Assert.assertEquals("Wrong Id returned.", true, mz.getId().equals(id));
		Assert.assertEquals("Id not unique.", false, mz.getId().equals("0"));
	}
	
	private Polygon[] setupPolygon(){
		Coordinate c1 = new Coordinate(0,0);
		Coordinate c2 = new Coordinate(5,0);
		Coordinate c3 = new Coordinate(5,5);
		Coordinate c4 = new Coordinate(0,5);		
		Coordinate[] ca1 = {c1, c2, c3, c4, c1};		
		LinearRing lr1 = gf.createLinearRing(ca1);
		Polygon p1 = gf.createPolygon(lr1, null);
		Polygon[] ps = {p1};
		return ps;
	}
}

