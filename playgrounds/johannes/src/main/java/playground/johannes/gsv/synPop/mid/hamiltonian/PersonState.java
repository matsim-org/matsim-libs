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

package playground.johannes.gsv.synPop.mid.hamiltonian;

import java.util.Collection;
import java.util.Map;

import org.geotools.geometry.jts.JTSFactoryFinder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.gsv.synPop.sim.Hamiltonian;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;

/**
 * @author johannes
 *
 */
public class PersonState implements Hamiltonian {

	private final ZoneLayer<Map<String, Object>> states;
	
	private final GeometryFactory geoFactory = JTSFactoryFinder.getGeometryFactory(null);

	public PersonState(ZoneLayer<Map<String, Object>> states) {
		this.states = states;
	}
	
	@Override
	public double evaluate(ProxyPerson original, ProxyPerson modified) {
		Zone<Map<String, Object>> zone = (Zone<Map<String, Object>>) modified.getUserData(this);
		if(zone == null) {
			double x = Double.parseDouble((String) modified.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
			double y = Double.parseDouble((String) modified.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
			
			Point p = geoFactory.createPoint(new Coordinate(x, y));
			zone = states.getZone(p);
			modified.setUserData(this, zone);
		}
		
		if(zone == null)
			return Double.NEGATIVE_INFINITY;
		
		String thisName = (String) zone.getAttribute().get("NAME_1");
		String targetName = modified.getAttribute(MIDKeys.PERSON_STATE);
		
		if(thisName.equalsIgnoreCase(targetName)){
			return 0;
		} else {
			return -1;
		}
	}

	
@Override
	public double evaluate(Collection<ProxyPerson> persons) {
		double sum = 0;
		for(ProxyPerson person : persons) {
			double val = evaluate(null, person);
			if(!Double.isInfinite(val))
				sum += val; //FIXME
		}
		
		return sum;
	}

}
