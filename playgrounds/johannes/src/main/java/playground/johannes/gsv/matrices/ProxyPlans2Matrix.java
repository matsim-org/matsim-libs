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

package playground.johannes.gsv.matrices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.gsv.synPop.mid.run.ProxyTaskRunner;
import playground.johannes.gsv.synPop.sim3.RestoreActTypes;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.util.ProgressLogger;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author johannes
 * 
 */
public class ProxyPlans2Matrix {

	private static final Logger logger = Logger.getLogger(ProxyPlans2Matrix.class);

	private Predicate predicate;

	private MathTransform transform;
	
//	private int validTrips;

	public ProxyPlans2Matrix(Predicate predicate) {
		setPredicate(predicate);

		try {
			transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
		} catch (FactoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void setPredicate(Predicate predicate) {
		this.predicate = predicate;
	}

	public KeyMatrix run(Collection<ProxyPerson> persons, ZoneCollection zones, ActivityFacilities facilities, String key) {

		KeyMatrix m = new KeyMatrix();

		int noZones = 0;

		ProgressLogger.init(persons.size(), 2, 10);

		int legs = 0;
		int trips = 0;

		for (ProxyPerson person : persons) {
			ProxyPlan plan = person.getPlans().get(0);
			for (int i = 0; i < plan.getLegs().size(); i++) {
				ProxyObject leg = plan.getLegs().get(i);
				ProxyObject prev = plan.getActivities().get(i);
				ProxyObject next = plan.getActivities().get(i + 1);

				legs++;
				if (predicate.test(person, leg, prev, next)) {
					trips++;
					Id<ActivityFacility> origId = Id.create(prev.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
					ActivityFacility origFac = facilities.getFacilities().get(origId);

					Id<ActivityFacility> destId = Id.create(next.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
					ActivityFacility destFac = facilities.getFacilities().get(destId);

					Coordinate c1 = new Coordinate(origFac.getCoord().getX(), origFac.getCoord().getY());
					Coordinate c2 = new Coordinate(destFac.getCoord().getX(), destFac.getCoord().getY());

					CRSUtils.transformCoordinate(c1, transform);
					CRSUtils.transformCoordinate(c2, transform);

					Zone origZone = zones.get(c1);
					Zone destZone = zones.get(c2);

					if (origZone != null && destZone != null) {
						String key1 = origZone.getAttribute(key);
						String key2 = destZone.getAttribute(key);

						// if (origZone != null && destZone != null) {
						if (key1 != null && key2 != null) {
							Double val = m.get(key1, key2);
							if (val == null) {
								val = new Double(0);
							}

							m.set(key1, key2, ++val);
//							validTrips++;
						} else {
							noZones++;
						}
					} else {
						noZones++;
					}
				}
			}
			ProgressLogger.step();
		}

		if (noZones > 0) {
			logger.warn(String.format("%s activity locations could not be located in a zone.", noZones));
		}

		logger.info(String.format("Processed %s legs.", legs));
		logger.info(String.format("Processed %s car trips.", trips));
//		logger.info(String.format("Processed %s car trips.", trips));

		return m;
	}

	public static void main(String args[]) throws IOException {
		String key = args[3];

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		logger.info("Loading facilities...");
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(args[1]);

		logger.info("Loading zones...");
		ZoneCollection zones = new ZoneCollection();
		String data = new String(Files.readAllBytes(Paths.get(args[2])));
		zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));

		logger.info("Loading persons...");
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[0]);
		logger.info(String.format("Loaded %s persons...", parser.getPersons().size()));

		Set<ProxyPerson> persons = parser.getPersons();

		logger.info("Restoring original activity types...");
		ProxyTaskRunner.run(new RestoreActTypes(), persons, true);
		
		String outdir = args[4];

		ModePredicate modePred = new ModePredicate("car");
		ProxyPlans2Matrix p2m = new ProxyPlans2Matrix(modePred);
		/*
		 * all car
		 */
		logger.info("Extracting total matrix...");
		KeyMatrix m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		KeyMatrixXMLWriter writer = new KeyMatrixXMLWriter();
		writer.write(m, String.format("%s/miv.xml", outdir));
		/*
		 * types
		 */
		String[] types = new String[] { "work", "buisiness", "shop", "edu", "vacations_short", "vacations_long" };
		for (String type : types) {
			logger.info(String.format("Extracting matrix %s...", type));
			PredicateANDComposite pred = new PredicateANDComposite();
			pred.addComponent(modePred);
			pred.addComponent(new ActivityTypePredicate(type));

			p2m.setPredicate(pred);
			m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);

			writer.write(m, String.format("%s/miv.%s.xml", outdir, type));
		}
		/*
		 * one-day leisure
		 */
		logger.info("Extracting matrix leisure...");
		types = new String[] { "leisure", "visit", "gastro", "culture", "private", "pickdrop", "sport" };
		PredicateORComposite predOR = new PredicateORComposite();
		for (String type : types) {
			predOR.addComponent(new ActivityTypePredicate(type));
		}

		PredicateANDComposite pred = new PredicateANDComposite();
		pred.addComponent(predOR);
		pred.addComponent(modePred);

		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);

		writer.write(m, String.format("%s/miv.leisure.xml", outdir));
		/*
		 * wecommuter
		 */
		logger.info("Extracting matrix wecommuter...");
		pred = new PredicateANDComposite();
		pred.addComponent(modePred);
		pred.addComponent(new WeCommuterPredicate(100000));
		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		writer.write(m, String.format("%s/miv.wecommuter.xml", outdir));
		/*
		 * days
		 */
		String[] days = new String[] {CommonKeys.MONDAY, CommonKeys.FRIDAY, CommonKeys.SATURDAY, CommonKeys.SUNDAY};
		for(String day : days) {
			logger.info(String.format("Extracting matrix %s...", day));
			pred = new PredicateANDComposite();
			pred.addComponent(modePred);
			pred.addComponent(new DayPredicate(day));
			p2m.setPredicate(pred);
			m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
			writer.write(m, String.format("%s/miv.%s.xml", outdir, day));
			
		}
		
		logger.info("Extracting matrix di-mi-do...");
		pred = new PredicateANDComposite();
		predOR = new PredicateORComposite();
		predOR.addComponent(new DayPredicate(CommonKeys.TUESDAY));
		predOR.addComponent(new DayPredicate(CommonKeys.WEDNESDAY));
		predOR.addComponent(new DayPredicate(CommonKeys.THURSDAY));
		pred.addComponent(modePred);
		pred.addComponent(predOR);
		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		writer.write(m, String.format("%s/miv.dimido.xml", outdir));
		/*
		 * seasons
		 */
		logger.info("Extracting summer matrix...");
		predOR = new PredicateORComposite();
		String[] months = new String[]{MIDKeys.APRIL, MIDKeys.MAY, MIDKeys.JUNE, MIDKeys.JULY, MIDKeys.AUGUST, MIDKeys.SEPTEMBER, MIDKeys.OCTOBER};
		for(String month : months) {
			predOR.addComponent(new MonthPredicate(month));
		}
		pred = new PredicateANDComposite();
		pred.addComponent(modePred);
		pred.addComponent(predOR);
		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		writer.write(m, String.format("%s/miv.summer.xml", outdir));
		
		logger.info("Extracting winter matrix...");
		predOR = new PredicateORComposite();
		months = new String[]{MIDKeys.NOVEMBER, MIDKeys.DECEMBER, MIDKeys.JANUARY, MIDKeys.FEBRUARY, MIDKeys.MARCH};
		for(String month : months) {
			predOR.addComponent(new MonthPredicate(month));
		}
		pred = new PredicateANDComposite();
		pred.addComponent(modePred);
		pred.addComponent(predOR);
		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		writer.write(m, String.format("%s/miv.winter.xml", outdir));
		/*
		 * directions
		 */
		logger.info("Extracting matrix from home...");
		pred = new PredicateANDComposite();
		pred.addComponent(modePred);
		pred.addComponent(new FromHomePredicate());
		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		writer.write(m, String.format("%s/miv.fromHome.xml", outdir));
		
		logger.info("Extracting matrix to home...");
		pred = new PredicateANDComposite();
		pred.addComponent(modePred);
		pred.addComponent(new ToHomePredicate());
		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		writer.write(m, String.format("%s/miv.toHome.xml", outdir));
	}
}
