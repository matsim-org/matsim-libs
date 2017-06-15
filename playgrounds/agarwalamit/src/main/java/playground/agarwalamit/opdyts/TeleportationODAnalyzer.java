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

package playground.agarwalamit.opdyts;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimCountingStateAnalyzer;
import opdytsintegration.utils.TimeDiscretization;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * Created by amit on 15.06.17. Adapted after {@link opdytsintegration.car.DifferentiatedLinkOccupancyAnalyzer}
 */


public class TeleportationODAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler {

    private final Map<String, MATSimCountingStateAnalyzer<Geometry>> mode2stateAnalyzer;
    private final Map<Id<Geometry>,Geometry> relevantZones;
    private final Network network;
    private final TimeDiscretization timeDiscretization;

    public TeleportationODAnalyzer(final TimeDiscretization timeDiscretization,
                                   final Map<Id<Geometry>,Geometry> relevantZones,
                                   final Set<String> relevantModes,
                                   final Network network) {
        this.relevantZones = relevantZones;
        this.mode2stateAnalyzer = new LinkedHashMap<>();
        this.timeDiscretization = timeDiscretization;
        for (String mode : relevantModes) {
            this.mode2stateAnalyzer.put(mode, new MATSimCountingStateAnalyzer<Geometry>(timeDiscretization));
        }
        this.network = network;
    }

    private Geometry getRelevantGeometry (final Point point) {
        for (Geometry geometry : this.relevantZones.values()) {
            if (geometry.covers(point)) return geometry;
        }
        return null;
    }

    private Point getPointFromLinkId(final Id<Link> linkId) {
        final GeometryFactory gf = new GeometryFactory();
        Link l = this.network.getLinks().get(linkId);
        Coordinate actCoordinate = new Coordinate (l.getCoord().getX(),l.getCoord().getY());
        return gf.createPoint(actCoordinate);
    }

    public MATSimCountingStateAnalyzer<Geometry> getNetworkModeAnalyzer(final String mode) {
        return this.mode2stateAnalyzer.get(mode);
    }

    public void beforeIteration() {
        for (MATSimCountingStateAnalyzer<Geometry> stateAnalyzer : this.mode2stateAnalyzer.values()) {
            stateAnalyzer.beforeIteration();
        }
    }

    @Override
    public void reset(int iteration) {

    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        final MATSimCountingStateAnalyzer<Geometry> stateAnalyzer = this.mode2stateAnalyzer.get(event.getLegMode());
        if (this.mode2stateAnalyzer.containsKey(event.getLegMode())) {
            Point point = this.getPointFromLinkId(event.getLinkId());
            Geometry geometry = this.getRelevantGeometry(point);
            if (geometry!=null) {
                stateAnalyzer.registerDecrease(Id.create(geometry.getCentroid().getX()+"_"+geometry.getCentroid().getY(),Geometry.class), (int)event.getTime());
            }
        } else {
            // network modes thus irrelevant here
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        final MATSimCountingStateAnalyzer<Geometry> stateAnalyzer = this.mode2stateAnalyzer.get(event.getLegMode());
        if (this.mode2stateAnalyzer.containsKey(event.getLegMode())) {
            Point point = this.getPointFromLinkId(event.getLinkId());
            Geometry geometry = this.getRelevantGeometry(point);
            if (geometry!=null) {
                stateAnalyzer.registerIncrease(Id.create(geometry.getCentroid().getX()+"_"+geometry.getCentroid().getY(),Geometry.class), (int)event.getTime());
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
            final MATSimCountingStateAnalyzer<Geometry> analyzer = this.mode2stateAnalyzer.get(mode);
            for (Id<Geometry> linkId : this.relevantZones.keySet()) {
                for (int bin = 0; bin < this.timeDiscretization.getBinCnt(); bin++) {
                    result.set(i++, analyzer.getCount(linkId, bin));
                }
            }
        }
        return result;
    }
}
