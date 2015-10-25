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
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.gis.FacilityDataLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class FacilityZoneValidator {

	private static final Logger logger = Logger.getLogger(FacilityZoneValidator.class);
	
	public static void validate(DataPool dataPool, String type, int zoom) {
		FacilityData fData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
		LandUseData luData = (LandUseData) dataPool.get(LandUseDataLoader.KEY);
		ZoneLayer<?> zoneLayer = null;
		if(zoom == 3) {
			zoneLayer = luData.getNuts3Layer();
		} else if(zoom == 1) {
			zoneLayer = luData.getNuts1Layer();
		}
		
		Set<ActivityFacility> remove = new HashSet<>();
		List<ActivityFacility> facilities = fData.getFacilities(type); 
		for(ActivityFacility f : facilities) {
			Zone<?> zone = zoneLayer.getZone(MatsimCoordUtils.coordToPoint(f.getCoord()));
			if(zone == null) {
				remove.add(f);
			}
		}
		
		if(remove.size() > 0) {
			logger.info(String.format("Removing %s facilities that cannot be assigned to a zone.", remove.size()));
		}
		
		for(ActivityFacility f : remove) {
			facilities.remove(f);
		}
		
		
	}
}
