/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
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
package playground.jbischoff.taxibus.run.sim;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.router.DistanceAsTravelDisutility;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vis.otfvis.OTFClientLive;

import com.google.inject.Inject;
import com.google.inject.Provider;

import playground.jbischoff.taxibus.algorithm.TaxibusActionCreator;
import playground.jbischoff.taxibus.algorithm.optimizer.TaxibusOptimizer;
import playground.jbischoff.taxibus.algorithm.optimizer.TaxibusOptimizerConfiguration;
import playground.jbischoff.taxibus.algorithm.optimizer.fifo.FifoOptimizer;
import playground.jbischoff.taxibus.algorithm.optimizer.fifo.MultipleFifoOptimizer;
import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.LineDispatcher;
import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.LinesUtils;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusPassengerEngine;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusPassengerOrderManager;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequestCreator;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusScheduler;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusSchedulerParams;
import playground.jbischoff.taxibus.algorithm.utils.TaxibusUtils;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;

/**
 * @author jbischoff
 *
 */

public class TaxibusQSimProvider implements Provider<QSim> {
	private TaxibusConfigGroup tbcg;
	private MatsimVrpContextImpl context;
	private TaxibusOptimizer optimizer;
	private EventsManager events;
	private TravelTime travelTime;
	private LineDispatcher dispatcher;
	private 	TaxibusPassengerEngine passengerEngine;
	private TaxibusPassengerOrderManager orderManager ;
	@Inject
	TaxibusQSimProvider(Config config, MatsimVrpContext context , EventsManager events, Map<String,TravelTime> travelTimes, LineDispatcher dispatcher) {
		this.dispatcher = dispatcher;
		this.tbcg = (TaxibusConfigGroup) config.getModule("taxibusConfig");
		this.context = (MatsimVrpContextImpl) context;
		this.events=events;
		this.travelTime = travelTimes.get("car");
		passengerEngine = new TaxibusPassengerEngine(TaxibusUtils.TAXIBUS_MODE, events, new TaxibusRequestCreator(), optimizer, context);
		orderManager = new TaxibusPassengerOrderManager(passengerEngine);
		events.addHandler(orderManager);

	}

	private QSim createMobsim(Scenario sc, EventsManager eventsManager) {
		initiate();
		QSim qSim = DynAgentLauncherUtils.initQSim(sc, eventsManager);
		qSim.addQueueSimulationListeners(optimizer);
		
		context.setMobsimTimer(qSim.getSimTimer());
		
		qSim.addMobsimEngine(passengerEngine);
		qSim.addDepartureHandler(passengerEngine);
		qSim.addQueueSimulationListeners(orderManager);

		LegCreator legCreator = VrpLegs.createLegWithOfflineTrackerCreator(qSim
				.getSimTimer());
		TaxibusActionCreator actionCreator = new TaxibusActionCreator(
				passengerEngine, legCreator, tbcg.getPickupDuration());
		VrpLauncherUtils.initAgentSources(qSim, context, optimizer,
				actionCreator);
		if (tbcg.isOtfvis()){
		OTFClientLive.run(sc.getConfig(), OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim));
		}
		return qSim;
	}

	void initiate() {
		//this initiation takes place upon creating qsim for each iteration
		TravelDisutility travelDisutility = new DistanceAsTravelDisutility();
		
		
		TaxibusSchedulerParams params = new TaxibusSchedulerParams(tbcg.getPickupDuration(), tbcg.getDropoffDuration());
		
		resetSchedules(context.getVrpData().getVehicles().values());

		TaxibusScheduler scheduler = new TaxibusScheduler(context, params);
	
		
		TaxibusOptimizerConfiguration optimConfig = new TaxibusOptimizerConfiguration(
				context, travelTime, travelDisutility, scheduler, tbcg.getOutputDir());

	 if (tbcg.getAlgorithmConfig().equals("line")){
		
		optimizer = new FifoOptimizer(optimConfig, dispatcher, false);
		
		}
		else if (tbcg.getAlgorithmConfig().equals("multipleLine")){
			optimizer = new MultipleFifoOptimizer(optimConfig, dispatcher, false);
			
			}
		else throw new RuntimeException("No config parameter set for algorithm, please check and assign in config");

	}
	
	private void resetSchedules(Iterable<Vehicle> vehicles) {

    	for (Vehicle v : vehicles){
    		VehicleImpl vi = (VehicleImpl) v;
    		vi.resetSchedule();
    		
    	}
	}

	@Override
	public QSim get() {
		return createMobsim(context.getScenario(), this.events);
	}

}
