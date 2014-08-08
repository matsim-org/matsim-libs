/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CreateODDemand.java
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

package playground.mzilske.sensors;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;

import javax.inject.Inject;

class ODDemandControlerListener implements Provider<ControlerListener> {

    @Inject
    SightingsPopulation sightingsPopulation;

    @Inject
    Scenario scenario;

    @Override
    public ControlerListener get() {
        return new StartupListener() {
            @Override
            public void notifyStartup(StartupEvent event) {
                sightingsPopulation.insertPersons(scenario);
            }
        };
    }

}
