/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerEventsTest.java
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

package org.matsim.controler;

import java.io.File;
import java.util.List;

import org.matsim.config.Config;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.io.IOUtils;

/**
 * @author dgrether
 */
public class ControlerEventsTest extends MatsimTestCase {

	private String configfile = null;
	private Config config = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.configfile = getInputDirectory() + "config.xml";
		this.config = loadConfig(this.configfile);
	}

	public void testEvents() {
		Controler controler = new Controler(this.config);
		ControlerEventsTestListener listener = new ControlerEventsTestListener();
		controler.addControlerListener(listener);
		controler.run();
		//test for startup events
		StartupEvent startup = listener.getStartupEvent();
		assertNotNull("No ControlerStartupEvent fired!", startup);
		assertEquals(controler, startup.getControler());
		//test for shutdown
		ShutdownEvent shutdown = listener.getShutdownEvent();
		assertNotNull("No ControlerShutdownEvent fired!", shutdown);
		assertEquals(controler, shutdown.getControler());
		//test for iterations
		//setup
		List<IterationStartsEvent> setupIt = listener.getIterationStartsEvents();
		assertEquals(1, setupIt.size());
		assertEquals(0, setupIt.get(0).getIteration());
		//shutdown
		List<IterationEndsEvent> finishIt = listener.getIterationEndsEvents();
		assertEquals(1, finishIt.size());
		assertEquals(0, finishIt.get(0).getIteration());

		// prepare remove test
		Gbl.reset();
		controler = new Controler(this.config);
		listener = new ControlerEventsTestListener();
		// we know from the code above, that "add" works
		controler.addControlerListener(listener);
		// now remove the listener
		controler.removeControlerListener(listener);

		// clear directory to run with same config again...
		String outPath = this.config.controler().getOutputDirectory();
		File outDir = new File(outPath);
		IOUtils.deleteDirectory(outDir);

		// now run
		controler.run();

		//test for startup events
		startup = listener.getStartupEvent();
		assertNull("ControlerStartupEvent fired!", startup);
		//test for shutdown
		shutdown = listener.getShutdownEvent();
		assertNull("ControlerShutdownEvent fired!", shutdown);
		//test for iterations
		//setup
		setupIt = listener.getIterationStartsEvents();
		assertEquals(0, setupIt.size());
		//shutdown
		finishIt = listener.getIterationEndsEvents();
		assertEquals(0, finishIt.size());
	}

}
