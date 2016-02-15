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
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author johannes
 * 
 */
public class PersonNuts1Name implements Hamiltonian {

	private final Map<ActivityFacility, String> nuts1Names;
	
	private final LandUseData landUseData;
	
	public PersonNuts1Name(DataPool dataPool) {
		this.landUseData = (LandUseData) dataPool.get(LandUseDataLoader.KEY);
		nuts1Names = new ConcurrentHashMap<ActivityFacility, String>();
	}

	@Override
	public double evaluate(Person person) {
		ActivityFacility home = (ActivityFacility) ((PlainPerson)person).getUserData(SwitchHomeLocation
				.USER_FACILITY_KEY);
		String name = nuts1Names.get(home);
		
		if(name == null) {
			name = attachName(home);
		}
		
		if(name == null) {
			return Double.POSITIVE_INFINITY;
		}
		
		String targetName = person.getAttribute(MiDKeys.PERSON_NUTS1);

		if (name.equalsIgnoreCase(targetName)) {
			return 0;
		} else {
			return 1;
		}
	}
	
	public synchronized String attachName(ActivityFacility home) {
		ZoneLayer<Map<String, Object>> nuts1Layer = landUseData.getNuts1Layer();
		Zone<Map<String, Object>> zone = nuts1Layer.getZone(MatsimCoordUtils.coordToPoint(home.getCoord()));
		
		if(zone == null) {
			return null;
		} else {
			String name = (String) zone.getAttribute().get(LandUseData.NAME_KEY);
			nuts1Names.put(home, name);
			return name;
		}
	}

}
