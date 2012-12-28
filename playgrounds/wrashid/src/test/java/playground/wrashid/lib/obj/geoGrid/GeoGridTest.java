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

import org.matsim.core.utils.geometry.CoordImpl;

public class GeoGridTest extends TestCase{

	public void testSingleDataItem(){
		GeoGrid geoGrid=new GeoGrid(10);
		CoordImpl sampleCoord = new CoordImpl(55,55);
		GridDataItem gridDataItem=new GridDataItem(5.0, 1.0, sampleCoord);
		geoGrid.addGridInformation(gridDataItem);
		geoGrid.markDataCollectionPhaseAsFishished();
		
		assertEquals(5.0, geoGrid.getValue(sampleCoord));
		assertEquals(5.0, geoGrid.getValue(new CoordImpl(50.0,50.0)));
		assertEquals(5.0, geoGrid.getValue(new CoordImpl(100.0,100.0)));
	}
	
	public void testTwoDataItemsAndWeights(){
		GeoGrid geoGrid=new GeoGrid(10);
		CoordImpl firstCoord = new CoordImpl(55,55);
		CoordImpl secondCoord = new CoordImpl(56,56);
		geoGrid.addGridInformation(new GridDataItem(2.0, 1.0, firstCoord));
		geoGrid.addGridInformation(new GridDataItem(10.0, 3.0, secondCoord));
		geoGrid.markDataCollectionPhaseAsFishished();
		
		assertEquals(8.0, geoGrid.getValue(firstCoord));
		assertEquals(8.0, geoGrid.getValue(secondCoord));
	}
	
	
}
