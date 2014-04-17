/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ControllerModuleWithScenario.java
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

import com.google.inject.AbstractModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioImpl;

public class ControllerModuleWithScenario extends AbstractModule {
    private final Scenario scenario;

    public ControllerModuleWithScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    protected void configure() {
        install(new ControllerModule());
        bind(Scenario.class).toInstance(scenario);
    }

}
