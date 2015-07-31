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

package playground.johannes.gsv.matrices.plans2matrix;

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
import playground.johannes.synpop.data.Element;
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
	
	public ProxyPlans2Matrix(Predicate predicate) {
		setPredicate(predicate);

		try {
			transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
		} catch (FactoryException e1) {
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

		int legCandidates = 0;
		int tripCandidates = 0;
		int trips = 0;
		double pkmRoute = 0;
		double pkmGeo = 0;

		for (ProxyPerson person : persons) {
			ProxyPlan plan = person.getPlans().get(0);
			for (int i = 0; i < plan.getLegs().size(); i++) {
				Element leg = plan.getLegs().get(i);
				Element prev = plan.getActivities().get(i);
				Element next = plan.getActivities().get(i + 1);

				legCandidates++;
				if (predicate.test(person, leg, prev, next)) {
					tripCandidates++;
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

						if (key1 != null && key2 != null) {
							Double val = m.get(key1, key2);
							if (val == null) {
								val = new Double(0);
							}

							m.set(key1, key2, ++val);
							trips++;
							
							String routeDistStr = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
							if(routeDistStr != null) pkmRoute +=  Double.parseDouble(routeDistStr);
							
							String geoDistStr = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
							if(geoDistStr != null) pkmGeo += Double.parseDouble(geoDistStr);
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

		logger.info(String.format("Processed %s legs.", legCandidates));
		logger.info(String.format("Processed %s car trips.", tripCandidates));
		logger.info(String.format("Volume: %s car trips.", trips));
		logger.info(String.format("PKM (route distance): %s", pkmRoute));
		logger.info(String.format("PKM (geo distance): %s", pkmGeo));
		
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
		
		logger.info("Replaceing misc types...");
		new ReplaceMiscType().apply(persons);
		
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
		PredicateORComposite wkDayPred = new PredicateORComposite();
		wkDayPred.addComponent(new DayPredicate(CommonKeys.MONDAY));
		wkDayPred.addComponent(new DayPredicate(CommonKeys.TUESDAY));
		wkDayPred.addComponent(new DayPredicate(CommonKeys.WEDNESDAY));
		wkDayPred.addComponent(new DayPredicate(CommonKeys.THURSDAY));
		wkDayPred.addComponent(new DayPredicate(CommonKeys.FRIDAY));
		
		String[] types = new String[] { "work", "buisiness", "shop", "edu", "vacations_short", "vacations_long" };
		for (String type : types) {
			logger.info(String.format("Extracting matrix %s...", type));
			PredicateANDComposite pred = new PredicateANDComposite();
			pred.addComponent(modePred);
			pred.addComponent(new ActivityTypePredicate(type));

			p2m.setPredicate(pred);
			m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);

			writer.write(m, String.format("%s/miv.%s.xml", outdir, type));
			/*
			 * types for mo-fr only
			 */
			logger.info(String.format("Extracting matrix %s.wkday...", type));
			pred.addComponent(wkDayPred);
			p2m.setPredicate(pred);
			m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);

			writer.write(m, String.format("%s/miv.wkday.%s.xml", outdir, type));
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
		 * one-day leisure only mo-fr
		 */
		logger.info("Extracting matrix leisure wkday...");
		pred.addComponent(wkDayPred);
		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);

		writer.write(m, String.format("%s/miv.wkday.leisure.xml", outdir));
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
		 * wecommuter only mo-fr
		 */
		logger.info("Extracting matrix wecommuter wkday...");
		pred.addComponent(wkDayPred);
		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		writer.write(m, String.format("%s/miv.wkday.wecommuter.xml", outdir));
		/*
		 * misc -- for validation
		 */
		logger.info("Extracting matrix wecommuter...");
		pred = new PredicateANDComposite();
		pred.addComponent(modePred);
		pred.addComponent(new ActivityTypePredicate("misc"));
		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		writer.write(m, String.format("%s/miv.misc.xml", outdir));
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
		
		logger.info("Extracting matrix mo-fr...");
		pred = new PredicateANDComposite();
		pred.addComponent(modePred);
		pred.addComponent(wkDayPred);
		p2m.setPredicate(pred);
		m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		writer.write(m, String.format("%s/miv.wkday.xml", outdir));
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
