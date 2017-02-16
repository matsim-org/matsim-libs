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

package playground.ikaddoura.optAV;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;
import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutility;


public class OptAVQSimProvider implements Provider<Mobsim> {
	   
	private final EventsManager eventsManager;
    private final Collection<AbstractQSimPlugin> plugins;

    protected final Scenario scenario;
    protected final Fleet fleet;
    protected final TravelTime travelTime;

    protected final TaxiConfigGroup taxiCfg;
    private final VehicleType vehicleType;
    private final TaxiOptimizerFactory optimizerFactory;
    private final SubmittedTaxiRequestsCollector requestsCollector;
    private final MoneyEventAnalysis moneyAnalysis;
    
    @Inject
    public OptAVQSimProvider(
    		EventsManager eventsManager,
    		Collection<AbstractQSimPlugin> plugins,
            Scenario scenario,
            Fleet fleet,
            @Named(VrpTravelTimeModules.DVRP_ESTIMATED) TravelTime travelTime,
            @Named(TaxiModule.TAXI_MODE) VehicleType vehicleType,
            TaxiOptimizerFactory optimizerFactory,
            SubmittedTaxiRequestsCollector requestsCollector,
            MoneyEventAnalysis moneyAnalysis) {
    	    	
        this.eventsManager = eventsManager;
        this.plugins = plugins;
        this.scenario = scenario;
        this.fleet = fleet;
        this.travelTime = travelTime;
        this.taxiCfg = TaxiConfigGroup.get(scenario.getConfig());
        this.vehicleType = vehicleType;
        this.optimizerFactory = optimizerFactory;
        this.requestsCollector = requestsCollector;
        this.moneyAnalysis = moneyAnalysis;
    }


    @Override
    public Mobsim get() {
    	QSim qSim = QSimUtils.createQSim(scenario, eventsManager, plugins);
        
        requestsCollector.reset();

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


    protected TaxiOptimizer createTaxiOptimizer(QSim qSim) {
       
    	TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(taxiCfg);
        
    	RandomizingTimeDistanceTravelDisutilityFactory timeDistanceTravelDisutility = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, scenario.getConfig().planCalcScore());
		MoneyTimeDistanceTravelDisutility travelDisutility = new MoneyTimeDistanceTravelDisutility(timeDistanceTravelDisutility.createTravelDisutility(travelTime), scenario, moneyAnalysis);
    	        
        TaxiScheduler scheduler = new TaxiScheduler(
        		scenario,
        		fleet,
        		qSim.getSimTimer(),
                schedulerParams,
                travelTime,
                travelDisutility);

        TaxiOptimizerContext optimContext = new TaxiOptimizerContext(
        		fleet,
        		scenario.getNetwork(),
                qSim.getSimTimer(),
                travelTime,
                travelDisutility,
                scheduler);
        
        return optimizerFactory.createTaxiOptimizer(optimContext, taxiCfg.getOptimizerConfigGroup());
    }


    protected PassengerEngine createPassengerEngine(TaxiOptimizer optimizer) {
        return new PassengerEngine(TaxiModule.TAXI_MODE, eventsManager, new TaxiRequestCreator(requestsCollector), optimizer, scenario.getNetwork());
    }


    protected VrpAgentSource createVrpAgentSource(
    		TaxiOptimizer optimizer,
    		QSim qSim,
            PassengerEngine passengerEngine,
            VehicleType vehicleType) {
    	    	
        LegCreator legCreator = taxiCfg.isOnlineVehicleTracker() ? //
                VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) : //
                VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                taxiCfg.getPickupDuration());
        return new VrpAgentSource(actionCreator, fleet, optimizer, qSim, vehicleType);
    }
}
