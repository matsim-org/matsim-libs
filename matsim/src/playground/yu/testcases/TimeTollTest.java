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
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author ychen
 * 
 */
public class TimeTollTest extends MatsimTestCase {
	private static class TestControlerListener implements
			IterationEndsListener, IterationStartsListener {
		private CalcLegTimes clt = null;

		public void notifyIterationEnds(final IterationEndsEvent event) {
			if (event.getIteration() == event.getControler().getLastIteration()
					&& clt != null) {
				double traveling = Double.parseDouble(Gbl.getConfig().getParam(
						"planCalcScore", "traveling"));
				double criterion = 0.0;
				if (traveling == -6.0)
					criterion = 315.0;
				else if (traveling == -3.0)
					criterion = 314.0;
				assertEquals(criterion, clt.getAverageTripDuration());
			}
		}

		public void notifyIterationStarts(final IterationStartsEvent event) {
			if (event.getIteration() == event.getControler().getLastIteration()) {
				clt = new CalcLegTimes(event.getControler().getPopulation());
				event.getControler().getEvents().addHandler(clt);
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
