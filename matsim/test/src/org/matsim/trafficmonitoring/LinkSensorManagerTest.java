/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSensorManagerTest.java
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

package org.matsim.trafficmonitoring;

import org.matsim.controler.Controler;
import org.matsim.controler.events.ControlerFinishIterationEvent;
import org.matsim.controler.events.ControlerSetupIterationEvent;
import org.matsim.controler.listener.ControlerFinishIterationListener;
import org.matsim.controler.listener.ControlerSetupIterationListener;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.LinkSensorManager;


/**
 * @author dgrether
 */
public class LinkSensorManagerTest extends MatsimTestCase implements ControlerFinishIterationListener, ControlerSetupIterationListener {

	private LinkSensorManager manager = null;

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		loadConfig(getInputDirectory() + "config.xml");
	}

	public void testSensorManagement() {
		Controler controler = new Controler();
		controler.setOverwriteFiles(true);
		controler.addControlerListener(this);
		this.manager = new LinkSensorManager();
		this.manager.addLinkSensor("1");
		controler.run(null);
	}

	public void notifyIterationFinished(final ControlerFinishIterationEvent event) {
		if (event.getIteration() > 0)  {
			assertEquals(100, this.manager.getLinkTraffic("1"));
		}
	}

	public void notifyIterationSetup(final ControlerSetupIterationEvent event) {
		if (event.getIteration() == 1) {
			QueueSimulation.getEvents().addHandler(this.manager);
		}
	}

}
