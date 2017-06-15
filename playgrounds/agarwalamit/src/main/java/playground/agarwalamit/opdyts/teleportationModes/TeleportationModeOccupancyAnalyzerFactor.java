/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.opdyts.teleportationModes;

import java.util.Map;
import java.util.Set;
import floetteroed.utilities.math.Vector;
import opdytsintegration.SimulationStateAnalyzer;
import opdytsintegration.utils.TimeDiscretization;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;

/**
 * Created by amit on 15.06.17. Adapted after {@link opdytsintegration.car.DifferentiatedLinkOccupancyAnalyzerFactory}
 */

public class TeleportationModeOccupancyAnalyzerFactor implements SimulationStateAnalyzer {

    private final TimeDiscretization timeDiscretization;
    private final Set<String> relevantTeleportationMdoes;
    private final Map<Id<Link>,Set<Id<Link>>> relevantZones;
    private TeleportationODAnalyzer teleportationODAnalyzer;

    public TeleportationModeOccupancyAnalyzerFactor (final TimeDiscretization timeDiscretization,
                                                     final Set<String> relevantTeleportationMdoes,
                                                     final Map<Id<Link>, Set<Id<Link>>> relevantZones) {
        this.timeDiscretization = timeDiscretization;
        this.relevantTeleportationMdoes = relevantTeleportationMdoes;

        this.relevantZones = relevantZones;
    }

    @Override
    public String getStringIdentifier() {
        return "teleportationModes";
    }

    @Override
    public EventHandler newEventHandler() {
        this.teleportationODAnalyzer = new TeleportationODAnalyzer(timeDiscretization, relevantZones, relevantTeleportationMdoes);
        return this.teleportationODAnalyzer;
    }

    @Override
    public Vector newStateVectorRepresentation() {
        return this.teleportationODAnalyzer.newStateVectorRepresentation();
    }

    @Override
    public void beforeIteration() {
        this.teleportationODAnalyzer.beforeIteration();
    }
}
