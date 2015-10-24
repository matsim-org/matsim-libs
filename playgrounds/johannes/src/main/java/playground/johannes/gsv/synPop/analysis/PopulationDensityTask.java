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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.synpop.data.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class PopulationDensityTask extends AnalyzerTask {
	
	private static final Logger logger = Logger.getLogger(PopulationDensityTask.class);

	private ZoneLayer<Integer> zones;
	
	private GeometryFactory factory = new GeometryFactory();
	
	private String output;
	
	private final ActivityFacilities facilities;
	
	public PopulationDensityTask(Set<Geometry> geometries, ActivityFacilities facilities, String output) {
		this.output = output;
		this.facilities = facilities;
		
		Set<Zone<Integer>> zoneSet = new HashSet<Zone<Integer>>();
		for(Geometry geo : geometries) {
			zoneSet.add(new Zone<Integer>(geo));
		}
		
		zones = new ZoneLayer<Integer>(zoneSet);
	}
	
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.analysis.AnalyzerTask#analyze(java.util.Collection, java.util.Map)
	 */
	@Override
	public void analyze(Collection<? extends Person> persons, Map<String, DescriptiveStatistics> results) {
		ProgressLogger.init(persons.size(), 1, 10);

		int nozone = 0;
		
		for(Person person : persons) {
			Episode plan = person.getEpisodes().get(0);
			
			ActivityFacility home = null;
			for(Attributable act : plan.getActivities()) {
				if(ActivityTypes.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
					String idStr = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
					Id<ActivityFacility> id = Id.create(idStr, ActivityFacility.class);
					home = facilities.getFacilities().get(id);
					break;
				}
			}
			
			if (home != null) {
				Point p = MatsimCoordUtils.coordToPoint(home.getCoord());

				Zone<Integer> zone = zones.getZone(p);
				if (zone != null) {
					Integer val = zone.getAttribute();

					if (val == null) {
						zone.setAttribute(1);
					} else {
						zone.setAttribute(val + 1);
					}
				}
			}
			ProgressLogger.step();
		}
		
		if(nozone > 0) {
			logger.warn(String.format("%s home locations cound not be assigned to a zone.", nozone));
		}

		Set<Zone<Double>> newZones = new HashSet<Zone<Double>>();
		for(Zone<Integer> zone : zones.getZones()) {
			Zone<Double> newZone = new Zone<Double>(zone.getGeometry());
			if(zone.getAttribute() != null) {
				newZone.setAttribute(zone.getAttribute() / (double)persons.size());
			} else {
				newZone.setAttribute(0.0);
			}
			newZones.add(newZone);
		}
		
//		try {
			ZoneLayer<Double> layer = new ZoneLayer<Double>(newZones);
			layer.overwriteCRS(CRSUtils.getCRS(31467));
			try {
				ZoneLayerSHP.write(layer, output+"/popden.shp");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			new ZoneLayerKMLWriter().writeWithColor(layer, output);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}

}
