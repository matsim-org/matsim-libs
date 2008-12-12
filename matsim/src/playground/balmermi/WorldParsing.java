/* *********************************************************************** *
 * project: org.matsim.*
 * WorldParsing.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi;

import org.matsim.config.ConfigWriter;
import org.matsim.gbl.Gbl;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.world.algorithms.WorldMappingInfo;

public class WorldParsing {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void testRun01() {

		System.out.println("TEST RUN 01:");

		System.out.println("  reading world xml file... ");
		World world = Gbl.getWorld();
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println();
		System.out.println("1. VALIDATE AND COMPLETE THE WORLD");
		System.out.println();

		System.out.println("  running world algorithms... ");
		new WorldMappingInfo().run(world);
		new WorldBottom2TopCompletion().run(world);
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(world);
		world_writer.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
		config_writer.write();
		System.out.println("  done.");

		System.out.println("TEST SUCCEEDED.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {

		Gbl.startMeasurement();

		Gbl.createConfig(args);
		Gbl.createWorld();

		testRun01();

		Gbl.printElapsedTime();
	}
}
