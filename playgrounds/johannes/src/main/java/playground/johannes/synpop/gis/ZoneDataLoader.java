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

import org.matsim.core.config.ConfigGroup;

import java.io.IOException;
import java.util.Collection;

/**
 * @author jillenberger
 */
public class ZoneDataLoader implements DataLoader {

    public final static String KEY = "zoneData";

    private final static String LAYERNAME_PARAM = "layer";

    private final static String FILE_PARAM = "file";

    private final static String PRIMARY_ZONE_KEY_PARAM = "primaryZoneKey";

    private final static String NAME_KEY_PARAM = "nameKey";

    private final static String POPULATION_KEY_PARAM = "populationKey";

    private final ConfigGroup module;

    public ZoneDataLoader(ConfigGroup module) {
        this.module = module;
    }

    @Override
    public Object load() {
        ZoneData data = new ZoneData();

        Collection<? extends ConfigGroup> modules = module.getParameterSets(KEY);
        for(ConfigGroup paramset : modules) {
            String layerName = paramset.getValue(LAYERNAME_PARAM);
            String file = paramset.getValue(FILE_PARAM);
            String primaryKey = paramset.getValue(PRIMARY_ZONE_KEY_PARAM);
            String nameKey = paramset.getValue(NAME_KEY_PARAM);
            String populationKey = paramset.getValue(POPULATION_KEY_PARAM);

            try {
                ZoneCollection zones;
                if(file.endsWith(".json") || file.endsWith(".geojson"))
                    zones = ZoneGeoJsonIO.readFromGeoJSON(file, primaryKey, layerName);
                else if(file.endsWith(".shp")) {
                    zones = ZoneEsriShapeIO.read(file);
                    zones.setPrimaryKey(primaryKey);
                } else {
                    throw new RuntimeException("Unknown file format.");
                }

                for(Zone zone : zones.getZones()) {
                    zone.setAttribute(ZoneData.POPULATION_KEY, zone.getAttribute(populationKey));
                    zone.setAttribute(ZoneData.NAME_KEY, zone.getAttribute(nameKey));
                }

                data.addLayer(zones, layerName);

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return data;
    }
}
