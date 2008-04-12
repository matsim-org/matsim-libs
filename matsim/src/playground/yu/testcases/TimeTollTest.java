/* *********************************************************************** *
 * project: org.matsim.*
 * TimeTollTest.java
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

/**
 * 
 */
package playground.yu.testcases;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author ychen
 * 
 */
public class TimeTollTest extends MatsimTestCase {
	private static class TestControlerListener implements
			IterationEndsListener, IterationStartsListener {

		public void notifyIterationEnds(IterationEndsEvent event) {

		}

		public void notifyIterationStarts(IterationStartsEvent event) {
			if (event.getIteration() == event.getControler().getLastIteration()) {
				CalcLegTimes clt = new CalcLegTimes(event.getControler().getPopulation());
				event.getControler().getEvents().addHandler(null);
			}
		}

	}

	public void testBetaTraveling_6() {
		Controler ctl = new Controler(getInputDirectory() + "config.xml");
		ctl.addControlerListener(new TestControlerListener());
		ctl.setCreateGraphs(false);
		ctl.run();
	}
}
