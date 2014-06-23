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

package playground.johannes.gsv.synPop.analysis;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class PopulationDensityTask implements ProxyAnalyzerTask {

	private ZoneLayer<Integer> zones;
	
	private GeometryFactory factory = new GeometryFactory();
	
	private String output;
	
	public PopulationDensityTask(Set<Geometry> geometries, String output) {
		this.output = output;
		
		Set<Zone<Integer>> zoneSet = new HashSet<Zone<Integer>>();
		for(Geometry geo : geometries) {
			zoneSet.add(new Zone<Integer>(geo));
		}
		
		zones = new ZoneLayer<Integer>(zoneSet);
	}
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.analysis.ProxyAnalyzerTask#analyze(java.util.Collection)
	 */
	@Override
	public void analyze(Collection<ProxyPerson> persons) {
		ProgressLogger.init(persons.size(), 1, 10);
		for(ProxyPerson person : persons) {
			double x = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
			double y = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
				
			Point p = factory.createPoint(new Coordinate(x, y));
			
			Zone<Integer> zone = zones.getZone(p);
			if(zone != null) {
			Integer val = zone.getAttribute();
			
			if(val == null) {
				zone.setAttribute(1);
			} else {
				zone.setAttribute(val + 1);
			}
			}
			ProgressLogger.step();
		}

		Set<Zone<Double>> newZones = new HashSet<Zone<Double>>();
		for(Zone<Integer> zone : zones.getZones()) {
			Zone<Double> newZone = new Zone<Double>(zone.getGeometry());
			if(zone.getAttribute() != null) {
				newZone.setAttribute(zone.getAttribute() / zone.getGeometry().getArea() * 1000 * 1000);
			} else {
				newZone.setAttribute(0.0);
			}
			newZones.add(newZone);
		}
		
		try {
			ZoneLayer<Double> layer = new ZoneLayer<Double>(newZones);
			layer.overwriteCRS(CRSUtils.getCRS(31467));
			ZoneLayerSHP.write(layer, output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
