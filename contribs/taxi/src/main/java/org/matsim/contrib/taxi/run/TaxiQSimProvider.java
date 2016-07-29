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
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.router.util.*;
import org.matsim.vehicles.VehicleType;

import com.google.inject.*;
import com.google.inject.name.Named;


public class TaxiQSimProvider
    implements Provider<Mobsim>
{
    private final EventsManager eventsManager;
    private final Collection<AbstractQSimPlugin> plugins;

    protected final Scenario scenario;
    protected final TaxiData taxiData;
    protected final TravelTime travelTime;

    protected final TaxiConfigGroup taxiCfg;
    private final VehicleType vehicleType;//TODO resolve this by subclassing (without guice)??
    private final TaxiOptimizerFactory optimizerFactory;//TODO resolve this subclassing (without guice)??


    @Inject
    public TaxiQSimProvider(EventsManager eventsManager, Collection<AbstractQSimPlugin> plugins,
            Scenario scenario, TaxiData taxiData,
            @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime,
            @Named(TaxiModule.TAXI_MODE) VehicleType vehicleType,
            TaxiOptimizerFactory optimizerFactory)
    {
        this.eventsManager = eventsManager;
        this.plugins = plugins;
        this.scenario = scenario;
        this.taxiData = taxiData;
        this.travelTime = travelTime;
        this.taxiCfg = TaxiConfigGroup.get(scenario.getConfig());
        this.vehicleType = vehicleType;
        this.optimizerFactory = optimizerFactory;
    }


    @Override
    public Mobsim get()
    {
        //TODO add this to Config checkers
        if (taxiCfg.isVehicleDiversion() && !taxiCfg.isOnlineVehicleTracker()) {
            throw new IllegalStateException("Diversion requires online tracking");
        }

        QSim qSim = QSimUtils.createQSim(scenario, eventsManager, plugins);

        TaxiOptimizer optimizer = createTaxiOptimizer(qSim);
        qSim.addQueueSimulationListeners(optimizer);

        PassengerEngine passengerEngine = createPassengerEngine(optimizer);
        qSim.addMobsimEngine(passengerEngine);
        qSim.addDepartureHandler(passengerEngine);

        VrpAgentSource agentSource = createVrpAgentSource(optimizer, qSim, passengerEngine,
                vehicleType);
        qSim.addAgentSource(agentSource);

        return qSim;
    }


    protected TaxiOptimizer createTaxiOptimizer(QSim qSim)
    {
        TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(taxiCfg);
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
        TaxiScheduler scheduler = new TaxiScheduler(scenario, taxiData, qSim.getSimTimer(),
                schedulerParams, travelTime, travelDisutility);

        TaxiOptimizerContext optimContext = new TaxiOptimizerContext(taxiData,
                scenario.getNetwork(), qSim.getSimTimer(), travelTime, travelDisutility, scheduler);
        return optimizerFactory.createTaxiOptimizer(optimContext,
                taxiCfg.getOptimizerConfigGroup());
    }


    protected PassengerEngine createPassengerEngine(TaxiOptimizer optimizer)
    {
        return new PassengerEngine(TaxiModule.TAXI_MODE, eventsManager, new TaxiRequestCreator(),
                optimizer, taxiData, scenario.getNetwork());
    }


    protected VrpAgentSource createVrpAgentSource(TaxiOptimizer optimizer, QSim qSim,
            PassengerEngine passengerEngine, VehicleType vehicleType)
    {
        LegCreator legCreator = taxiCfg.isOnlineVehicleTracker() ? //
                VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) : //
                VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                taxiCfg.getPickupDuration());
        return new VrpAgentSource(actionCreator, taxiData, optimizer, qSim, vehicleType);
    }
}
