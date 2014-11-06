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

package playground.mzilske.matrices;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import playground.mzilske.cdr.PopulationFromSightings;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.cdr.ZoneTracker;

import javax.inject.Inject;

class MatrixPopulationGenerationControlerListener implements Provider<ControlerListener> {

    @Inject
    Sightings sightings;

    @Inject
    TimedMatrices timedMatrices;

    @Inject
    Scenario scenario;

    @Inject
    ZoneTracker.LinkToZoneResolver linkToZoneResolver;

    @Override
    public ControlerListener get() {
        return new StartupListener() {
            @Override
            public void notifyStartup(StartupEvent event) {
                MatricesToSightings.insertSightingsForMatrices(timedMatrices, sightings);
                PopulationFromSightings.createPopulationWithRandomRealization(scenario, sightings, linkToZoneResolver);
             }
        };
    }

}
