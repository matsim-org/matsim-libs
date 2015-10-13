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

package playground.johannes.gsv.synPop.mid.run;

import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class SpatialFacilityFilter {

	private static final Logger logger = Logger.getLogger(SpatialFacilityFilter.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
	
		logger.info("Loading facilities...");
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile(args[0]);
		
		ActivityFacilities facilities = scenario.getActivityFacilities();
		logger.info(String.format("Loaded %s facilities.", facilities.getFacilities().size()));
		
		ZoneLayer<Map<String, Object>> zoneLayer = ZoneLayerSHP.read(args[1]);
		Zone<?> hessen = null;
		for(Zone<Map<String, Object>> zone : zoneLayer.getZones()) {
			if("Hessen".equalsIgnoreCase((String) zone.getAttribute().get("GEN"))) {
				hessen = zone;
				break;
			}
		}
		
		List<Id<ActivityFacility>> toRemove = new ArrayList<Id<ActivityFacility>>(facilities.getFacilities().size());
		ProgressLogger.init(facilities.getFacilities().size(), 2, 10);
		for(ActivityFacility fac : facilities.getFacilities().values()) {
			Point p = MatsimCoordUtils.coordToPoint(fac.getCoord());
			if(!hessen.getGeometry().contains(p)) {
				toRemove.add(fac.getId());
			}
			ProgressLogger.step();
		}
		
		logger.info(String.format("Removing %s facilities...", toRemove.size()));
		
		for(Id<ActivityFacility> id : toRemove) {
			facilities.getFacilities().remove(id);
		}
		
		logger.info(String.format("Writing %s facilities...", facilities.getFacilities().size()));
		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write(args[2]);
		logger.info("Done.");

	}

}
