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

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import playground.johannes.gsv.synPop.sim3.RestoreActTypes;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLWriter;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

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

	public NumericMatrix run(Collection<PlainPerson> persons, ZoneCollection zones, ActivityFacilities facilities, String key) {

		NumericMatrix m = new NumericMatrix();

		int noZones = 0;

		ProgressLogger.init(persons.size(), 2, 10);

		int legCandidates = 0;
		int tripCandidates = 0;
		int trips = 0;
		double pkmRoute = 0;
		double pkmGeo = 0;

		for (PlainPerson person : persons) {
			Episode plan = person.getEpisodes().get(0);
			for (int i = 0; i < plan.getLegs().size(); i++) {
				Attributable leg = plan.getLegs().get(i);
				Attributable prev = plan.getActivities().get(i);
				Attributable next = plan.getActivities().get(i + 1);

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
		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get(args[2])));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));

		logger.info("Loading persons...");
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
		parser.parse(args[0]);
		logger.info(String.format("Loaded %s persons...", parser.getPersons().size()));

		Set<PlainPerson> persons = (Set<PlainPerson>)parser.getPersons();

		logger.info("Restoring original activity types...");
		TaskRunner.run(new RestoreActTypes(), persons, true);
		
		logger.info("Replaceing misc types...");
		new ReplaceMiscType().apply(persons);
		
		String outdir = args[4];

		ModePredicate modePred = new ModePredicate("car");
		ProxyPlans2Matrix p2m = new ProxyPlans2Matrix(modePred);
		/*
		 * all car
		 */
		logger.info("Extracting total matrix...");
		NumericMatrix m = p2m.run(persons, zones, scenario.getActivityFacilities(), key);
		NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
		writer.write(m, String.format("%s/miv.xml", outdir));
		/*
		 * types
		 */
		PredicateORComposite wkDayPred = new PredicateORComposite();
		wkDayPred.addComponent(new DayPredicate(CommonValues.MONDAY));
		wkDayPred.addComponent(new DayPredicate(CommonValues.TUESDAY));
		wkDayPred.addComponent(new DayPredicate(CommonValues.WEDNESDAY));
		wkDayPred.addComponent(new DayPredicate(CommonValues.THURSDAY));
		wkDayPred.addComponent(new DayPredicate(CommonValues.FRIDAY));
		
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
		String[] days = new String[] {CommonValues.MONDAY, CommonValues.FRIDAY, CommonValues.SATURDAY, CommonValues.SUNDAY};
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
		predOR.addComponent(new DayPredicate(CommonValues.TUESDAY));
		predOR.addComponent(new DayPredicate(CommonValues.WEDNESDAY));
		predOR.addComponent(new DayPredicate(CommonValues.THURSDAY));
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
		String[] months = new String[]{MiDValues.APRIL, MiDValues.MAY, MiDValues.JUNE, MiDValues.JULY, MiDValues.AUGUST, MiDValues.SEPTEMBER, MiDValues.OCTOBER};
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
		months = new String[]{MiDValues.NOVEMBER, MiDValues.DECEMBER, MiDValues.JANUARY, MiDValues.FEBRUARY, MiDValues.MARCH};
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
