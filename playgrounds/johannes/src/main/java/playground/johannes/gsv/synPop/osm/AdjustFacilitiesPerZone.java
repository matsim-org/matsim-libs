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

package playground.johannes.gsv.synPop.osm;

import gnu.trove.TObjectIntHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.gis.KreisOsmCompare;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

import com.vividsolutions.jts.geom.Point;

/**
 * @author johannes
 * 
 */
public class AdjustFacilitiesPerZone {

	private static final Logger logger = Logger.getLogger(AdjustFacilitiesPerZone.class);

	/**
	 * @param args
	 * @throws IOException
	 * @throws FactoryException
	 */
	public static void main(String[] args) throws IOException, FactoryException {
		ZoneLayer<Map<String, Object>> zones = ZoneLayerSHP.read("/home/johannes/gsv/osm/kreisCompare/zones_zone.SHP");

		String[] types = new String[] { "W" };
		TObjectIntHashMap<String> attractivness = KreisOsmCompare.readAttractivness("/home/johannes/gsv/osm/kreisCompare/StrukturAttraktivitaet.csv",
				types);

		double sumAttract = 0;
		for (Zone<Map<String, Object>> zone : zones.getZones()) {
			String code = (String) zone.getAttribute().get("ISO_CODE");
			if (code != null && code.equalsIgnoreCase("DE")) {
				int attract = attractivness.get(zone.getAttribute().get("NO").toString());
				zone.getAttribute().put("attractivness", attract);
				sumAttract += attract;
			}
		}

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		logger.info("Loading facilities...");
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile("/home/johannes/sge/prj/osm/run/384/output/home.xml");

		MathTransform transform = CRS.findMathTransform(CRSUtils.getCRS(31467), CRSUtils.getCRS(4326));
		int notfound = 0;
		for (ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()) {
			Point p = MatsimCoordUtils.coordToPoint(fac.getCoord());
			p = CRSUtils.transformPoint(p, transform);
			Zone<Map<String, Object>> zone = zones.getZone(p);

			if (zone != null) {
				Set<ActivityFacility> facs = (Set<ActivityFacility>) zone.getAttribute().get("facilities");
				if (facs == null) {
					facs = new HashSet<>();
					zone.getAttribute().put("facilities", facs);
				}
				facs.add(fac);
			} else {
				notfound++;
			}
		}
		if (notfound > 0)
			logger.warn(String.format("%s facilities cannot be assigned to a zone", notfound));

		double c = sumAttract / (double) scenario.getActivityFacilities().getFacilities().size();
		Random random = new XORShiftRandom(1);

		for (Zone<Map<String, Object>> zone : zones.getZones()) {
			String code = (String) zone.getAttribute().get("ISO_CODE");
			if (code != null && code.equalsIgnoreCase("DE")) {
				String name = (String) zone.getAttribute().get("NAME");
				Set<ActivityFacility> facs = (Set<ActivityFacility>) zone.getAttribute().get("facilities");
				if (facs != null) {
					int attract = (int) zone.getAttribute().get("attractivness");
					int targetNum = (int) (attract/c);

					logger.info(String.format("%s: target: %s, current: %s", name, targetNum, facs.size()));

					if (targetNum > facs.size()) {
						add(scenario.getActivityFacilities(), facs, targetNum, random);
					} else if (targetNum < facs.size()) {
						remove(scenario.getActivityFacilities(), facs, targetNum, random);
					}
				} else {
					logger.info(String.format("No facilities for %s.", name));
				}
			}
		}

		logger.info("Writing facilities...");
		FacilitiesWriter writer = new FacilitiesWriter(scenario.getActivityFacilities());
		writer.write("/home/johannes/gsv/osm/facilities/netz2030/home.netz2030.xml");
		logger.info("Done.");
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
				ActivityFacility newfac = facilities.getFactory().createActivityFacility(id, new CoordImpl(x, y));

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
