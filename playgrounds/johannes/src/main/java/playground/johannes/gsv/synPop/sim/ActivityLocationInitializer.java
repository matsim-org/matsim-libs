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

package playground.johannes.gsv.synPop.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.opengis.kml._2.BalloonStyleType;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityOption;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.sna.util.ProgressLogger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * @author johannes
 *
 */
public class ActivityLocationInitializer implements Initializer {

	private final String blacklist;
	
	private final ZoneLayer<?> zoneLayer;
	
	private final Map<Zone<?>, Map<String, List<ActivityFacility>>> facilityMap;
	
	private final ActivityFacilities facilities;
	
	private final Random random;
	
	public ActivityLocationInitializer(ActivityFacilities facilities, ZoneLayer<?> zoneLayer, String blacklist, Random random) {
		this.blacklist = blacklist;
		this.zoneLayer = zoneLayer;
		this.facilities = facilities;
		this.random = random;
		facilityMap = new HashMap<Zone<?>, Map<String, List<ActivityFacility>>>();
		
		Quadtree quadtree = new Quadtree();
		for(ActivityFacility facility : facilities.getFacilities().values()) {
			Point p = MatsimCoordUtils.coordToPoint(facility.getCoord());
			quadtree.insert(p.getEnvelopeInternal(), facility);
		}
		
		
		ProgressLogger.init(zoneLayer.getZones().size(), 2, 10);
		for(Zone<?> zone : zoneLayer.getZones()) {
			List<ActivityFacility> results = quadtree.query(zone.getGeometry().getEnvelopeInternal());
			for(ActivityFacility facility : results) {
				Point p = MatsimCoordUtils.coordToPoint(facility.getCoord());
				PreparedGeometry geo = zone.getPreparedGeometry();
				if(geo.contains(p)) {
					Map<String, List<ActivityFacility>> map = facilityMap.get(zone);
					if(map == null) {
						map = new HashMap<String, List<ActivityFacility>>();
						facilityMap.put(zone, map);
					}
					
					for(ActivityOption opt : facility.getActivityOptions().values()) {
						List<ActivityFacility> list = map.get(opt.getType());
						if(list == null) {
							list = new ArrayList<ActivityFacility>(1000);
							map.put(opt.getType(), list);
						}
						list.add(facility);
					}
				}
			}
			
			ProgressLogger.step();
		}
	}
	
	@Override
	public void init(ProxyPerson person) {
		GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
		
		for(ProxyObject act : person.getPlan().getActivities()) {
			String id = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
			ActivityFacility facility = null;
			if (id != null) {
				facility = facilities.getFacilities().get(new IdImpl(id));
			}

			if (facility == null) {
				String type = (String) act.getAttribute(CommonKeys.ACTIVITY_TYPE);

				if (blacklist == null || !blacklist.equalsIgnoreCase(type)) {
					double x = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
					double y = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
						
					Point p = factory.createPoint(new Coordinate(x, y));
					Zone<?> zone = zoneLayer.getZone(p);
					Map<String, List<ActivityFacility>> map = facilityMap.get(zone);
					if(map == null) {
						map = facilityMap.entrySet().iterator().next().getValue(); // get random zone;
					}
					List<ActivityFacility> list = map.get(type);
					
					facility = list.get(random.nextInt(list.size()));
					act.setAttribute(CommonKeys.ACTIVITY_FACILITY, facility.getId().toString());
				}
			}

			act.setUserData(MutateActivityLocation.USER_DATA_KEY, facility);
		}
	}

}
