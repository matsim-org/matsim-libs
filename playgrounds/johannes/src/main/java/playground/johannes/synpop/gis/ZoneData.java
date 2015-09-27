/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.synpop.gis;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jillenberger
 */
public class ZoneData {

    public static final String POPULATION_KEY = "population";

    public static final String NAME_KEY = "name";

    private final Map<String, ZoneCollection> layers;

    public ZoneData() {
        layers = new HashMap<>();
    }

    public ZoneCollection getLayer(String name) {
        return layers.get(name);
    }

    ZoneCollection addLayer(ZoneCollection zones, String name) {
        return layers.put(name, zones);
    }
}
