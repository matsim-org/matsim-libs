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

import java.util.EnumSet;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;

import playground.mrieser.pt.config.TransitConfigGroup;
import playground.mzilske.bvg09.TransitControler;
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

	private final TransitConfigGroup transitConfig;

	public TrCtl(String[] args) {
		super(args);
		this.transitConfig = new TransitConfigGroup();
		init();
	}

	private final void init() {
		if (this.config.getModule(TransitConfigGroup.GROUP_NAME) == null) {
			this.config.addModule(TransitConfigGroup.GROUP_NAME,
					this.transitConfig);
		} else {
			// this would not be necessary if TransitConfigGroup is part of core
			// config
			Module oldModule = this.config
					.getModule(TransitConfigGroup.GROUP_NAME);
			this.config.removeModule(TransitConfigGroup.GROUP_NAME);
			this.transitConfig.addParam("transitScheduleFile", oldModule
					.getValue("transitScheduleFile"));
			this.transitConfig.addParam("vehiclesFile", oldModule
					.getValue("vehiclesFile"));
			this.transitConfig.addParam("transitModes", oldModule
					.getValue("transitModes"));
		}
		this.config.scenario().setUseTransit(true);
		this.config.scenario().setUseVehicles(true);
		Set<EventsFileFormat> formats = EnumSet.copyOf(this.config.controler()
				.getEventsFileFormats());
		formats.add(EventsFileFormat.xml);
		this.config.controler().setEventsFileFormats(formats);

		ActivityParams params = new ActivityParams(
				PtConstants.TRANSIT_ACTIVITY_TYPE);
		params.setTypicalDuration(120.0);
		this.config.charyparNagelScoring().addActivityParams(params);

		this.getNetwork().getFactory().setRouteFactory(TransportMode.pt,
				new ExperimentalTransitRouteFactory());

		TransitControlerListener cl = new TransitControlerListener(
				this.transitConfig);
		addControlerListener(cl);
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
