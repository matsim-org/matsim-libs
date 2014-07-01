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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.resources.CRSUtilities;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim.Hamiltonian;
import playground.johannes.gsv.synPop.sim.Initializer;
import playground.johannes.gsv.synPop.sim.SamplerListener;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.io.ZoneLayerKMLWriter;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 *
 */
public class PopulationDensity implements Hamiltonian, Initializer, SamplerListener {
	
	private static final Logger logger = Logger.getLogger(PopulationDensity.class);

//	public static final String PERSON_HOME_ZONE = "homeZone";
	
	private ZoneLayer<ZoneData> zones;
	
	private GeometryFactory factory = new GeometryFactory();
	
	public PopulationDensity(ZoneLayer<Double> inhabitants, int N, Random random) {
		logger.info("Initializing population density hamiltonian...");
		double maxFrac = 0;
		Set<Zone<ZoneData>> newZones = new HashSet<Zone<ZoneData>>(inhabitants.getZones().size());
		for(Zone<Double> zone : inhabitants.getZones()) {
			Zone<ZoneData> newZone = new Zone<ZoneData>(zone.getGeometry());
			newZone.setAttribute(new ZoneData());
			newZone.getAttribute().targetFraction = zone.getAttribute();
			
			newZones.add(newZone);
			
			maxFrac = Math.max(maxFrac, newZone.getAttribute().targetFraction);
		}
		
		zones = new ZoneLayer<PopulationDensity.ZoneData>(newZones);
		
		List<Zone<ZoneData>> zoneList = new ArrayList<Zone<ZoneData>>(zones.getZones());
		int total = 0;
		ProgressLogger.init(N, 2, 10);
		while(total < N) {
			Zone<ZoneData> zone = zoneList.get(random.nextInt(zoneList.size()));
			double p = zone.getAttribute().targetFraction / maxFrac;
			if(p > random.nextDouble()) {
				zone.getAttribute().target++;
				total++;
				ProgressLogger.step();
			}
		}
		ProgressLogger.termiante();
		
//		writeZoneData("/home/johannes/gsv/mid2008/targetPopDen.shp");
	}
	
	@Override
	public double evaluate(ProxyPerson original, ProxyPerson modified) {
		Zone<ZoneData> modZone = getZone(modified);
		
		if(modZone == null)
			return - Double.MAX_VALUE;
		
		Zone<ZoneData> origZone = getZone(original);
		
		double gradient1 = gradient(origZone, -1);
		double gradient2 = gradient(modZone, 1);
		
		return -(gradient1 + gradient2);
	}
	
	private Zone<ZoneData> getZone(ProxyPerson person) {
		Zone<ZoneData> modZone = (Zone<ZoneData>) person.getUserData(this);
		if(modZone == null) {
			double x = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
			double y = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
				
			Point p = factory.createPoint(new Coordinate(x, y));
			
			modZone = zones.getZone(p);
			person.setUserData(this, modZone);
		}
		return modZone;
	}

	private double gradient(Zone<ZoneData> zone, int diff) {
		
		int delta1 = Math.abs(zone.getAttribute().current - zone.getAttribute().target);
		int delta2 = Math.abs(zone.getAttribute().current - zone.getAttribute().target + diff);
		int delta3 = delta2 - delta1;
		int target = 1;//Math.max(zone.getAttribute().target, 1);
		if(delta3 < 0) {
			return -1/(double)target;
		} else if(delta3 > 0) {
			return 1/(double)target;
		} else {
			return 0;
		}
	}
	
	@Override
	public double evaluate(Collection<ProxyPerson> persons) {
		double sum = 0;
		for(Zone<ZoneData> zone : zones.getZones()) {
			sum -= Math.abs(zone.getAttribute().current - zone.getAttribute().target);
		}
		
		sum = sum/zones.getZones().size();
//		for(ProxyPerson person : persons) {
////			Point point = (Point) person.getAttribute(CommonKeys.PERSON_HOME_POINT);
////			Zone<ZoneData> zone = zones.getZone(point);
//			
//			Zone<ZoneData> zone = (Zone<ZoneData>) person.getUserData(this);
//			sum -= Math.abs(zone.getAttribute().current - zone.getAttribute().target);
//			
//		}
		
		return sum;
	}

	@Override
	public void init(ProxyPerson person) {
		double x = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
		double y = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
			
		Point p = factory.createPoint(new Coordinate(x, y));
		Zone<ZoneData> zone = zones.getZone(p);
		if(zone == null) {
			zone = zones.getZones().iterator().next(); //get a random zone;
//			person.setAttribute(CommonKeys.PERSON_HOME_POINT, zone.getGeometry().getCentroid());
			Coordinate coord = zone.getGeometry().getCentroid().getCoordinate();
			person.setAttribute(CommonKeys.PERSON_HOME_COORD_X, String.valueOf(coord.x));
			person.setAttribute(CommonKeys.PERSON_HOME_COORD_Y, String.valueOf(coord.y));
		}
		person.setUserData(this, zone);
		zone.getAttribute().current++;
		
	}

	@Override
	public void afterStep(Collection<ProxyPerson> population, ProxyPerson original, ProxyPerson mutation, boolean accepted) {
		if(accepted) {
			Zone<ZoneData> zone1 = (Zone<ZoneData>) original.getUserData(this);
			Zone<ZoneData> zone2 = (Zone<ZoneData>) mutation.getUserData(this);
			
			zone1.getAttribute().current--;
			zone2.getAttribute().current++;
		}
	}

	private static class ZoneData {
		
		private int current;
		
		private int target;
		
		private double targetFraction;
	
	}
	
	public void writeZoneData(String file) {
		try {
			Set<Zone<Double>> newZones = new HashSet<Zone<Double>>();
			for(Zone<ZoneData> zone : zones.getZones()) {
				Zone<Double> newZone = new Zone<Double>(zone.getGeometry());
//				newZone.setAttribute((double) zone.getAttribute().current/zone.getGeometry().getArea() * 1000000);
				newZone.setAttribute((double) (zone.getAttribute().target - zone.getAttribute().current));
				newZones.add(newZone);
			}
			
			ZoneLayer<Double> newLayer = new ZoneLayer<Double>(newZones);
			newLayer.overwriteCRS(CRSUtils.getCRS(31467));
			ZoneLayerSHP.write(newLayer, file);
			
//			ZoneLayerKMLWriter writer =	new ZoneLayerKMLWriter();
//			writer.writeWithColor(newLayer, file);
		} catch (IOException e) {
//			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
