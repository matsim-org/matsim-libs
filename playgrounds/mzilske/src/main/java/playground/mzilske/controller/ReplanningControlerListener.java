/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ReplanningControlerListener.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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
 */

package playground.mzilske.controller;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;

import javax.inject.Inject;
import javax.inject.Provider;

class ReplanningControlerListener implements ReplanningListener {

    @Inject StrategyManager strategyManager;
    @Inject Population population;
    @Inject ReplanningContextFactory replanningContextFactory;

    @Override
    public void notifyReplanning(ReplanningEvent event) {
        strategyManager.run(population, event.getIteration(), replanningContextFactory.create(event.getIteration()));
    }

}
