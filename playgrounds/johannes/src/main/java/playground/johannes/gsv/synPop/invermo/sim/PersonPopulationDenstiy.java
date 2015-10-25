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

import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.gsv.synPop.data.LandUseData;
import playground.johannes.gsv.synPop.data.LandUseDataLoader;
import playground.johannes.gsv.synPop.sim3.Hamiltonian;
import playground.johannes.gsv.synPop.sim3.SwitchHomeLocation;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.gis.DataPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author johannes
 *
 */
public class PersonPopulationDenstiy implements Hamiltonian {

	public static final Object TARGET_DENSITY = new Object();
	
	private final LandUseData landUseData;
	
	private final Map<ActivityFacility, Double> densities;
	
	public PersonPopulationDenstiy(DataPool dataPool) {
		landUseData = (LandUseData) dataPool.get(LandUseDataLoader.KEY);
		densities = new ConcurrentHashMap<>();
	}
	
	@Override
	public double evaluate(Person person) {
		ActivityFacility home = (ActivityFacility) ((PlainPerson)person).getUserData(SwitchHomeLocation
				.USER_FACILITY_KEY);
		Double density = densities.get(home);
		if(density == null) {
			attachDensity(home);
			density = densities.get(home);
		}
		
		if(Double.isNaN(density)) {
			return Double.POSITIVE_INFINITY;
		}
		
		Double target = (Double) ((PlainPerson)person).getUserData(TARGET_DENSITY);
		
		return Math.abs(density - target)/density;
	}
	
	private void attachDensity(ActivityFacility home) {
		ZoneLayer<Map<String, Object>> zoneLayer = landUseData.getNuts3Layer();
		Zone<Map<String, Object>> zone = zoneLayer.getZone(MatsimCoordUtils.coordToPoint(home.getCoord()));
		
		if(zone == null) {
			densities.put(home, Double.NaN);
		} else {
			Double inhabs = (Double)zone.getAttribute().get(LandUseData.POPULATION_KEY);
			double area = zone.getGeometry().getArea();
			
			densities.put(home, inhabs/area);
		}
	}

}
