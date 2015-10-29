/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVisTest.java
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

/**
 * 
 */
package org.matsim.contrib.otfvis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * Simple test case to ensure the converting from eventsfile to .mvi-file
 * Needs somehow a bunch of memory - please use "-Xmx630m"!
 * 
 * @author yu
 * 
 */
public class OTFVisTest  {

	@Test
	public void testConvert() {
		String networkFilename = "test/scenarios/equil/network.xml";
		String eventsFilename = "test/scenarios/equil/events.xml";
		String mviFilename = "test/scenarios/equil/events.mvi";

		String[] args = {"-convert", eventsFilename, networkFilename, mviFilename, "300"};
		OTFVis.main(args);

		File f = new File(mviFilename);
		Assert.assertTrue("No mvi file written!", f.exists());
	}

	@Test
	public void testOTFVisSnapshotWriterOnQSim() {
		final Config config = ConfigUtils.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(2);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setMobsim("qsim");
		config.controler().setSnapshotFormat(Arrays.asList("otfvis"));
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setSnapshotPeriod(600);
		qSimConfigGroup.setSnapshotStyle( SnapshotStyle.equiDist ) ;;

		final Controler controler = new Controler(config);
		controler.addOverridingModule(new OTFVisModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controler().setCreateGraphs(false);
        controler.setDumpDataAtEnd(false);
		controler.run();

		Assert.assertTrue(new File(controler.getControlerIO().getIterationFilename(0, "otfvis.mvi")).exists());
		Assert.assertTrue(new File(controler.getControlerIO().getIterationFilename(1, "otfvis.mvi")).exists());
		Assert.assertTrue(new File(controler.getControlerIO().getIterationFilename(2, "otfvis.mvi")).exists());
	}

}
