/* *********************************************************************** *
 * project: org.matsim.*
 * TrafficStateTest
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
package playground.vsp.energy.trafficstate;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;


/**
 * @author dgrether
 *
 */
public class TrafficStateTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void testTrafficState() {
//		File f = new File("file");
//		System.out.println(f.getAbsolutePath());
		
		String configfile = this.testUtils.getInputDirectory() + "config.xml";
//		System.err.println(configfile);
		File config = new File(configfile);
//		System.out.println(config.exists());
		
		Controler controler = new Controler(configfile);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.getConfig().controler().setWriteEventsInterval(0);
		controler.getConfig().controler().setWritePlansInterval(0);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.addControlerListener(new TrafficStateControlerListener());
		controler.run();
		
	}

}
