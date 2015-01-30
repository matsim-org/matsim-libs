/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim.foreign;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 * 
 */
public class PopulationGenerator {

	private static final Logger logger = Logger.getLogger(PopulationGenerator.class);

	private static final Random random = new XORShiftRandom();

	private static final double scaleFactor = 11*365;

	private static Map<Zone, List<ActivityFacility>> facilityMap;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String zonesFile = args[0];
		String facilitiesFile = args[1];
		String matrixDir = args[2];
		File dir = new File(matrixDir);
		String[] matrixFiles = dir.list();
		
		String outFile = args[3];
		/*
		 * load zones
		 */
		logger.info("Loading geometries...");
		String data = new String(Files.readAllBytes(Paths.get(zonesFile)));
		Set<Zone> zones = Zone2GeoJSON.parseFeatureCollection(data);
		ZoneCollection zoneCollection = new ZoneCollection();
		zoneCollection.addAll(zones);

		Set<Zone> deZones = new HashSet<>();
		Set<Zone> euZones = new HashSet<>();
		for (Zone zone : zones) {
			if (zone.getAttribute("NUTS0_CODE").equalsIgnoreCase("DE")) {
				deZones.add(zone);
			} else {
				euZones.add(zone);
			}
		}
		ZoneCollection deZoneLayer = new ZoneCollection();
		deZoneLayer.addAll(deZones);
		/*
		 * load facilities
		 */
		logger.info("Loading facilities...");
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MatsimFacilitiesReader fReader = new MatsimFacilitiesReader(scenario);
		fReader.readFile(facilitiesFile);
		logger.info("Assigning facilities to zones...");
		facilityMap = assignFacilities(zoneCollection, scenario.getActivityFacilities());
		/*
		 * load matrix
		 */
		Set<ProxyPerson> persons = new HashSet<>();

		for (String file : matrixFiles) {
			String type = file.split("\\.")[1];
			
			logger.info(String.format("Loading %s matrix...", type));
			
			Matrix m = new Matrix("modena", null);
			VisumMatrixReader mReader = new VisumMatrixReader(m);
			mReader.readFile(matrixDir + file);
			/*
			 * create persons
			 */
			logger.info("Creating persons...");
			ProgressLogger.init(euZones.size(), 2, 10);
			for (Zone zone : euZones) {
				ActivityFacility euFac = randomFacility(zone);
				String id = zone.getAttribute("CODE");
				/*
				 * incoming
				 */
				List<Entry> entries = m.getFromLocEntries(id);
				for (Entry entry : entries) {
					double volume = entry.getValue() / scaleFactor;
					volume = Math.round(volume);
					if (volume > 0) {
						Zone deZone = deZoneLayer.get(entry.getFromLocation());

						for (int i = 0; i < volume; i++) {
							ActivityFacility deFac = randomFacility(deZone);
							ProxyPerson person = buildPerson(String.format("foreign%s.%s.0", id, i), deFac, euFac, ActivityType.HOME, type);
							persons.add(person);
						}
					}
				}
				/*
				 * outgoing
				 */
				entries = m.getToLocEntries(id);
				for (Entry entry : entries) {
					double volume = entry.getValue() / scaleFactor;
					volume = Math.round(volume);
					if (volume > 0) {
						Zone deZone = deZoneLayer.get(entry.getToLocation());

						for (int i = 0; i < volume; i++) {
							ActivityFacility deFac = randomFacility(deZone);
							ProxyPerson person = buildPerson(String.format("foreign%s.%s.1", id, i), euFac, deFac, type, ActivityType.HOME);
							persons.add(person);
						}
					}
				}
				ProgressLogger.step();
			}
			ProgressLogger.termiante();
		}

		logger.info(String.format("Created %s persons.", persons.size()));
		
		logger.info("Writing persons...");
		XMLWriter writer = new XMLWriter();
		writer.write(outFile, persons);
		logger.info("Done.");
	}

	private static Map<Zone, List<ActivityFacility>> assignFacilities(ZoneCollection zones, ActivityFacilities facilities) {
		Map<Zone, Set<ActivityFacility>> setMap = new HashMap<Zone, Set<ActivityFacility>>();

		int cnt = 0;
		ProgressLogger.init(facilities.getFacilities().size(), 2, 10);
		for (ActivityFacility f : facilities.getFacilities().values()) {
			Zone zone = zones.get(MatsimCoordUtils.coordToPoint(f.getCoord()).getCoordinate());
			if (zone != null) {
				Set<ActivityFacility> set = setMap.get(zone);
				if (set == null) {
					set = new HashSet<>();
					setMap.put(zone, set);
				}
				set.add(f);
			} else {
				cnt++;
			}
			ProgressLogger.step();
		}
		ProgressLogger.termiante();
		if (cnt > 0) {
			logger.info(String.format("%s facilities cannot be assigned to a zone.", cnt));
		}

		Map<Zone, List<ActivityFacility>> listMap = new HashMap<>(setMap.size());
		for (java.util.Map.Entry<Zone, Set<ActivityFacility>> entry : setMap.entrySet()) {
			listMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		return listMap;
	}

	private static ActivityFacility randomFacility(Zone zone) {
		List<ActivityFacility> list = facilityMap.get(zone);
		return list.get(random.nextInt(list.size()));
	}

	private static ProxyPerson buildPerson(String id, ActivityFacility orig, ActivityFacility target, String origType, String targetType) {
		ProxyPerson person = new ProxyPerson(id);

		ProxyPlan plan = new ProxyPlan();
		plan.setAttribute(CommonKeys.DATA_SOURCE, "foreign");

		ProxyObject origAct = new ProxyObject();
		origAct.setAttribute(CommonKeys.ACTIVITY_TYPE, origType);
		origAct.setAttribute(CommonKeys.ACTIVITY_FACILITY, orig.getId().toString());
		origAct.setAttribute(CommonKeys.ACTIVITY_START_TIME, "0");
		origAct.setAttribute(CommonKeys.ACTIVITY_END_TIME, "1");

		ProxyObject leg = new ProxyObject();
		leg.setAttribute(CommonKeys.LEG_MODE, "car");

		ProxyObject targetAct = new ProxyObject();
		targetAct.setAttribute(CommonKeys.ACTIVITY_TYPE, targetType);
		targetAct.setAttribute(CommonKeys.ACTIVITY_FACILITY, target.getId().toString());
		targetAct.setAttribute(CommonKeys.ACTIVITY_START_TIME, "86399");
		targetAct.setAttribute(CommonKeys.ACTIVITY_END_TIME, "86400");

		plan.addActivity(origAct);
		plan.addLeg(leg);
		plan.addActivity(targetAct);

		person.addPlan(plan);
		return person;
	}

}
