/* *********************************************************************** *
 * project: org.matsim.*
 * TrCtl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.yu.run;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.mrieser.pt.controler.TransitControler;
import playground.yu.analysis.pt.OccupancyAnalyzer;
import playground.yu.counts.pt.PtBoardCountControlerListener;

/**
 * @author yu
 * 
 */
public class TrCtl extends TransitControler {
	public static class OccupancyAnalyzerListener implements StartupListener,
			BeforeMobsimListener, AfterMobsimListener {
		private OccupancyAnalyzer oa = null;

		public OccupancyAnalyzer getOccupancyAnalysis() {
			return oa;
		}

		public void notifyStartup(StartupEvent event) {
			this.oa = new OccupancyAnalyzer(3600, 24 * 3600 - 1);
		}

		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			if (event.getIteration() % 10 == 0) {
				this.oa.reset(event.getIteration());
				event.getControler().getEvents().addHandler(oa);
			}
		}

		public void notifyAfterMobsim(AfterMobsimEvent event) {
			int it = event.getIteration();
			if (it % 10 == 0 && it > event.getControler().getFirstIteration()) {
				// TODO transfer oa 2 countscompare
				event.getControler().getEvents().removeHandler(oa);
				this.oa.write(event.getControler().getNameForIterationFilename(
						"occupancyAnalysis.txt"));
			}
		}

	}

	public TrCtl(String[] args) {
		super(args);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TrCtl ctl = new TrCtl(args);
		OccupancyAnalyzerListener oal = new OccupancyAnalyzerListener();
		ctl.addControlerListener(oal);
		ctl.addControlerListener(new PtBoardCountControlerListener(ctl.config,
				oal.getOccupancyAnalysis()));
		ctl.setOverwriteFiles(true);
		ctl.setCreateGraphs(false);
		ctl.run();
	}

}
