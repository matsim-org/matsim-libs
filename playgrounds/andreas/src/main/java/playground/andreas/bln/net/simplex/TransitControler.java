/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
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

package playground.andreas.bln.net.simplex;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.PtConstants;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.counts.OccupancyAnalyzer;
import org.matsim.pt.counts.PtCountControlerListener;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import java.util.EnumSet;
import java.util.Set;


/**
 * @author mrieser
 */
public final class TransitControler {

	private final static Logger logger = Logger.getLogger(TransitControler.class);

	private final static String COUNTS_MODULE_NAME = "ptCounts";

	private final TransitConfigGroup transitConfig;

	private boolean useOTFVis = true;

	public TransitControler(final String[] args) {
//		super(args);
		this.transitConfig = new TransitConfigGroup();
		init();
		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE 
				+ Gbl.RUN_MOB_SIM_NO_LONGER_POSSIBLE + Gbl.CONTROLER_IS_NOW_FINAL ) ;
	}

	public TransitControler(final ScenarioImpl scenario) {
//		super(scenario);
		this.transitConfig = new TransitConfigGroup();
		init();
		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE
				+ Gbl.RUN_MOB_SIM_NO_LONGER_POSSIBLE + Gbl.CONTROLER_IS_NOW_FINAL ) ;
	}

	private final void init() {
//		if (this.getConfig().getModule(TransitConfigGroup.GROUP_NAME) == null) {
//			this.getConfig().addModule(this.transitConfig);
//		} else {
//			// this would not be necessary if TransitConfigGroup is part of core config
//			ConfigGroup oldModule = this.getConfig().getModule(TransitConfigGroup.GROUP_NAME);
//			this.getConfig().removeModule(TransitConfigGroup.GROUP_NAME);
//			this.transitConfig.addParam("transitScheduleFile", oldModule.getValue("transitScheduleFile"));
//			this.transitConfig.addParam("vehiclesFile", oldModule.getValue("vehiclesFile"));
//			this.transitConfig.addParam("transitModes", oldModule.getValue("transitModes"));
//		}
//		this.getConfig().scenario().setUseTransit(true);
//		this.getConfig().scenario().setUseVehicles(true);
//		Set<EventsFileFormat> formats = EnumSet.copyOf(this.getConfig().controler().getEventsFileFormats());
//		formats.add(EventsFileFormat.xml);
//		this.getConfig().controler().setEventsFileFormats(formats);
//		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
//		transitActivityParams.setTypicalDuration(120.0);
//		this.getConfig().planCalcScore().addActivityParams(transitActivityParams);
//        ((PopulationFactoryImpl) getScenario().getPopulation().getFactory()).setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
//        
//        this.loadMyControlerListeners();
	}

	private void loadMyControlerListeners() {
////		super.loadControlerListeners();
//		addTransitControlerListener();
//		if (getConfig().getModule(COUNTS_MODULE_NAME) != null) {
//			addPtCountControlerListener();
//		}
	}

	private void addPtCountControlerListener() {
//		logger.info("Using counts.");
//
////		OccupancyAnalyzer occupancyAnalyzer = new OccupancyAnalyzer(3600, 24 * 3600 - 1);
////		addControlerListener(new OccupancyAnalyzerListener(occupancyAnalyzer));
////		addControlerListener(new PtCountControlerListener(config, occupancyAnalyzer));
//		addControlerListener(new PtCountControlerListener(getConfig()) );
//		// the PtCountControlerListener now has its own OccupancyAnalyzer.  kai, oct'10
//
//        this.getConfig().controler().setCreateGraphs(false);
    }

	private void addTransitControlerListener() {
//		TransitControlerListener cl = new TransitControlerListener(this.transitConfig);
//		addControlerListener(cl);
	}

//	@Override
//	protected void runMobSim() {
//		QSim sim = (QSim) QSimUtils.createDefaultQSim(this.getScenario(), this.getEvents());
//		if (useOTFVis) {
//			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(this.getScenario().getConfig(), this.getScenario(), getEvents(), sim);
//			OTFClientLive.run(this.getScenario().getConfig(), server);
//		}
//
////		this.events.addHandler(new LogOutputEventHandler());
//
//		sim.run();
//	}

//	@Override
//	public PlanAlgorithm createRoutingAlgorithm() {
//		return createRoutingAlgorithm(
//				this.createTravelCostCalculator(),
//				this.getLinkTravelTimes());
//	}

	public static class TransitControlerListener implements StartupListener {

		private final TransitConfigGroup config;

		public TransitControlerListener(final TransitConfigGroup config) {
			this.config = config;
		}

		@Override
		public void notifyStartup(final StartupEvent event) {
			if (this.config.getTransitScheduleFile() != null) {
				new TransitScheduleReaderV1(event.getControler().getScenario().getTransitSchedule(), event.getControler().getScenario().getNetwork()).readFile(this.config.getTransitScheduleFile());
			}
			if (this.config.getVehiclesFile() != null) {
				new VehicleReaderV1(((ScenarioImpl) event.getControler().getScenario()).getTransitVehicles()).parse(this.config.getVehiclesFile());
			}
			ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(
					event.getControler().getScenario().getNetwork(), event
							.getControler().getScenario()
							.getTransitSchedule().getTransitLines().values(),
					((ScenarioImpl) event.getControler().getScenario()).getTransitVehicles(),
					event.getControler().getScenario().getConfig().planCalcScore());
			reconstructingUmlaufBuilder.build();
		}

	}

	public static void main(final String[] args) {
		TransitControler tc = new TransitControler(args);
//		tc.setOverwriteFiles(true);
////		tc.setCreateGraphs(false);
//		tc.run();
	}

	public static class OccupancyAnalyzerListener implements
			BeforeMobsimListener, AfterMobsimListener {

		private OccupancyAnalyzer occupancyAnalyzer;

		public OccupancyAnalyzerListener(OccupancyAnalyzer occupancyAnalyzer) {
			this.occupancyAnalyzer = occupancyAnalyzer;
		}

		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			int iter = event.getIteration();
			if (iter % 10 == 0&& iter > event.getControler().getConfig().controler().getFirstIteration()) {
				occupancyAnalyzer.reset(iter);
				event.getControler().getEvents().addHandler(occupancyAnalyzer);
			}
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			int it = event.getIteration();
			if (it % 10 == 0 && it > event.getControler().getConfig().controler().getFirstIteration()) {
				event.getControler().getEvents().removeHandler(occupancyAnalyzer);
				occupancyAnalyzer.write(event.getControler().getControlerIO()
						.getIterationFilename(it, "occupancyAnalysis.txt"));
			}
		}

	}

	boolean isUseOTFVis() {
		return useOTFVis;
	}

	protected void setUseOTFVis(boolean useOTFVis) {
		this.useOTFVis = useOTFVis;
	}


}
