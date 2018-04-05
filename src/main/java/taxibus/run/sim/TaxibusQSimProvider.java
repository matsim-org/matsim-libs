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
package taxibus.run.sim;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.router.util.*;

import com.google.inject.*;
import com.google.inject.name.Named;

import taxibus.TaxibusActionCreator;
import taxibus.algorithm.optimizer.*;
import taxibus.algorithm.optimizer.prebooked.PrebookedTaxibusOptimizerContext;
import taxibus.algorithm.optimizer.prebooked.clustered.*;
import taxibus.algorithm.optimizer.prebooked.jsprit.JspritTaxibusOptimizer;
import taxibus.algorithm.passenger.*;
import taxibus.algorithm.scheduler.*;
import taxibus.algorithm.utils.TaxibusUtils;
import taxibus.run.configuration.TaxibusConfigGroup;

/**
 * @author jbischoff
 */

public class TaxibusQSimProvider implements Provider<QSim> {
	private final Scenario scenario;
	private final EventsManager events;
	private final Collection<AbstractQSimPlugin> plugins;
	private final Fleet fleetData;
	private final TravelTime travelTime;
	private final TaxibusConfigGroup tbcg;
	private final OrderManager orderManager;

	@Inject
	TaxibusQSimProvider(Scenario scenario, EventsManager events, Collection<AbstractQSimPlugin> plugins, Fleet vrpData,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, TaxibusConfigGroup tbcg,
			OrderManager orderManager) {
		this.scenario = scenario;
		this.events = events;
		this.plugins = plugins;
		this.fleetData = vrpData;
		this.travelTime = travelTime;
		this.tbcg = tbcg;
		this.orderManager = orderManager;
	}

	@Override
	public QSim get() {
		QSim qSim = QSimUtils.createQSim(scenario, events, plugins);

		TaxibusOptimizer optimizer = createTaxibusOptimizer(qSim);
		qSim.addQueueSimulationListeners(optimizer);

		TaxibusPassengerEngine passengerEngine = new TaxibusPassengerEngine(TaxibusUtils.TAXIBUS_MODE, events,
				new TaxibusRequestCreator(), optimizer, scenario.getNetwork());
		qSim.addMobsimEngine(passengerEngine);
		qSim.addDepartureHandler(passengerEngine);
		
		if (orderManager != null) {
			orderManager.setPassengerEngine(passengerEngine);
			qSim.addQueueSimulationListeners((MobsimListener) orderManager);
		}

		VrpLegFactory legFactory = vehicle -> VrpLegFactory.createWithOfflineTracker(vehicle, qSim.getSimTimer());
		TaxibusActionCreator actionCreator = new TaxibusActionCreator(passengerEngine, legFactory,
				tbcg.getPickupDuration());
		qSim.addAgentSource(new VrpAgentSource(actionCreator, fleetData, optimizer, qSim));

		return qSim;
	}

	private TaxibusOptimizer createTaxibusOptimizer(QSim qSim) {
		// Joschka, now you can safely use travel times for disutility - these are from the past
		// (not the currently collected)
		TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
		TaxibusSchedulerParams params = new TaxibusSchedulerParams(tbcg.getPickupDuration(), tbcg.getDropoffDuration());
		TaxibusScheduler scheduler = new TaxibusScheduler(fleetData, qSim.getSimTimer(), params);

		switch (tbcg.getAlgorithm()) {
			case "jsprit": {
				PrebookedTaxibusOptimizerContext context = new PrebookedTaxibusOptimizerContext(fleetData, scenario,
						qSim.getSimTimer(), travelTime, travelDisutility, scheduler, tbcg);
				return new JspritTaxibusOptimizer(context);
			}

			case "clustered_jsprit": {
				PrebookedTaxibusOptimizerContext context = new PrebookedTaxibusOptimizerContext(fleetData, scenario,
						qSim.getSimTimer(), travelTime, travelDisutility, scheduler, tbcg);
				return new ClusteringTaxibusOptimizer(context, new JspritDispatchCreator(context));
			}
			default:
				throw new RuntimeException("No config parameter set for algorithm, please check and assign in config");
		}
	}
}
