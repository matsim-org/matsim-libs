/*
 *  *********************************************************************** *
// *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package org.matsim.contrib.freight.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.GenericStrategyManager;

import javax.inject.Inject;
/**
 * Controls the workflow of the simulation.
 * <p/>
 * <p>Processes the required actions during the matsim simulation workflow (replanning, scoring, sim). For example, it informs agents to
 * score their plans when it is scoring time, and it informs them to re-plan, or it injects carriers into the simulation when it is time
 * to inject them. Currently it is kept to minimum functions, i.e. injecting carrier plans into sim and the possibility
 * to set custom scoring- and replanning-functionalities.
 *
 * @author sschroeder, mzilske
 */

class CarrierControlerListener implements StartupListener, ShutdownListener, BeforeMobsimListener, AfterMobsimListener, ScoringListener,
        ReplanningListener, IterationEndsListener {

    private CarrierScoringFunctionFactory carrierScoringFunctionFactory;

    private CarrierPlanStrategyManagerFactory carrierPlanStrategyManagerFactory;

    private CarrierAgentTracker carrierAgentTracker;

    private Carriers carriers;

    @Inject EventsManager eventsManager;
    @Inject Network network;

    /**
     * Constructs a controller with a set of carriers, re-planning capabilities and scoring-functions.
     */
    @Inject
    CarrierControlerListener(Carriers carriers, CarrierPlanStrategyManagerFactory strategyManagerFactory, CarrierScoringFunctionFactory scoringFunctionFactory) {
        this.carriers = carriers;
        this.carrierPlanStrategyManagerFactory = strategyManagerFactory;
        this.carrierScoringFunctionFactory = scoringFunctionFactory;
    }

    public Map<Id<Carrier>, Carrier> getCarriers() {
        return carriers.getCarriers();
    }

    @Override
    public void notifyStartup(StartupEvent event) {
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        carrierAgentTracker = new CarrierAgentTracker(carriers, network, carrierScoringFunctionFactory);
        eventsManager.addHandler(carrierAgentTracker);
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        eventsManager.removeHandler(carrierAgentTracker);
    }

    @Override
    public void notifyScoring(ScoringEvent event) {
        carrierAgentTracker.scoreSelectedPlans();
    }

    public CarrierAgentTracker getCarrierAgentTracker() {
        return carrierAgentTracker;
    }

    @Override
    public void notifyReplanning(final ReplanningEvent event) {
        if (carrierPlanStrategyManagerFactory == null) {
            return;
        }
        GenericStrategyManager<CarrierPlan, Carrier> strategyManager = carrierPlanStrategyManagerFactory.createStrategyManager();
        Collection<HasPlansAndId<CarrierPlan, Carrier>> collection = new ArrayList<>();
        for (Carrier carrier : carriers.getCarriers().values()) {
            collection.add(carrier);
        }
        strategyManager.run(collection, null, event.getIteration(), event.getReplanningContext());
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {

    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {

    }

}
