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
package playground.yu.timeToll;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author ychen
 *
 */
public class TimeTollTest extends MatsimTestCase {
	private static class TestControlerListener implements
			IterationEndsListener, IterationStartsListener {
		private CalcLegTimes clt = null;

		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {
			if (event.getIteration() == event.getControler().getConfig().controler().getLastIteration()
					&& clt != null) {
				double traveling = event.getControler().getConfig().planCalcScore().getTraveling_utils_hr();
				double criterion = 0;
				if (traveling == -30.0) {
					criterion = 1710.0;
					assertEquals(clt.getAverageTripDuration() < criterion, true);
				} else if (traveling == -6.0) {
					criterion = 1720.0;
//					System.out.println("avg. trip Duration:\t"
//							+ clt.getAverageTripDuration());
//					System.exit(0);
					assertEquals(clt.getAverageTripDuration() > criterion, true);
				}
			}
		}

		@Override
		public void notifyIterationStarts(final IterationStartsEvent event) {
			if (event.getIteration() == event.getControler().getConfig().controler().getLastIteration()) {
				clt = new CalcLegTimes();
				event.getControler().getEvents().addHandler(clt);
			}
		}

	}

	public void testBetaTraveling_6() {
		Config config = super.loadConfig(getInputDirectory() + "config.xml");
		Controler ctl = new Controler(config);
		ctl.addControlerListener(new TestControlerListener());
        ctl.getConfig().controler().setCreateGraphs(false);
		ctl.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		ctl.run();
	}

	public void testBetaTraveling_30() {
		Config config = super.loadConfig(getInputDirectory() + "config.xml");
		Controler ctl = new Controler(config);
		ctl.addControlerListener(new TestControlerListener());
        ctl.getConfig().controler().setCreateGraphs(false);
		ctl.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		ctl.run();
	}
}
