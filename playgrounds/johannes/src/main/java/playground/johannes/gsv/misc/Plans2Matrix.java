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

package playground.johannes.gsv.misc;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLWriter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class Plans2Matrix {
	
	private static final Logger logger = Logger.getLogger(Plans2Matrix.class);

	public NumericMatrix run(Collection<Plan> plans, ZoneCollection zones, ActivityFacilities facilities, String zoneIdKey) {
		NumericMatrix m = new NumericMatrix();
//		Set<Zone> getZones = zones.getZones();
//		for(Zone zone1 : getZones) {
//			String isocode1 = zone1.getAttribute("ISO_CODE");
//			if ("DE".equalsIgnoreCase(isocode1)) {
//				for (Zone zone2 : getZones) {
//					String isocode2 = zone2.getAttribute("ISO_CODE");
//					if ("DE".equalsIgnoreCase(isocode2)) {
//						String id1 = zone1.getAttribute("NO");
//						String id2 = zone2.getAttribute("NO");
//						
////						m.createEntry(id1, id2, 0);
//					}
//				}
//			}
//		}

		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
		} catch (FactoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int noZones = 0;
		int legs = 0;
		int trips = 0;
		int nullEntries = 0;
		
		ProgressLogger.init(plans.size(), 2, 10);
		for (Plan plan : plans) {
			for (int i = 1; i < plan.getPlanElements().size(); i += 2) {
				Leg leg = (Leg) plan.getPlanElements().get(i);
				legs++;
				if (leg.getMode().equalsIgnoreCase("car")) {
					trips++;
					Activity orig = (Activity) plan.getPlanElements().get(i - 1);
					Activity dest = (Activity) plan.getPlanElements().get(i + 1);

					Id<ActivityFacility> origId = Id.create(orig.getFacilityId(), ActivityFacility.class);
					ActivityFacility origFac = facilities.getFacilities().get(origId);

					Id<ActivityFacility> destId = Id.create(dest.getFacilityId(), ActivityFacility.class);
					ActivityFacility destFac = facilities.getFacilities().get(destId);

//					Point origPoint = CRSUtils.transformPoint(MatsimCoordUtils.coordToPoint(origFac.getCoord()), transform);
//					Point destPoint = CRSUtils.transformPoint(MatsimCoordUtils.coordToPoint(destFac.getCoord()), transform);
					Zone origZone = zones.get(new Coordinate(origFac.getCoord().getX(), origFac.getCoord().getY()));
					Zone destZone = zones.get(new Coordinate(destFac.getCoord().getX(), destFac.getCoord().getY()));

					if (origZone != null && destZone != null) {
						String origZoneId = origZone.getAttribute(zoneIdKey);
						String destZoneId = destZone.getAttribute(zoneIdKey);
						Double val = m.get(origZoneId, destZoneId);
						if (val == null) val = 0.0;
						val++;
						m.set(origZoneId, destZoneId, val);
					} else {
						noZones++;
					}
				}
			}
			ProgressLogger.step();
		}

		if(noZones > 0)
			logger.info(String.format("%s activities could not be located in a zone.", noZones));
		logger.info(String.format("%s null entries (non DE?)", nullEntries));
		logger.info(String.format("Processed %s legs.", legs));
		logger.info(String.format("Processed %s car trips.", trips));

		return m;
	}

	public static void main(String args[]) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(args[0]);

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(args[1]);

		ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(args[2], args[3], null);

		Set<Plan> plans = new HashSet<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			plans.add(person.getPlans().get(0));
		}

		NumericMatrix m = new Plans2Matrix().run(plans, zones, scenario.getActivityFacilities(), args[3]);

		NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
		writer.write(m, args[4]);
	}
}
