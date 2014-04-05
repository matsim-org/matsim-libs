/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * StrategyManagerProvider.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.PlanStrategyFactoryRegister;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;

import javax.inject.Inject;
import javax.inject.Provider;


public class StrategyManagerProvider implements Provider<StrategyManager> {

    private static Logger log = Logger.getLogger(StrategyManagerProvider.class);

    @Inject Scenario scenario;
    @Inject EventsManager events;
    @Inject OutputDirectoryHierarchy controlerIO;

    @Override
    public StrategyManager get() {
        PlanStrategyRegistrar planStrategyFactoryRegistrar = new PlanStrategyRegistrar();
        PlanStrategyFactoryRegister planStrategyFactoryRegister = planStrategyFactoryRegistrar.getFactoryRegister();
        StrategyManager manager = new StrategyManager();
        StrategyManagerConfigLoader.load(scenario, controlerIO, events, manager, planStrategyFactoryRegister);
        return manager;
    }

}
