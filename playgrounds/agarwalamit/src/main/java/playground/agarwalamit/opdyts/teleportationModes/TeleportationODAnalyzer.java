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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimCountingStateAnalyzer;
import opdytsintegration.utils.TimeDiscretization;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;

/**
 * Created by amit on 15.06.17. Adapted after {@link opdytsintegration.car.DifferentiatedLinkOccupancyAnalyzer}
 */


public class TeleportationODAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler {

    private final Map<String, MATSimCountingStateAnalyzer<Link>> mode2stateAnalyzer;
    private final Map<Id<Link>,Set<Id<Link>>> relevantZones;
    private final TimeDiscretization timeDiscretization;

    public TeleportationODAnalyzer(final TimeDiscretization timeDiscretization,
                                   final Map<Id<Link>,Set<Id<Link>>> relevantZones,
                                   final Set<String> relevantModes) {
        this.relevantZones = relevantZones;
        this.mode2stateAnalyzer = new LinkedHashMap<>();
        this.timeDiscretization = timeDiscretization;
        for (String mode : relevantModes) {
            this.mode2stateAnalyzer.put(mode, new MATSimCountingStateAnalyzer<Link>(timeDiscretization));
        }
    }

    public MATSimCountingStateAnalyzer<Link> getNetworkModeAnalyzer(final String mode) {
        return this.mode2stateAnalyzer.get(mode);
    }

    public void beforeIteration() {
        for (MATSimCountingStateAnalyzer<Link> stateAnalyzer : this.mode2stateAnalyzer.values()) {
            stateAnalyzer.beforeIteration();
        }
    }

    @Override
    public void reset(int iteration) {

    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        final MATSimCountingStateAnalyzer<Link> stateAnalyzer = this.mode2stateAnalyzer.get(event.getLegMode());
        if (this.mode2stateAnalyzer.containsKey(event.getLegMode())) {
            for (Id<Link> id : this.relevantZones.keySet()) {
                if ( this.relevantZones.get(id).contains(event.getLinkId())) {
                    stateAnalyzer.registerDecrease(id, (int)event.getTime());
                } else {
                    //dont do anything.
                }
            }
        } else {
            // network modes thus irrelevant here
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        final MATSimCountingStateAnalyzer<Link> stateAnalyzer = this.mode2stateAnalyzer.get(event.getLegMode());
        if (this.mode2stateAnalyzer.containsKey(event.getLegMode())) {
            for (Id<Link> id : this.relevantZones.keySet()) {
                if ( this.relevantZones.get(id).contains(event.getLinkId())) {
                    stateAnalyzer.registerIncrease(id, (int)event.getTime());
                } else {
                    //dont do anything.
                }
            }
        } else {
            // network modes thus irrelevant here
        }
    }

    public Vector newStateVectorRepresentation() {
        final Vector result = new Vector(
                this.mode2stateAnalyzer.size() * this.relevantZones.size()  * this.timeDiscretization.getBinCnt());
        int i = 0;
        for (String mode : this.mode2stateAnalyzer.keySet()) {
            final MATSimCountingStateAnalyzer<Link> analyzer = this.mode2stateAnalyzer.get(mode);
            for (Id<Link> zoneId : this.relevantZones.keySet()) {
                for (int bin = 0; bin < this.timeDiscretization.getBinCnt(); bin++) {
                    result.set(i++, analyzer.getCount(zoneId, bin));
                }
            }
        }
        return result;
    }
}
