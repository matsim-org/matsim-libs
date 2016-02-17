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

import java.util.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.contrib.taxi.*;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.*;


public class TaxiQSimProvider
    implements Provider<Mobsim>
{
    @Inject Scenario scenario;
    @Inject EventsManager events;
    @Inject Collection<AbstractQSimPlugin> plugins;

    @Inject VrpData vrpData;
    @Inject TravelTime travelTime;
    @Inject TaxiSchedulerParams schedulerParams;
    @Inject AbstractTaxiOptimizerParams optimParams;

    private boolean onlineVehicleTracker = true;//TODO move to the config group
    private boolean prebookTripsBeforeSimulation = false;//TODO configurable by guice????


    @Override
    public Mobsim get()
    {
        QSim qSim = QSimUtils.createQSim(scenario, events, plugins);

        MatsimVrpContextImpl context = new MatsimVrpContextImpl();//TODO use guice instead for accessing the data
        context.setVrpData(vrpData);
        context.setScenario(scenario);
        context.setMobsimTimer(qSim.getSimTimer());

        if (schedulerParams.vehicleDiversion && onlineVehicleTracker) {
            throw new IllegalStateException("Diversion requires online tracking");
        }
        TaxiOptimizer optimizer = TaxiOptimizers.createOptimizer(context, travelTime, optimParams,
                schedulerParams);

        qSim.addQueueSimulationListeners(optimizer);

        //TODO create PassengerEngineProfile??
        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(TaxiModule.TAXI_MODE,
                new TaxiRequestCreator(), optimizer, context, qSim);

        if (prebookTripsBeforeSimulation) {
            qSim.addQueueSimulationListeners(new BeforeSimulationTripPrebooker(passengerEngine));
        }

        LegCreator legCreator = onlineVehicleTracker ? //
                VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) : //
                VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                schedulerParams.pickupDuration);
        qSim.addAgentSource(new VrpAgentSource(actionCreator, context, optimizer, qSim));

        return qSim;
    }
}
