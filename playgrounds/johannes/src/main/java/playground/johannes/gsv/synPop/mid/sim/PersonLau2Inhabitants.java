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

package playground.johannes.gsv.synPop.mid.sim;

import org.matsim.facilities.ActivityFacility;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.data.DataPool;
import playground.johannes.gsv.synPop.data.LandUseData;
import playground.johannes.gsv.synPop.data.LandUseDataLoader;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.gsv.synPop.mid.PersonMunicipalityClassHandler;
import playground.johannes.gsv.synPop.sim3.Hamiltonian;
import playground.johannes.gsv.synPop.sim3.SwitchHomeLocation;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.synpop.data.PlainPerson;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author johannes
 *
 */
public class PersonLau2Inhabitants implements Hamiltonian {
	
	private static final Object USER_DAT_KEY = new Object();
	
	private final Map<ActivityFacility, Integer> inhabitants;
	
	private final LandUseData landUseData;
	
	public PersonLau2Inhabitants(DataPool dataPool) {
		landUseData = (LandUseData) dataPool.get(LandUseDataLoader.KEY);
		inhabitants = new ConcurrentHashMap<ActivityFacility, Integer>();
	}
	
	@Override
	public double evaluate(PlainPerson person) {
		ActivityFacility home = (ActivityFacility) person.getUserData(SwitchHomeLocation.USER_FACILITY_KEY);
		Integer inhabs = inhabitants.get(home);
		if(inhabs == null) {
			inhabs = attachInhabitants(home);
		}
		
		if(inhabs == null) {
			return Double.POSITIVE_INFINITY;
		}
		
		
		Integer intObj = (Integer) person.getUserData(USER_DAT_KEY);
		if(intObj == null) {
			intObj = new Integer(person.getAttribute(MIDKeys.PERSON_MUNICIPALITY_CLASS));
			person.setUserData(USER_DAT_KEY, intObj);
		}
		
		double target = intObj;
		int cat = PersonMunicipalityClassHandler.getCategory(inhabs);
		
		double err = Math.abs(target - cat);
		
		return err;
	}

	private synchronized Integer attachInhabitants(ActivityFacility home) {
		ZoneLayer<Map<String, Object>> zoneLayer = landUseData.getLau2Layer();
		Zone<Map<String, Object>> zone = zoneLayer.getZone(MatsimCoordUtils.coordToPoint(home.getCoord()));
		
		if(zone == null) {
			return null;
		} else {
			Integer inhabs = ((Double)zone.getAttribute().get(LandUseData.POPULATION_KEY)).intValue();
			inhabitants.put(home, inhabs);
			return inhabs;
		}
	}
}
