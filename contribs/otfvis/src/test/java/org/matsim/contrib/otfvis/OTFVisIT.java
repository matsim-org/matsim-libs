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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.Collections;

import static org.matsim.core.config.groups.ControllerConfigGroup.*;

/**
 * Simple test case to ensure the converting from eventsfile to .mvi-file
 * Needs somehow a bunch of memory - please use "-Xmx630m"!
 *
 * @author yu
 *
 */
public class OTFVisIT {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testConvert() {
		String networkFilename = "test/scenarios/equil/network.xml";
		String eventsFilename = "test/scenarios/equil/events.xml";
		String mviFilename = testUtils.getOutputDirectory()+"/events.mvi";

		String[] args = {"-convert", eventsFilename, networkFilename, mviFilename, "300"};
		OTFVis.main(args);

		File f = new File(mviFilename);
		Assertions.assertTrue(f.exists(), "No mvi file written!");
	}

	@Test
	void testOTFVisSnapshotWriterOnQSim() {
		final Config config = ConfigUtils.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controller().setLastIteration(2);
		config.controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);
		config.controller().setMobsim("qsim");
		config.controller().setSnapshotFormat( Collections.singletonList( SnapshotFormat.otfvis ) );
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setSnapshotPeriod(600);
		qSimConfigGroup.setSnapshotStyle( SnapshotStyle.equiDist ) ;;

		final Controler controler = new Controler(config);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.run();

		Assertions.assertTrue(new File(controler.getControlerIO().getIterationFilename(0, "otfvis.mvi")).exists());
		Assertions.assertTrue(new File(controler.getControlerIO().getIterationFilename(1, "otfvis.mvi")).exists());
		Assertions.assertTrue(new File(controler.getControlerIO().getIterationFilename(2, "otfvis.mvi")).exists());
	}

}
