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
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.router.util.*;

import com.google.inject.*;
import com.google.inject.name.Named;

import playground.jbischoff.taxibus.algorithm.TaxibusActionCreator;
import playground.jbischoff.taxibus.algorithm.optimizer.*;
import playground.jbischoff.taxibus.algorithm.optimizer.fifo.*;
import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.LineDispatcher;
import playground.jbischoff.taxibus.algorithm.passenger.*;
import playground.jbischoff.taxibus.algorithm.scheduler.*;
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
    private final LineDispatcher dispatcher;
    private final TaxibusPassengerOrderManager orderManager;


    @Inject
    TaxibusQSimProvider(Scenario scenario, EventsManager events,
            Collection<AbstractQSimPlugin> plugins, VrpData vrpData,
            @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime, TaxibusConfigGroup tbcg,
            LineDispatcher dispatcher, TaxibusPassengerOrderManager orderManager)
    {
        this.scenario = scenario;
        this.events = events;
        this.plugins = plugins;
        this.vrpData = vrpData;
        this.travelTime = travelTime;
        this.tbcg = tbcg;
        this.dispatcher = dispatcher;
        this.orderManager = orderManager;
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
        orderManager.setPassengerEngine(passengerEngine);
        qSim.addMobsimEngine(passengerEngine);
        qSim.addDepartureHandler(passengerEngine);
        qSim.addQueueSimulationListeners(orderManager);

        LegCreator legCreator = VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        TaxibusActionCreator actionCreator = new TaxibusActionCreator(passengerEngine, legCreator,
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
                qSim.getSimTimer(), travelTime, travelDisutility, scheduler, tbcg.getOutputDir(),
                tbcg);

        switch (tbcg.getAlgorithmConfig()) {
            case "line":
                return new FifoOptimizer(optimizerContext, dispatcher, false);

            case "multipleLine":
                return new MultipleFifoOptimizer(optimizerContext, dispatcher, false);

            default:
                throw new RuntimeException(
                        "No config parameter set for algorithm, please check and assign in config");
        }
    }
}
