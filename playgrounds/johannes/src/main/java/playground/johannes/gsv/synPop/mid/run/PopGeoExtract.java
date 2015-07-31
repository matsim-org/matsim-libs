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

package playground.johannes.gsv.synPop.mid.run;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTask;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.data.FacilityDataLoader;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * @author johannes
 * 
 */
public class PopGeoExtract {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String infile = args[0];
		String facFile = args[1];
		String geoFile = args[2];
		String outfile = args[3];

		Logger logger = Logger.getLogger(PopGeoExtract.class);

		XMLParser parser = new XMLParser();
		parser.setValidating(false);

		logger.info("Loading persons...");
		parser.parse(infile);
		Set<ProxyPerson> persons = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));

		FacilityDataLoader loader = new FacilityDataLoader(facFile, null);
		FacilityData fData = (FacilityData) loader.load();

		String data = new String(Files.readAllBytes(Paths.get(geoFile)));
		Set<Zone> zones = Zone2GeoJSON.parseFeatureCollection(data);

		logger.info("Applying filter...");
		persons = ProxyTaskRunner.runAndDeletePerson(new GeoFilter(fData.getAll(), zones.iterator().next().getGeometry()), persons);
		logger.info(String.format("Population size: %s", persons.size()));

		XMLWriter writer = new XMLWriter();
		writer.write(outfile, persons);

	}

	private static class GeoFilter implements ProxyPersonTask {

		private ActivityFacilities facilities;

		private PreparedGeometry geo;

		public GeoFilter(ActivityFacilities facilities, Geometry boundary) {
			this.facilities = facilities;
			geo = PreparedGeometryFactory.prepare(boundary);
		}

		@Override
		public void apply(ProxyPerson person) {
			boolean keep = false;

			for (ProxyPlan plan : person.getPlans()) {
				for (Element act : plan.getActivities()) {
					Id<ActivityFacility> id = Id.create(act.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
					ActivityFacility f = facilities.getFacilities().get(id);
					Point p = MatsimCoordUtils.coordToPoint(f.getCoord());
					if (geo.contains(p)) {
						keep = true;
						break;
					}
				}

				if (keep)
					break;
			}

			if (!keep) {
				person.setAttribute(CommonKeys.DELETE, "true");
			}
		}

	}

}
