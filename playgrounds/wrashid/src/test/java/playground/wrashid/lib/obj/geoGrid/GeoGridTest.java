/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.lib.obj.geoGrid;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;

public class GeoGridTest extends TestCase{

	public void testSingleDataItem(){
		GeoGrid geoGrid=new GeoGrid(10);
		Coord sampleCoord = new Coord((double) 55, (double) 55);
		GridDataItem gridDataItem=new GridDataItem(5.0, 1.0, sampleCoord);
		geoGrid.addGridInformation(gridDataItem);
		geoGrid.markDataCollectionPhaseAsFishished();
		
		assertEquals(5.0, geoGrid.getValue(sampleCoord));
		assertEquals(5.0, geoGrid.getValue(new Coord(50.0, 50.0)));
		assertEquals(5.0, geoGrid.getValue(new Coord(100.0, 100.0)));
	}
	
	public void testTwoDataItemsAndWeights(){
		GeoGrid geoGrid=new GeoGrid(10);
		Coord firstCoord = new Coord((double) 55, (double) 55);
		Coord secondCoord = new Coord((double) 56, (double) 56);
		geoGrid.addGridInformation(new GridDataItem(2.0, 1.0, firstCoord));
		geoGrid.addGridInformation(new GridDataItem(10.0, 3.0, secondCoord));
		geoGrid.markDataCollectionPhaseAsFishished();
		
		assertEquals(8.0, geoGrid.getValue(firstCoord));
		assertEquals(8.0, geoGrid.getValue(secondCoord));
	}
	
	
}
