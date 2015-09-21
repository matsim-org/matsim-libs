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
package playground.johannes.gsv.synPop.data;

import org.matsim.core.config.ConfigGroup;

import java.util.Collection;

/**
 * @author jillenberger
 */
public class ZoneDataLoader implements DataLoader {

    private final static String PARAMSET_TYPE = "zoneData";

    private final ConfigGroup module;

    public ZoneDataLoader(ConfigGroup module) {
        this.module = module;
    }

    @Override
    public Object load() {
        ZoneData data = new ZoneData();

        Collection<? extends ConfigGroup> modules = module.getParameterSets(PARAMSET_TYPE);
        for(ConfigGroup paramset : modules) {
            String layerName = paramset.getName();
            String file = paramset.getValue("file");
            String key = paramset.getValue("primaryKey");
            String nameKey = paramset.getValue("namekey");
            String popKey = paramset.getValue("popkey");

//            try {
//                ZoneCollection zones = ZoneCollection.readFromGeoJSON(file, key);
//
//                for (Zone<Map<String, Object>> zone : zoneLayer.getZones()) {
//                    zone.getAttribute().put(LandUseData.NAME_KEY, zone.getAttribute().get(nameKey));
//                    Object value = zone.getAttribute().get(popKey);
//                    if(value != null) {
//                        double d = Double.parseDouble(value.toString());
//                        zone.getAttribute().put(LandUseData.POPULATION_KEY, d);
//                    }
//                }
//
//                return zoneLayer;
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                return null;
//            }
//        } else {
//            return null;
        }
        return null;
    }
}
