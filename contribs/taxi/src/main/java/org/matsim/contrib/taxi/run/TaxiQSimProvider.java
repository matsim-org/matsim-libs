/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.run;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.contrib.taxi.util.stats.*;
import org.matsim.contrib.taxi.util.stats.StatsCollector.StatsCalculator;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.router.util.*;

import com.google.inject.*;
import com.google.inject.name.Named;


public class TaxiQSimProvider
    implements Provider<Mobsim>
{
    private final Scenario scenario;
    private final EventsManager events;
    private final Collection<AbstractQSimPlugin> plugins;

    private final TaxiData taxiData;
    private final TravelTime travelTime;

    private final TaxiConfigGroup taxiCfg;
    private final TaxiOptimizerFactory optimizerFactory;


    @Inject
    public TaxiQSimProvider(Scenario scenario, EventsManager events,
            Collection<AbstractQSimPlugin> plugins, TaxiData taxiData,
            @Named(VrpTravelTimeModules.DVRP) TravelTime travelTime, TaxiConfigGroup taxiCfg,
            TaxiOptimizerFactory optimizerFactory)
    {
        this.scenario = scenario;
        this.events = events;
        this.plugins = plugins;
        this.taxiData = taxiData;
        this.travelTime = travelTime;
        this.taxiCfg = taxiCfg;
        this.optimizerFactory = optimizerFactory;
    }


    @Override
    public Mobsim get()
    {
        if (taxiCfg.isVehicleDiversion() && taxiCfg.isOnlineVehicleTracker()) {
            throw new IllegalStateException("Diversion requires online tracking");
        }

        QSim qSim = QSimUtils.createQSim(scenario, events, plugins);

        TaxiOptimizer optimizer = createTaxiOptimizer(qSim);
        qSim.addQueueSimulationListeners(optimizer);

        PassengerEngine passengerEngine = new PassengerEngine(TaxiModule.TAXI_MODE, events,
                new TaxiRequestCreator(), optimizer, taxiData, scenario.getNetwork());
        qSim.addMobsimEngine(passengerEngine);
        qSim.addDepartureHandler(passengerEngine);

        LegCreator legCreator = taxiCfg.isOnlineVehicleTracker() ? //
                VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) : //
                VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                taxiCfg.getPickupDuration());
        qSim.addAgentSource(new VrpAgentSource(actionCreator, taxiData, optimizer, qSim));

        addTaxiOverTimeCounters(qSim);
        return qSim;
    }


    private TaxiOptimizer createTaxiOptimizer(QSim qSim)
    {
        TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(taxiCfg);
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
        TaxiScheduler scheduler = new TaxiScheduler(scenario, taxiData, qSim.getSimTimer(),
                schedulerParams, travelTime, travelDisutility);

        TaxiOptimizerContext optimContext = new TaxiOptimizerContext(taxiData, scenario,
                qSim.getSimTimer(), travelTime, travelDisutility, scheduler);
        return optimizerFactory.createTaxiOptimizer(optimContext,
                taxiCfg.getOptimizerConfigGroup());
    }


    private void addTaxiOverTimeCounters(QSim qSim)
    {
        if (taxiCfg.getTaxiStatsFile() != null) {
            StatsCalculator<String> dispatchStatsCalc = StatsCalculators.combineStatsCalculators(
                    StatsCalculators.createCurrentTaxiTaskOfTypeCounter(taxiData), //
                    StatsCalculators.createRequestsWithStatusCounter(taxiData,
                            TaxiRequestStatus.UNPLANNED));
            qSim.addQueueSimulationListeners(new StatsCollector<>(dispatchStatsCalc, 300,
                    StatsCalculators.TAXI_TASK_TYPES_HEADER + //
                            TaxiRequestStatus.UNPLANNED, //
                    taxiCfg.getTaxiStatsFile()));
        }
    }
}
