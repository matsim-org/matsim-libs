/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ControllerModuleWithConfigFilename.java
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
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;

import javax.inject.Singleton;

public class ControllerModuleWithConfigFilename  extends AbstractModule {

    private String configFileName;

    public ControllerModuleWithConfigFilename(String configFileName) {
        this.configFileName = configFileName;
    }

    @Override
    protected void configure() {
        install(new ControllerModule());
        bindConstant().annotatedWith(Names.named("configFileName")).to(configFileName);
        bind(Config.class).toProvider(ConfigLoader.class).in(Singleton.class);
        bind(Scenario.class).toProvider(ScenarioLoader.class).in(Singleton.class);
    }

}
