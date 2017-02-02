package playground.gregor.gis.cutoutnetwork;/* *********************************************************************** *
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;

import java.util.function.Predicate;

public class BountingBoxFilter implements Predicate<Node> {


    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;

    public BountingBoxFilter(double minX, double maxX, double minY, double maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public boolean test(Node n) {
        Coord coord = n.getCoord();
        return !(coord.getX() < minX || coord.getX() > maxX || coord.getY() < minY || coord.getY() > maxY);
    }
}
