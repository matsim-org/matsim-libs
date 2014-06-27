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

package playground.johannes.gsv.synPop.mid;

import java.util.Collection;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim.Hamiltonian;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class HPersonMunicipality implements Hamiltonian {

	private ZoneLayer<Double> municipalities;
	
	private GeometryFactory geoFactory = new GeometryFactory();
	
	public HPersonMunicipality(ZoneLayer<Double> municipalities) {
		this.municipalities = municipalities;
	}
	
	@Override
	public double evaluate(ProxyPerson original, ProxyPerson modified) {
		return eval(modified) - eval(original);
	}
	
	private double eval(ProxyPerson person) {
		Zone<Double> zone = (Zone<Double>) person.getUserData(this);
		if(zone == null) {
			double x = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
			double y = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
				
			Point p = geoFactory.createPoint(new Coordinate(x, y));
			zone = municipalities.getZone(p);
			person.setUserData(this, zone);
		}
		
		if(zone == null)
			return Double.NEGATIVE_INFINITY;
		
		double inhabs = zone.getAttribute();
		
		
		Integer intObj = (Integer) person.getUserData(MIDKeys.PERSON_MUNICIPALITY_CLASS);
		if(intObj == null) {
			intObj = new Integer(person.getAttribute(MIDKeys.PERSON_MUNICIPALITY_CLASS));
			person.setUserData(MIDKeys.PERSON_MUNICIPALITY_CLASS, intObj);
		}
		int target = intObj;
		int cat = PersonMunicipalityClassHandler.getCategorie((int) inhabs);
		if(target == cat)
			return 0;
		else
			return -1;
	}

	@Override
	public double evaluate(Collection<ProxyPerson> persons) {
		double sum = 0;
		for(ProxyPerson person : persons) {
			sum += eval(person);
		}
		
		return sum;
	}


}
