/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.synpop.gis.DataLoader;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author johannes
 * 
 */
public class LandUseDataLoader implements DataLoader {

	private static final Logger logger = Logger.getLogger(LandUseDataLoader.class);
	
	public static final String KEY = "landuse";

	private final ConfigGroup module;

	public LandUseDataLoader(ConfigGroup module) {
		this.module = module;
	}

	@Override
	public Object load() {
		logger.info("Loading land use data...");
		LandUseData data = new LandUseData();
		data.setNuts1Layer(loadLayer("nuts1"));
		data.setNuts3Layer(loadLayer("nuts3"));
		data.setLau2Layer(loadLayer("lau2"));
		data.setModenaLayer(loadLayer("modena"));
		logger.info("Done.");
		return data;
	}

	public ZoneLayer<Map<String, Object>> loadLayer(String type) {
		Collection<? extends ConfigGroup> modules = module.getParameterSets(type);
		if (!modules.isEmpty()) {
			ConfigGroup m = modules.iterator().next();
			String file = m.getValue("file");
			String nameKey = m.getValue("namekey");
			String popKey = m.getValue("popkey");

			try {
				ZoneLayer<Map<String, Object>> zoneLayer = ZoneLayerSHP.read(file);

				for (Zone<Map<String, Object>> zone : zoneLayer.getZones()) {
					zone.getAttribute().put(LandUseData.NAME_KEY, zone.getAttribute().get(nameKey));
					Object value = zone.getAttribute().get(popKey);
					if(value != null) {
						double d = Double.parseDouble(value.toString());
						zone.getAttribute().put(LandUseData.POPULATION_KEY, d);
					}
				}

				return zoneLayer;

			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

}
