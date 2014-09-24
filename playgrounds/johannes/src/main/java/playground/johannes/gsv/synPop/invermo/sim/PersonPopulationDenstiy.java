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

import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim2.Hamiltonian;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;

/**
 * @author johannes
 *
 */
public class PersonPopulationDenstiy implements Hamiltonian {

	public static final Object TARGET_DENSITY = new Object();
	
//	public static final Object ZONE_KEY =  new Object();
	
	private final ZoneLayer<Double> zoneLayer;
	
	public PersonPopulationDenstiy(ZoneLayer<Double> zoneLayer) {
		this.zoneLayer = zoneLayer;
	}
	
	@Override
	public double evaluate(ProxyPerson person) {
		ActivityFacility home = (ActivityFacility) person.getUserData(SwitchHomeLocations.HOME_FACIL_KEY);
		Zone<Double> zone = zoneLayer.getZone(MatsimCoordUtils.coordToPoint(home.getCoord()));
		if(zone == null) {
			return Double.NEGATIVE_INFINITY;
		}
		
		Double target = (Double) person.getUserData(TARGET_DENSITY);
		
		double diff = Math.abs(zone.getAttribute() - target);
				 
		return diff;
	}

}
