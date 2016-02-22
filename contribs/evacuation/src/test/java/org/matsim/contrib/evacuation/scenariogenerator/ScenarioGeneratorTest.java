/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioGeneratorTest.java
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

package org.matsim.contrib.evacuation.scenariogenerator;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evacuation.analysis.control.EventReaderThread;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.io.ConfigIO;
import org.matsim.contrib.evacuation.model.config.EvacuationConfigModule;
import org.matsim.contrib.evacuation.utils.NetworksComparator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ScenarioGeneratorTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public void testScenarioGenerator() {
		
		ArrayList<Id<Link>> closedRoadIDs = new ArrayList<>();
		closedRoadIDs.add(Id.create(156, Link.class));
		closedRoadIDs.add(Id.create(316, Link.class));
		closedRoadIDs.add(Id.create(263, Link.class));
		
		String inputDir = testUtils.getInputDirectory();
		String outputDir = testUtils.getOutputDirectory();
		
		String gripsFileString = inputDir + "/grips_config.xml";
		String matsimConfigFileString = outputDir + "/config.xml";
		
		System.out.println("grips file:" + gripsFileString);
		System.out.println("matsim config file:" + matsimConfigFileString);
		
//		File file = new File("oloberg.jpg");
//		File file2 = new File("C:/HTW_Logo_rgb.jpg");
//		File file3 = new File("/lol/HTW_Logo_rgb.jpg");
//		File file4 = new File("./test/input/org/matsim/contrib/grips/scenariogenerator/ScenarioGeneratorTest/testScenarioGenerator/lenzen.osm");
//		
//		System.out.println("file1 absolute path:" + file.isAbsolute());
//		System.out.println("file2 absolute path:" + file2.isAbsolute());
//		System.out.println("file3 absolute path:" + file3.isAbsolute());
//		
//		System.out.println(":" + file4.isAbsolute());
//		System.out.println("file4 absolute path:" + file4.exists());
		
//		System.exit(0);
		
		File gripsConfigFile = new File(gripsFileString);
		File matsimConfigFile;
		
		Controller controller = new Controller();
		EvacuationConfigModule gcm;
		Config mc;
		
		//check for files
		Assert.assertTrue("grips config file is missing", gripsConfigFile.exists());
		Assert.assertTrue("evacuation area shape file is missing", (new File(inputDir + "/evacuation_area.shp")).exists());
		Assert.assertTrue("population area shape file is missing", (new File(inputDir + "/population.shp")).exists());
		Assert.assertTrue("open street map file is missing", (new File(inputDir + "/lenzen.osm")).exists());
		Assert.assertTrue("could not open grips config.", controller.evacuationEvacuationConfig(gripsConfigFile));
		
		gcm = controller.getEvacuationConfigModule();
		
		//generate and check matsim network/config
		boolean generateScenario = true;
		try {
			ScenarioGenerator scengen = new org.matsim.contrib.evacuation.scenariogenerator.ScenarioGenerator(gripsFileString);
			scengen.run();
		} catch (Exception e) {
			generateScenario = false;
			e.printStackTrace();
		}
		Assert.assertTrue("scenario was not generated", generateScenario);
		
		//check and open matsim scenario config file
		System.out.println("string:" + matsimConfigFileString);
		matsimConfigFile = new File(matsimConfigFileString);
		Assert.assertTrue("scenario config file is missing", matsimConfigFile.exists());
		Assert.assertTrue("could not open matsim config", controller.openMastimConfig(matsimConfigFile));
		
		//open matsim config, set first and last iteration
		mc = controller.getScenario().getConfig();
		mc.setParam("controler", "firstIteration", "0");
		mc.setParam("controler", "lastIteration", "10");
		new ConfigWriter(mc).write(matsimConfigFileString);
		
		//save road closures
		HashMap<Id<Link>, String> roadClosures = new HashMap<Id<Link>, String>();
		for (Id<Link> id : closedRoadIDs)
			roadClosures.put(id, "00:00");
		boolean saved = ConfigIO.saveRoadClosures(controller, roadClosures);
		Assert.assertTrue("could not save road closures", saved);
		
		//simulate and check scenario
		ConfigUtils.addOrGetModule(mc, EvacuationConfigModule.GROUP_NAME, EvacuationConfigModule.class);
		Controler matsimController = new Controler(mc);
		matsimController.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		matsimController.run();

		//parse events, check if closed roads are not being visited
		LinkEnterEventHandler eventHandler = null;
		EventsManager e = EventsUtils.createEventsManager();
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(e);
		Thread readerThread = new Thread(new EventReaderThread(reader, outputDir+"output/ITERS/it.10/10.events.xml.gz"), "readerthread");
		final ArrayList<Id<Link>> usedIDs = new ArrayList<Id<Link>>();
		eventHandler = new LinkEnterEventHandler() {
			@Override
			public void reset(int iteration) {
			}
			@Override
			public void handleEvent(LinkEnterEvent event) {
				if (usedIDs.contains(event.getLinkId()))
					usedIDs.add(event.getLinkId());
			}
		};
		e.addHandler(eventHandler);
		readerThread.run();
		
		for (Id<Link> id : closedRoadIDs)
			Assert.assertTrue("a closed road is crossed (id: " + id.toString() + ")", !usedIDs.contains(id));
		
//		assertEquals("different config-files.", CRCChecksum.getCRCFromFile(inputDir + "/config.xml"), CRCChecksum.getCRCFromFile(outputDir + "/config.xml"));
//		Assert.assertEquals("different network-files.", CRCChecksum.getCRCFromFile(inputDir + "/network.xml.gz"), CRCChecksum.getCRCFromFile(outputDir + "/network.xml.gz"));
		boolean equalNets = new NetworksComparator().compare(inputDir + "/network.xml.gz", outputDir + "/network.xml.gz");
		Assert.assertTrue("differnt network-files", equalNets);
//		assertEquals("different plans-files.", CRCChecksum.getCRCFromFile(inputDir + "/population.xml.gz"), CRCChecksum.getCRCFromFile(outputDir + "/population.xml.gz"));
//		EventsFileComparator
	}

}
