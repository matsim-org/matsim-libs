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
package playground.jbischoff.taxi.usability;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.extensions.taxi.TaxiUtils;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.router.DistanceAsTravelDisutility;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.*;

import com.google.inject.*;

import playground.michalm.taxi.*;
import playground.michalm.taxi.optimizer.TaxiOptimizerContext;
import playground.michalm.taxi.optimizer.rules.*;
import playground.michalm.taxi.scheduler.*;


/**
 * @author jbischoff
 */

public class TaxiQSimProvider
    implements Provider<QSim>
{
    private TaxiConfigGroup tcg;
    private MatsimVrpContextImpl context;
    private RuleBasedTaxiOptimizer optimizer;
    private EventsManager events;
    private TravelTime travelTime;


    //	@Inject
    //	TaxiQSimProvider(Config config, MatsimVrpContext context , EventsManager events, TravelTime travelTime) {
    //		this.tcg = (TaxiConfigGroup) config.getModule("taxiConfig");
    //		this.context = (MatsimVrpContextImpl) context;
    //		this.events=events;
    //		this.travelTime = travelTime;
    //
    //	}
    @Inject
    TaxiQSimProvider(Config config, MatsimVrpContext context, EventsManager events,
            Map<String, TravelTime> travelTimes)
    {
        this.tcg = (TaxiConfigGroup)config.getModule("taxiConfig");
        this.context = (MatsimVrpContextImpl)context;
        this.events = events;
        this.travelTime = travelTimes.get("car");

    }


    private QSim createMobsim(Scenario sc, EventsManager eventsManager)
    {
        initiate();
        QSim qSim = DynAgentLauncherUtils.initQSim(sc, eventsManager);
        qSim.addQueueSimulationListeners(optimizer);
        context.setMobsimTimer(qSim.getSimTimer());
        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(TaxiUtils.TAXI_MODE,
                new TaxiRequestCreator(), optimizer, context, qSim);
        LegCreator legCreator = VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                tcg.getPickupDuration());
        VrpLauncherUtils.initAgentSources(qSim, context, optimizer, actionCreator);
        return qSim;
    }


    void initiate()
    {
        //this initiation takes place upon creating qsim for each iteration
        TravelDisutility travelDisutility = new DistanceAsTravelDisutility();

        TaxiSchedulerParams params = new TaxiSchedulerParams(tcg.isDestinationKnown(),
                tcg.isVehicleDiversion(), tcg.getPickupDuration(), tcg.getDropoffDuration(), 1.);

        resetSchedules(context.getVrpData().getVehicles().values());

        TaxiScheduler scheduler = new TaxiScheduler(context, params, travelTime, travelDisutility);

        RuleBasedTaxiOptimizerParams optimParams = new RuleBasedTaxiOptimizerParams(null);

        TaxiOptimizerContext optimContext = new TaxiOptimizerContext(context, travelTime,
                travelDisutility, optimParams, scheduler);
        optimizer = new RuleBasedTaxiOptimizer(optimContext);

    }


    private void resetSchedules(Iterable<Vehicle> vehicles)
    {

        for (Vehicle v : vehicles) {
            VehicleImpl vi = (VehicleImpl)v;
            vi.resetSchedule();

        }
    }


    @Override
    public QSim get()
    {
        return createMobsim(context.getScenario(), this.events);
    }

}
