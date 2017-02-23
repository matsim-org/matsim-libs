package playground.gregor.sim2d_v4.events.debug;/* *********************************************************************** *
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

import com.vividsolutions.jts.geom.Coordinate;
import org.matsim.api.core.v01.events.Event;

public class PolygonEvent extends Event {

    private static final String type = "POLYGON_EVENT";
    private final Coordinate[] coords;
    private final int r;
    private final int g;
    private final int b;
    private final int a;
    private final boolean stat;

    public PolygonEvent(double time, Coordinate[] coords, int r, int g, int b, int a, boolean stat) {
        super(time);
        this.coords = coords;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.stat = stat;
    }

    public Coordinate[] getCoords() {
        return coords;
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    public int getA() {
        return a;
    }

    public boolean isStat() {
        return stat;
    }

    @Override
    public String getEventType() {
        return null;
    }


}
