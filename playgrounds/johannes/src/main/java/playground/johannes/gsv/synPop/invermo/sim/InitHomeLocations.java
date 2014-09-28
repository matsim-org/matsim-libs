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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.sim.Initializer;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.sna.util.ProgressLogger;

/**
 * @author johannes
 *
 */
public class InitHomeLocations implements Initializer {

	private static final Logger logger = Logger.getLogger(InitHomeLocations.class);
	
	public InitHomeLocations(Collection<ProxyPerson> persons, ZoneLayer<Double> zoneLayer, FacilityData facilities, Random random) {
		double max = Double.MIN_VALUE;
		
		for(Zone<Double> zone : zoneLayer.getZones()) {
			max = Math.max(max, zone.getAttribute());
		}
		
		int N = persons.size();
		
		
		List<ActivityFacility> facilList = facilities.getFacilities("home");
		Map<Zone<?>, List<ActivityFacility>> facilMap = new HashMap<Zone<?>, List<ActivityFacility>>();
		int notfound = 0;
		ProgressLogger.init(facilList.size(), 2, 10);
		for(ActivityFacility facil : facilList) {
			Zone<?> zone = zoneLayer.getZone(MatsimCoordUtils.coordToPoint(facil.getCoord()));
			if(zone != null) {
				List<ActivityFacility> list = facilMap.get(zone);
				if(list == null) {
					list = new ArrayList<>(facilList.size() / zoneLayer.getZones().size() * 2);
					facilMap.put(zone, list);
				}
				
				list.add(facil);
			} else {
				notfound++;
			}
			ProgressLogger.step();
		}
		
		ProgressLogger.termiante();
		
		if(notfound > 0) {
			logger.warn(String.format("Ignored %s facilities because they can not assigned to a zone.", notfound));
		}
		
		List<Zone<Double>> zoneList = new ArrayList<Zone<Double>>(zoneLayer.getZones());
//		int total = 0;
		
		ProgressLogger.init(N, 2, 10);
		Queue<ProxyPerson> personQueue = new LinkedList<>(persons);
		while (!personQueue.isEmpty()) {
			Zone<Double> zone = zoneList.get(random.nextInt(zoneList.size()));
			double p = zone.getAttribute()/ max;
			if (p > random.nextDouble()) {
				ProxyPerson person = personQueue.poll();
				List<ActivityFacility> list = facilMap.get(zone);
				ActivityFacility facil = list.get(random.nextInt(list.size()));
				
				person.setUserData(SwitchHomeLocations.HOME_FACIL_KEY, facil);
				ProgressLogger.step();
			}
		}
		ProgressLogger.termiante();
	}
	
	@Override
	public void init(ProxyPerson person) {
		// do nothing

	}

}
