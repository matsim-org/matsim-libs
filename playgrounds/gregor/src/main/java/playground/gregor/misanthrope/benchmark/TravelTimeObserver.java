package playground.gregor.misanthrope.benchmark;/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;
import playground.gregor.utils.Variance;

import java.util.HashMap;
import java.util.Map;

public class TravelTimeObserver implements LinkEnterEventHandler, LinkLeaveEventHandler {

    private final Id<Link> id1 = Id.createLinkId("1_8");
    private final Id<Link> id2 = Id.createLinkId("1_8r");
    private final double length = 500;
    private final Map<Id<Vehicle>, LinkEnterEvent> enters = new HashMap<>();
    private Variance v1 = new Variance();
    private Variance v2 = new Variance();

    public Variance getV1() {
        return v1;
    }

    public Variance getV2() {
        return v2;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (event.getLinkId() == id1 || event.getLinkId() == id2) {
            enters.put(event.getVehicleId(), event);
        }

    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (event.getLinkId() == id1) {
            LinkEnterEvent enter = enters.remove(event.getVehicleId());
            double tt = event.getTime() - enter.getTime();
            v1.addVar(length / tt);

        } else if (event.getLinkId() == id2) {
            LinkEnterEvent enter = enters.remove(event.getVehicleId());
            double tt = event.getTime() - enter.getTime();
            v2.addVar(length / tt);
        }
    }

    @Override
    public void reset(int iteration) {

    }
}
