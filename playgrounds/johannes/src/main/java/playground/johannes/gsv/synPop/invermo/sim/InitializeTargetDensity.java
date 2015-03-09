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

package playground.johannes.gsv.synPop.invermo.sim;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.facilities.ActivityFacility;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.gsv.synPop.data.DataPool;
import playground.johannes.gsv.synPop.data.LandUseData;
import playground.johannes.gsv.synPop.data.LandUseDataLoader;
import playground.johannes.gsv.synPop.sim3.SwitchHomeLocation;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;

/**
 * @author johannes
 *
 */
public class InitializeTargetDensity implements ProxyPersonTask {

	private static final Logger logger = Logger.getLogger(InitializeTargetDensity.class);
	
	private final LandUseData landUseData;
	
	public InitializeTargetDensity(DataPool dataPool) {
		this.landUseData = (LandUseData) dataPool.get(LandUseDataLoader.KEY);
	}
	
	@Override
	public void apply(ProxyPerson person) {
		ActivityFacility home = (ActivityFacility) person.getUserData(SwitchHomeLocation.USER_FACILITY_KEY);
		ZoneLayer<Map<String, Object>> zoneLayer = landUseData.getNuts3Layer();
		Zone<Map<String, Object>> zone = zoneLayer.getZone(MatsimCoordUtils.coordToPoint(home.getCoord()));
		if(zone == null) {
			zone = zoneLayer.getZones().iterator().next();
			logger.debug("Zone not found. Drawing random zone.");
		}
	
		Double inhabs = (Double) zone.getAttribute().get(LandUseData.POPULATION_KEY);
		double area = zone.getGeometry().getArea();
		
		person.setUserData(PersonPopulationDenstiy.TARGET_DENSITY, inhabs/area);
	}

}
