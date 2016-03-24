/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.gis;

import com.vividsolutions.jts.geom.Geometry;
import playground.johannes.synpop.data.PlainElement;

/**
 * @author johannes
 */
public class Feature extends PlainElement {

    private final Geometry geometry;

    private final String id;

    public Feature(String id, Geometry geometry) {
        this.id = id;
        this.geometry = geometry;
    }

    public String getId() {
        return id;
    }

    public Geometry getGeometry() {
        return geometry;
    }
}
