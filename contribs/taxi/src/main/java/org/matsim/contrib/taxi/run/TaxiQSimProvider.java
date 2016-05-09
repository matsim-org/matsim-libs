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
import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.router.util.*;
import org.matsim.vehicles.VehicleType;

import com.google.inject.*;
import com.google.inject.name.Named;


public class TaxiQSimProvider
    implements Provider<Mobsim>
{
    private final MatsimServices matsimServices;
    private final Collection<AbstractQSimPlugin> plugins;

    private final TaxiData taxiData;
    private final TravelTime travelTime;

    private final TaxiConfigGroup taxiCfg;
    private final VehicleType vehicleType;
    private final TaxiOptimizerFactory optimizerFactory;


    @Inject
    public TaxiQSimProvider(MatsimServices matsimServices, Collection<AbstractQSimPlugin> plugins,
            TaxiData taxiData, @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime,
            TaxiConfigGroup taxiCfg, @Named(TaxiModule.TAXI_MODE) VehicleType vehicleType,
            TaxiOptimizerFactory optimizerFactory)
    {
        this.matsimServices = matsimServices;
        this.plugins = plugins;
        this.taxiData = taxiData;
        this.travelTime = travelTime;
        this.taxiCfg = taxiCfg;
        this.vehicleType = vehicleType;
        this.optimizerFactory = optimizerFactory;
    }


    @Override
    public Mobsim get()
    {
        if (taxiCfg.isVehicleDiversion() && taxiCfg.isOnlineVehicleTracker()) {
            throw new IllegalStateException("Diversion requires online tracking");
        }

        QSim qSim = QSimUtils.createQSim(matsimServices.getScenario(), matsimServices.getEvents(),
                plugins);

        TaxiOptimizer optimizer = createTaxiOptimizer(qSim);
        qSim.addQueueSimulationListeners(optimizer);

        PassengerEngine passengerEngine = createPassengerEngine(optimizer);
        qSim.addMobsimEngine(passengerEngine);
        qSim.addDepartureHandler(passengerEngine);

        VrpAgentSource agentSource = createVrpAgentSource(optimizer, qSim, passengerEngine,
                vehicleType);
        qSim.addAgentSource(agentSource);

        addTimeProfileCollector(qSim);
        return qSim;
    }


    private TaxiOptimizer createTaxiOptimizer(QSim qSim)
    {
        TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(taxiCfg);
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
        TaxiScheduler scheduler = new TaxiScheduler(matsimServices.getScenario(), taxiData,
                qSim.getSimTimer(), schedulerParams, travelTime, travelDisutility);

        TaxiOptimizerContext optimContext = new TaxiOptimizerContext(taxiData, matsimServices,
                qSim.getSimTimer(), travelTime, travelDisutility, scheduler);
        return optimizerFactory.createTaxiOptimizer(optimContext,
                taxiCfg.getOptimizerConfigGroup());
    }


    private PassengerEngine createPassengerEngine(TaxiOptimizer optimizer)
    {
        return new PassengerEngine(TaxiModule.TAXI_MODE, matsimServices.getEvents(),
                new TaxiRequestCreator(), optimizer, taxiData,
                matsimServices.getScenario().getNetwork());
    }


    private VrpAgentSource createVrpAgentSource(TaxiOptimizer optimizer, QSim qSim,
            PassengerEngine passengerEngine, VehicleType vehicleType)
    {
        LegCreator legCreator = taxiCfg.isOnlineVehicleTracker() ? //
                VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) : //
                VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                taxiCfg.getPickupDuration());
        return new VrpAgentSource(actionCreator, taxiData, optimizer, qSim, vehicleType);
    }


    //TODO move outside QSimProvider
    private void addTimeProfileCollector(QSim qSim)
    {
        if (taxiCfg.getTimeProfiles()) {
            ProfileCalculator<String> dispatchStatsCalc = TimeProfiles.combineProfileCalculators(
                    TimeProfiles.createCurrentTaxiTaskOfTypeCounter(taxiData), //
                    TimeProfiles.createRequestsWithStatusCounter(taxiData,
                            TaxiRequestStatus.UNPLANNED));

            qSim.addQueueSimulationListeners(new TimeProfileCollector<>(dispatchStatsCalc, 300,
                    TimeProfiles.TAXI_TASK_TYPES_HEADER + //
                            TaxiRequestStatus.UNPLANNED, //
                    matsimServices));
        }
    }
}
