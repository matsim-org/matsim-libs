/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.freight.digicore.analysis.chain.chainSimilarity;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.analysis.chain.chainSimilarity.geometric.GeometricChainSimilarityAnalyser;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class GeometricChainSimilarityAnalyserTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testConstructor() {
	}
	
	@Test
	public void testGetBuffer(){
		DigicoreVehicle vehicle = setupVehicle();
		
		/* Chain 1. */
		DigicoreChain chain = vehicle.getChains().get(0);
		Polygon p1 = (Polygon) GeometricChainSimilarityAnalyser.getBuffer(chain, 0.5).getEnvelope();
		List<Coordinate> list = getCoordinateList(p1.getCoordinates());
		Assert.assertTrue("Coordinate (0,0) not in buffer.", list.contains(new Coordinate(0.0, 0.0)));
		Assert.assertTrue("Coordinate (1,0) not in buffer.", list.contains(new Coordinate(1.0, 0.0)));
		Assert.assertTrue("Coordinate (0,3) not in buffer.", list.contains(new Coordinate(0.0, 3.0)));
		Assert.assertTrue("Coordinate (1,3) not in buffer.", list.contains(new Coordinate(1.0, 3.0)));
		
		/* Chain 2. */
		chain = vehicle.getChains().get(1);
		Polygon p2 = (Polygon) GeometricChainSimilarityAnalyser.getBuffer(chain, 0.5).getEnvelope();
		list = getCoordinateList(p2.getCoordinates());
		Assert.assertTrue("Coordinate (0,2) not in buffer.", list.contains(new Coordinate(0.0, 2.0)));
		Assert.assertTrue("Coordinate (3,2) not in buffer.", list.contains(new Coordinate(3.0, 2.0)));
		Assert.assertTrue("Coordinate (0,3) not in buffer.", list.contains(new Coordinate(0.0, 3.0)));
		Assert.assertTrue("Coordinate (3,3) not in buffer.", list.contains(new Coordinate(3.0, 3.0)));
	}
	
	
	@Test
	public void testGetOverlapPercentage(){
		DigicoreVehicle vehicle = setupVehicle();
		
		DigicoreChain chain1 = vehicle.getChains().get(0);
		DigicoreChain chain2 = vehicle.getChains().get(1);
		double buffer = 0.5;	

		Geometry rectangle1 = GeometricChainSimilarityAnalyser.getBuffer(chain1, buffer).getEnvelope();
		Geometry rectangle2 = GeometricChainSimilarityAnalyser.getBuffer(chain2, buffer).getEnvelope();
		double percentage = GeometricChainSimilarityAnalyser.getPercentageOfInterSectionToUnion(rectangle1, rectangle2);
		Assert.assertEquals("Wrong overlap area.", 1.0/5.0, percentage, MatsimTestUtils.EPSILON);
	}
	
	
	private List<Coordinate> getCoordinateList(Coordinate[] array){
		List<Coordinate> list = new ArrayList<Coordinate>();
		for(int i = 0; i < array.length; i++){
			list.add(array[i]);
		}
		return list;
	}
	
	
	/**
	 * Set up a vehicle with two chains:
	 * 
	 * 3  (0.5, 2.5)
	 *      ._______. a3 (2.5, 2.5)
	 * 2    | a2
	 *      |
	 * 1    |
	 *      . a1
	 * 0  (0.5, 0.5)
	 * 
	 *    0   1   2   3
	 * @return
	 */
	private DigicoreVehicle setupVehicle(){
		DigicoreVehicle vehicle = new DigicoreVehicle(Id.create("1", Vehicle.class));
		
		/* Set up activities. */
		DigicoreActivity a1 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a1.setCoord(new Coord(0.5, 0.5));
		DigicoreActivity a2 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a2.setCoord(new Coord(0.5, 2.5));
		DigicoreActivity a3 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a3.setCoord(new Coord(2.5, 2.5));
		
		/* Set up chains. */
		DigicoreChain c1 = new DigicoreChain();
		c1.add(a1);
		c1.add(a2);
		DigicoreChain c2 = new DigicoreChain();
		c2.add(a2);
		c2.add(a3);
		
		vehicle.getChains().add(c1);
		vehicle.getChains().add(c2);
		return vehicle;
	}

}
