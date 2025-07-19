/* *********************************************************************** *
 * project: org.matsim.*
 * ControllerListenerManagerImplTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.controler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

	/**
 * @author mrieser / senozon
 */
public class ControllerListenerManagerImplTest {

	 @Test
	 void testAddControllerListener_ClassHierarchy() {
		ControllerListenerManagerImpl m = new ControllerListenerManagerImpl();
		CountingControllerListener ccl = new CountingControllerListener();
		ExtendedControllerListener ecl = new ExtendedControllerListener();
		m.addControllerListener(ccl);
		m.addControllerListener(ecl);

		m.fireControllerStartupEvent();
		Assertions.assertEquals(1, ccl.nOfStartups);
		Assertions.assertEquals(0, ccl.nOfIterStarts);
		Assertions.assertEquals(1, ecl.nOfStartups);
		Assertions.assertEquals(0, ecl.nOfIterStarts);
		Assertions.assertEquals(0, ecl.nOfShutdowns);

		m.fireControllerIterationStartsEvent(0, false);
		Assertions.assertEquals(1, ccl.nOfStartups);
		Assertions.assertEquals(1, ccl.nOfIterStarts);
		Assertions.assertEquals(1, ecl.nOfStartups);
		Assertions.assertEquals(1, ecl.nOfIterStarts);
		Assertions.assertEquals(0, ecl.nOfShutdowns);

		m.fireControllerIterationStartsEvent(1, false);
		Assertions.assertEquals(1, ccl.nOfStartups);
		Assertions.assertEquals(2, ccl.nOfIterStarts);
		Assertions.assertEquals(1, ecl.nOfStartups);
		Assertions.assertEquals(2, ecl.nOfIterStarts);
		Assertions.assertEquals(0, ecl.nOfShutdowns);

		m.fireControllerShutdownEvent(false, 1);
		Assertions.assertEquals(1, ccl.nOfStartups);
		Assertions.assertEquals(2, ccl.nOfIterStarts);
		Assertions.assertEquals(1, ecl.nOfStartups);
		Assertions.assertEquals(2, ecl.nOfIterStarts);
		Assertions.assertEquals(1, ecl.nOfShutdowns);
	}

	 @Test
	 void testAddCoreControllerListener_ClassHierarchy() {
		ControllerListenerManagerImpl m = new ControllerListenerManagerImpl();
		CountingControllerListener ccl = new CountingControllerListener();
		ExtendedControllerListener ecl = new ExtendedControllerListener();
		m.addCoreControllerListener(ccl);
		m.addCoreControllerListener(ecl);

		m.fireControllerStartupEvent();
		Assertions.assertEquals(1, ccl.nOfStartups);
		Assertions.assertEquals(0, ccl.nOfIterStarts);
		Assertions.assertEquals(1, ecl.nOfStartups);
		Assertions.assertEquals(0, ecl.nOfIterStarts);
		Assertions.assertEquals(0, ecl.nOfShutdowns);

		m.fireControllerIterationStartsEvent(0, false);
		Assertions.assertEquals(1, ccl.nOfStartups);
		Assertions.assertEquals(1, ccl.nOfIterStarts);
		Assertions.assertEquals(1, ecl.nOfStartups);
		Assertions.assertEquals(1, ecl.nOfIterStarts);
		Assertions.assertEquals(0, ecl.nOfShutdowns);

		m.fireControllerIterationStartsEvent(1, false);
		Assertions.assertEquals(1, ccl.nOfStartups);
		Assertions.assertEquals(2, ccl.nOfIterStarts);
		Assertions.assertEquals(1, ecl.nOfStartups);
		Assertions.assertEquals(2, ecl.nOfIterStarts);
		Assertions.assertEquals(0, ecl.nOfShutdowns);

		m.fireControllerShutdownEvent(false, 1);
		Assertions.assertEquals(1, ccl.nOfStartups);
		Assertions.assertEquals(2, ccl.nOfIterStarts);
		Assertions.assertEquals(1, ecl.nOfStartups);
		Assertions.assertEquals(2, ecl.nOfIterStarts);
		Assertions.assertEquals(1, ecl.nOfShutdowns);
	}

	private static class CountingControllerListener implements StartupListener, IterationStartsListener {

		/*package*/ int nOfStartups = 0;
		/*package*/ int nOfIterStarts = 0;

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			this.nOfIterStarts++;
		}

		@Override
		public void notifyStartup(StartupEvent event) {
			this.nOfStartups++;
		}

	}

	private static class ExtendedControllerListener extends CountingControllerListener implements ShutdownListener, StartupListener {
		// StartupListener is explicitly implemented, even if not necessary, to test that it will not be called twice

		/*package*/ int nOfShutdowns = 0;

		@Override
		public void notifyShutdown(ShutdownEvent event) {
			this.nOfShutdowns++;
		}

	}

}
