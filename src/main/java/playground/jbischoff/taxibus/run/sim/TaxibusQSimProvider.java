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

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.drt.DrtActionCreator;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.router.util.*;

import com.beust.jcommander.internal.Nullable;
import com.google.inject.*;
import com.google.inject.name.Named;

import playground.jbischoff.taxibus.algorithm.optimizer.TaxibusOptimizer;
import playground.jbischoff.taxibus.algorithm.optimizer.TaxibusOptimizerContext;
import playground.jbischoff.taxibus.algorithm.optimizer.clustered.ClusteringTaxibusOptimizer;
import playground.jbischoff.taxibus.algorithm.optimizer.clustered.ClusteringTaxibusOptimizerContext;
import playground.jbischoff.taxibus.algorithm.optimizer.clustered.JspritDispatchCreator;
import playground.jbischoff.taxibus.algorithm.optimizer.clustered.JspritTaxibusOptimizer;
import playground.jbischoff.taxibus.algorithm.optimizer.clustered.SimpleDispatchCreator;

import playground.jbischoff.taxibus.algorithm.optimizer.sharedTaxi.SharedTaxiOptimizer;
import playground.jbischoff.taxibus.algorithm.passenger.*;
import playground.jbischoff.taxibus.algorithm.scheduler.*;
import playground.jbischoff.taxibus.algorithm.tubs.StatebasedOptimizer;
import playground.jbischoff.taxibus.algorithm.tubs.datastructure.StateSpace;
import playground.jbischoff.taxibus.algorithm.utils.TaxibusUtils;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;


/**
 * @author jbischoff
 */

public class TaxibusQSimProvider
    implements Provider<QSim>
{
    private final Scenario scenario;
    private final EventsManager events;
    private final Collection<AbstractQSimPlugin> plugins;
    private final VrpData vrpData;
    private final TravelTime travelTime;
    private final TaxibusConfigGroup tbcg;
    private final TaxibusPassengerOrderManager orderManager;
    private final StateSpace stateSpace;

    @Inject
    TaxibusQSimProvider(Scenario scenario, EventsManager events,
            Collection<AbstractQSimPlugin> plugins, VrpData vrpData,
            @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime, TaxibusConfigGroup tbcg,
            @Nullable TaxibusPassengerOrderManager orderManager, @Nullable StateSpace stateSpace)
    {
        this.scenario = scenario;
        this.events = events;
        this.plugins = plugins;
        this.vrpData = vrpData;
        this.travelTime = travelTime;
        this.tbcg = tbcg;
        this.orderManager = orderManager;
        this.stateSpace = stateSpace;
    }


    @Override
    public QSim get()
    {
        QSim qSim = QSimUtils.createQSim(scenario, events, plugins);

        TaxibusOptimizer optimizer = createTaxibusOptimizer(qSim);
        qSim.addQueueSimulationListeners(optimizer);

        TaxibusPassengerEngine passengerEngine = new TaxibusPassengerEngine(
                TaxibusUtils.TAXIBUS_MODE, events, new TaxibusRequestCreator(), optimizer, vrpData,
                scenario.getNetwork());
        qSim.addMobsimEngine(passengerEngine);
        qSim.addDepartureHandler(passengerEngine);
        if (orderManager!=null){
        orderManager.setPassengerEngine(passengerEngine);
        qSim.addQueueSimulationListeners(orderManager);
        }
        
        LegCreator legCreator = VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        DrtActionCreator actionCreator = new DrtActionCreator(passengerEngine, legCreator,
                tbcg.getPickupDuration());
        qSim.addAgentSource(new VrpAgentSource(actionCreator, vrpData, optimizer, qSim));

        return qSim;
    }


    private TaxibusOptimizer createTaxibusOptimizer(QSim qSim)
    {
        //Joschka, now you can safely use travel times for disutility - these are from the past
        //(not the currently collected)
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
        TaxibusSchedulerParams params = new TaxibusSchedulerParams(tbcg.getPickupDuration(),
                tbcg.getDropoffDuration());
        TaxibusScheduler scheduler = new TaxibusScheduler(vrpData, qSim.getSimTimer(), params);
        TaxibusOptimizerContext optimizerContext = new TaxibusOptimizerContext(vrpData, scenario,
                qSim.getSimTimer(), travelTime, travelDisutility, scheduler,
                tbcg);

        switch (tbcg.getAlgorithm()) {

            case "sharedTaxi":
            	return new SharedTaxiOptimizer(optimizerContext, false, tbcg.getDetourFactor());
            case "stateBased":
            	return new StatebasedOptimizer(optimizerContext, false, this.stateSpace , tbcg);
            case "clustered":
            {
            	ClusteringTaxibusOptimizerContext context = new ClusteringTaxibusOptimizerContext(vrpData, scenario, qSim.getSimTimer(), travelTime, travelDisutility, scheduler, tbcg);
            	return new ClusteringTaxibusOptimizer(context, new SimpleDispatchCreator(context));
            }
            case "jsprit":
            {
            	ClusteringTaxibusOptimizerContext context = new ClusteringTaxibusOptimizerContext(vrpData, scenario, qSim.getSimTimer(), travelTime, travelDisutility, scheduler, tbcg);
            	return new JspritTaxibusOptimizer(context);
            }
            case "clustered_jsprit":
            {
            	ClusteringTaxibusOptimizerContext context = new ClusteringTaxibusOptimizerContext(vrpData, scenario, qSim.getSimTimer(), travelTime, travelDisutility, scheduler, tbcg);
            	return new ClusteringTaxibusOptimizer(context, new JspritDispatchCreator(context));
            }
            default:
                throw new RuntimeException(
                        "No config parameter set for algorithm, please check and assign in config");
        }
    }
}
