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

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim.Initializer;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;

/**
 * @author johannes
 *
 */
public class InitializeTargetDensity implements Initializer {

	private static final Logger logger = Logger.getLogger(InitializeTargetDensity.class);
	
	private final ZoneLayer<Double> zoneLayer;
	
	public InitializeTargetDensity(ZoneLayer<Double> zoneLayer) {
		this.zoneLayer = zoneLayer;
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Initializer#init(playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public void init(ProxyPerson person) {
		ActivityFacility startFacility = (ActivityFacility) person.getUserData(SwitchHomeLocations.HOME_FACIL_KEY);
		Zone<Double> zone = zoneLayer.getZone(MatsimCoordUtils.coordToPoint(startFacility.getCoord()));
		if(zone == null) {
			zone = zoneLayer.getZones().iterator().next();
			logger.warn("Zone not found. Drawing random zone.");
		}
		
		person.setUserData(PersonPopulationDenstiy.TARGET_DENSITY, zone.getAttribute());
	}

}
