/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TransitRouterModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package org.matsim.contrib.av.intermodal.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.name.Names;

import java.net.URL;


public class VariableAccessTransitRouterModule extends AbstractModule {

    @Override
    public void install() {
        if (getConfig().transit().isUseTransit()) {
        	VariableAccessConfigGroup vaconfig = (VariableAccessConfigGroup) getConfig().getModules().get(VariableAccessConfigGroup.GROUPNAME);
        	addRoutingModuleBinding(vaconfig.getMode()).toProvider(VariableAccessTransit.class);
        	Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        	URL scheduleFile = getConfig().transit().getTransitScheduleFileURL(getConfig().getContext());
        	if (vaconfig.getTransitScheduleFile()!=null){
        		scheduleFile = vaconfig.getTransitScheduleFileURL(getConfig().getContext());
        	}
        	
        	new TransitScheduleReader(scenario2).readURL(scheduleFile );
        	bind(MainModeIdentifier.class).to(VariableAccessMainModeIdentifier.class);
			bind(TransitSchedule.class).annotatedWith(Names.named("variableAccess")).toInstance(scenario2.getTransitSchedule());
            bind(TransitRouter.class).annotatedWith(Names.named("variableAccess")).toProvider(VariableAccessTransitRouterImplFactory.class);
        }
    }

}
