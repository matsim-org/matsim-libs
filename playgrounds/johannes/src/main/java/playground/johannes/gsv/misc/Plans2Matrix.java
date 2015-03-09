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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;

import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 * 
 */
public class Plans2Matrix {
	
	private static final Logger logger = Logger.getLogger(Plans2Matrix.class);

	public Matrix run(Collection<Plan> plans, ZoneLayer<?> zones, ActivityFacilities facilities) {
		Matrix m = new Matrix("1", null);

		for (Zone<?> zone1 : zones.getZones()) {
			String isocode1 = ((Map<String, Object>) zone1.getAttribute()).get("ISO_CODE").toString();
			if ("DE".equalsIgnoreCase(isocode1)) {
				for (Zone<?> zone2 : zones.getZones()) {
					String isocode2 = ((Map<String, Object>) zone2.getAttribute()).get("ISO_CODE").toString();
					if ("DE".equalsIgnoreCase(isocode2)) {
						String id1 = ((Map<String, Object>) zone1.getAttribute()).get("NO").toString();
						String id2 = ((Map<String, Object>) zone2.getAttribute()).get("NO").toString();
						m.createEntry(id1, id2, 0);
					}
				}
			}
		}

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

					Point origPoint = CRSUtils.transformPoint(MatsimCoordUtils.coordToPoint(origFac.getCoord()), transform);
					Point destPoint = CRSUtils.transformPoint(MatsimCoordUtils.coordToPoint(destFac.getCoord()), transform);
					Zone<?> origZone = zones.getZone(origPoint);
					Zone<?> destZone = zones.getZone(destPoint);

					if (origZone != null && destZone != null) {
						String origZoneId = ((Map<String, Object>) origZone.getAttribute()).get("NO").toString();
						String destZoneId = ((Map<String, Object>) destZone.getAttribute()).get("NO").toString();
						Entry e = m.getEntry(origZoneId, destZoneId);
						if (e == null) {
//							e = m.createEntry(origZoneId, destZoneId, 0);
							nullEntries++;
						}
						e.setValue(e.getValue() + 1);
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

		ZoneLayer<Map<String, Object>> zones = ZoneLayerSHP.read(args[2]);

		Set<Plan> plans = new HashSet<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			plans.add(person.getPlans().get(0));
		}

		Matrix m = new Plans2Matrix().run(plans, zones, scenario.getActivityFacilities());

		VisumMatrixWriter writer = new VisumMatrixWriter(m);
		writer.writeFile(args[3]);
	}
}
