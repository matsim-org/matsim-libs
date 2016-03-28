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

package playground.johannes.gsv.synPop.osm;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author johannes
 * 
 */
public class AdjustFacilitiesFromMatrix {

	private static final Logger logger = Logger.getLogger(AdjustFacilitiesFromMatrix.class);

	public static void main(String[] args) throws IOException {
		String matrixFile = args[0];
		String zonesFile = args[1];
		String facilitiesFile = args[2];
		String outFile = args[3];
		/*
		 * load matrix
		 */
		logger.info("Loading matrix...");
		NumericMatrixXMLReader mReader = new NumericMatrixXMLReader();
		mReader.setValidating(false);
		mReader.parse(matrixFile);
		NumericMatrix m = mReader.getMatrix();
		/*
		 * load zones
		 */
		logger.info("Loading zones...");
		String data = new String(Files.readAllBytes(Paths.get(zonesFile)));
		Set<Zone> tmp = ZoneGeoJsonIO.parseFeatureCollection(data);
		ZoneCollection zones = new ZoneCollection(null);
		zones.addAll(tmp);
		/*
		 * load facilities
		 */
		logger.info("Loading facilities...");
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MatsimFacilitiesReader fReader = new MatsimFacilitiesReader(scenario);
		fReader.readFile(facilitiesFile);
		/*
		 * assign facilities to zones
		 */
		logger.info("Assigning facilities to zones...");
		Map<Zone, Set<ActivityFacility>> f2Zone = assignFacilities(zones, scenario.getActivityFacilities());
		/*
		 * create/remove facilities
		 */
		logger.info("Adjusting facilitites...");
		
		Random random = new XORShiftRandom();
		double c = MatrixOperations.sum(m) / scenario.getActivityFacilities().getFacilities().size();
		TObjectDoubleHashMap<String> marginals = MatrixOperations.columnMarginals(m);

		for (Zone zone : zones.getZones()) {
			String name = zone.getAttribute("nuts3_name");
			Set<ActivityFacility> facilities = f2Zone.get(zone);
			if (facilities != null) {
				String id = zone.getAttribute("gsvId");

				double vol = marginals.get(id);
				int targetNum = (int) (vol / c);
				logger.info(String.format("%s: target: %s, current: %s", name, targetNum, facilities.size()));

				if (targetNum > facilities.size()) {
					add(scenario.getActivityFacilities(), facilities, targetNum, random);
				} else if (targetNum < facilities.size()) {
					remove(scenario.getActivityFacilities(), facilities, targetNum, random);
				}
			} else {
				logger.info(String.format("No facilities for %s.", name));
			}
		}

		logger.info("Writing facilities...");
		FacilitiesWriter fWriter = new FacilitiesWriter(scenario.getActivityFacilities());
		fWriter.write(outFile);
	}

	private static Map<Zone, Set<ActivityFacility>> assignFacilities(ZoneCollection zones, ActivityFacilities facilities) {
		Map<Zone, Set<ActivityFacility>> map = new HashMap<Zone, Set<ActivityFacility>>();
		int notfound = 0;

		ProgressLogger.init(facilities.getFacilities().size(), 2, 10);
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			Point p = MatsimCoordUtils.coordToPoint(facility.getCoord());
			Zone zone = zones.get(p.getCoordinate());

			if (zone != null) {
				Set<ActivityFacility> facs = map.get(zone);
				if (facs == null) {
					facs = new HashSet<>();
					map.put(zone, facs);
				}
				facs.add(facility);
			} else {
				notfound++;
			}
			ProgressLogger.step();
		}
		ProgressLogger.terminate();

		if (notfound > 0) {
			logger.warn(String.format("Cannot assign %s facilities to a zone.", notfound));
		}

		return map;
	}

	private static void remove(ActivityFacilities facilities, Set<ActivityFacility> facs, int targetNum, Random random) {
		int remove = facs.size() - targetNum;
		List<ActivityFacility> list = new ArrayList<>(facs);
		if (remove > 0) {
			for (int i = 0; i < remove; i++) {
				ActivityFacility f = list.get(random.nextInt(list.size()));
				facilities.getFacilities().remove(f.getId());
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	private static void add(ActivityFacilities facilities, Set<ActivityFacility> facs, int targetNum, Random random) {
		int add = targetNum - facs.size();
		List<ActivityFacility> list = new ArrayList<>(facs);
		if (add > 0) {
			for (int i = 0; i < add; i++) {
				ActivityFacility f = list.get(random.nextInt(list.size()));
				double x = f.getCoord().getX() + (random.nextDouble() * 100);
				double y = f.getCoord().getY() + (random.nextDouble() * 100);
				Id<ActivityFacility> id = Id.create(f.getId().toString() + "clone" + i, ActivityFacility.class);
				ActivityFacility newfac = facilities.getFactory().createActivityFacility(id, new Coord(x, y));

				for (ActivityOption opt : f.getActivityOptions().values()) {
					newfac.addActivityOption(facilities.getFactory().createActivityOption(opt.getType()));
				}

				facilities.addActivityFacility(newfac);
			}
		} else {
			throw new IllegalArgumentException();
		}
	}
}
