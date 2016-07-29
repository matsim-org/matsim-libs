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

package playground.michalm.taxi.run;

import java.util.Collection;

import org.apache.commons.configuration.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.run.*;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.router.util.*;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.michalm.ev.data.EvData;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.assignment.*;
import playground.michalm.taxi.scheduler.ETaxiScheduler;
import playground.michalm.taxi.vrpagent.ETaxiActionCreator;


public class ETaxiQSimProvider
    extends TaxiQSimProvider
{
    private final EvData evData;


    @Inject
    public ETaxiQSimProvider(EventsManager eventsManager, Collection<AbstractQSimPlugin> plugins,
            Scenario scenario, TaxiData taxiData,
            @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime,
            @Named(TaxiModule.TAXI_MODE) VehicleType vehicleType, EvData evData)
    {
        super(eventsManager, plugins, scenario, taxiData, travelTime, vehicleType, null);
        this.evData = evData;
    }


    protected TaxiOptimizer createTaxiOptimizer(QSim qSim)
    {
        TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(taxiCfg);
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
        ETaxiScheduler scheduler = new ETaxiScheduler(scenario, taxiData, qSim.getSimTimer(),
                schedulerParams, travelTime, travelDisutility);

        ETaxiOptimizerContext optimContext = new ETaxiOptimizerContext(taxiData,
                scenario.getNetwork(), qSim.getSimTimer(), travelTime, travelDisutility, scheduler,
                evData);

        Configuration optimizerConfig = new MapConfiguration(
                taxiCfg.getOptimizerConfigGroup().getParams());

        String type = optimizerConfig.getString("type");
        switch (type) {
            case "E_RULE_BASED":
                return new RuleBasedETaxiOptimizer(optimContext,
                        new RuleBasedETaxiOptimizerParams(optimizerConfig));

            case "E_ASSIGNMENT":
                return new AssignmentETaxiOptimizer(optimContext,
                        new AssignmentETaxiOptimizerParams(optimizerConfig));

            default:
                throw new RuntimeException();
        }
    }


    protected VrpAgentSource createVrpAgentSource(TaxiOptimizer optimizer, QSim qSim,
            PassengerEngine passengerEngine, VehicleType vehicleType)
    {
        LegCreator legCreator = taxiCfg.isOnlineVehicleTracker() ? //
                VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) : //
                VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        ETaxiActionCreator actionCreator = new ETaxiActionCreator(passengerEngine, legCreator,
                taxiCfg.getPickupDuration(), qSim.getSimTimer());
        return new VrpAgentSource(actionCreator, taxiData, optimizer, qSim, vehicleType);
    }

}
