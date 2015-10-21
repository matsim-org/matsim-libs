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
package playground.jbischoff.taxibus.sim;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import playground.jbischoff.taxibus.TaxibusActionCreator;
import playground.jbischoff.taxibus.optimizer.DefaultTaxibusOptimizer;
import playground.jbischoff.taxibus.optimizer.TaxibusOptimizerConfiguration;
import playground.jbischoff.taxibus.optimizer.TaxibusOptimizerConfiguration.Goal;
import playground.jbischoff.taxibus.optimizer.filter.DefaultTaxibusFilterFactory;
import playground.jbischoff.taxibus.optimizer.filter.TaxibusFilterFactory;
import playground.jbischoff.taxibus.passenger.TaxibusPassengerEngine;
import playground.jbischoff.taxibus.passenger.TaxibusPassengerOrderManager;
import playground.jbischoff.taxibus.passenger.TaxibusRequestCreator;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;
import playground.jbischoff.taxibus.scheduler.TaxibusScheduler;
import playground.jbischoff.taxibus.scheduler.TaxibusSchedulerParams;
import playground.jbischoff.taxibus.utils.TaxibusUtils;
import playground.jbischoff.taxibus.vehreqpath.TaxibusVehicleRequestPathFinder;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author jbischoff
 *
 */

public class TaxibusQSimProvider implements Provider<QSim> {
	private TaxibusConfigGroup tbcg;
	private MatsimVrpContextImpl context;
	private DefaultTaxibusOptimizer optimizer;
	private EventsManager events;
	private TravelTime travelTime;

	@Inject
	TaxibusQSimProvider(Config config, MatsimVrpContext context , EventsManager events) {
		this.tbcg = (TaxibusConfigGroup) config.getModule("taxibusConfig");
		this.context = (MatsimVrpContextImpl) context;
		this.events=events;
		this.travelTime = new FreeSpeedTravelTime();
		

	}

	private QSim createMobsim(Scenario sc, EventsManager eventsManager) {
		initiate();
		QSim qSim = DynAgentLauncherUtils.initQSim(sc, eventsManager);
		qSim.addQueueSimulationListeners(optimizer);
		
		context.setMobsimTimer(qSim.getSimTimer());
		
		TaxibusPassengerEngine passengerEngine = new TaxibusPassengerEngine(TaxibusUtils.TAXIBUS_MODE, eventsManager, new TaxibusRequestCreator(), optimizer, context);
		qSim.addMobsimEngine(passengerEngine);
		qSim.addDepartureHandler(passengerEngine);
		TaxibusPassengerOrderManager orderManager = new TaxibusPassengerOrderManager(passengerEngine);
		qSim.addQueueSimulationListeners(orderManager);
		eventsManager.addHandler(orderManager);
		LegCreator legCreator = VrpLegs.createLegWithOfflineTrackerCreator(qSim
				.getSimTimer());
		TaxibusActionCreator actionCreator = new TaxibusActionCreator(
				passengerEngine, legCreator, tbcg.getPickupDuration());
		VrpLauncherUtils.initAgentSources(qSim, context, optimizer,
				actionCreator);
		return qSim;
	}

	void initiate() {
		//this initiation takes place upon creating qsim for each iteration
		TravelDisutility travelDisutility = new DistanceAsTravelDisutility();
		
		
		TaxibusSchedulerParams params = new TaxibusSchedulerParams(tbcg.getPickupDuration(), tbcg.getDropoffDuration());
		
		resetSchedules(context.getVrpData().getVehicles().values());

		TaxibusScheduler scheduler = new TaxibusScheduler(context, params);
		TaxibusFilterFactory filterFactory = new DefaultTaxibusFilterFactory(scheduler, tbcg.getNearestRequestsLimit(), tbcg.getNearestVehiclesLimit());

		TaxibusOptimizerConfiguration optimConfig = new TaxibusOptimizerConfiguration(
				context, travelTime, travelDisutility, scheduler, filterFactory,
				Goal.MIN_WAIT_TIME, tbcg.getOutputDir());
		optimizer = new DefaultTaxibusOptimizer(optimConfig,  false);

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
