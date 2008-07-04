/* *********************************************************************** *
 * project: org.matsim.*
 * Shape2ZoneLayerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.demandmodeling;

import java.io.IOException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;
import org.matsim.world.ZoneLayer;

/**
 * Creates a simple world with zones taken from an ESRI shape file. The created
 * zones only have a center coordinate defined, but no extent.
 *
 * @author mrieser
 */
public class ShapeFile2ZoneLayerTest extends MatsimTestCase {

	public void testShp2ZoneLayer() throws IOException {
		final World world = Gbl.createWorld();
		final ZoneLayer layer = (ZoneLayer) world.createLayer(new IdImpl("zones"), "zones");
		final String shpFileName = getInputDirectory() + "zones.shp";
		final String worldFileName = getOutputDirectory() + "zones.xml";
		final String referenceFileName = getInputDirectory() + "zones.xml";

		ShapeFile2ZoneLayer shp2zl = new ShapeFile2ZoneLayer();
		shp2zl.shp2ZoneLayer(shpFileName, layer);
		world.complete();
		new WorldWriter(world, worldFileName).write();
		
		assertEquals("Created world does not match reference file.", 
				CRCChecksum.getCRCFromFile(worldFileName), CRCChecksum.getCRCFromFile(referenceFileName));
	}
}
