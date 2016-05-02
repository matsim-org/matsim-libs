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

package playground.johannes.gsv.matrices.misc;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.MatsimFacilitiesReader;
import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 * 
 */
public class Matrix2Population {
	
	private final static Logger logger = Logger.getLogger(Matrix2Population.class);
	
	private final Map<String, List<ActivityFacility>> zone2Fac;
	
	private final String type;
	
	public Matrix2Population(ActivityFacilities facilities, ZoneCollection zones, String type, String zoneIdKey) {
		this.type = type;
		zone2Fac = initFacilities(facilities, type, zones, zoneIdKey);
	}

	public void generatePersons(NumericMatrix matrix, Population population, double proba) {
		Random random = new XORShiftRandom();
		List<String> keys = new ArrayList<>(matrix.keys());

		ProgressLogger.init(keys.size(), 2, 10);
		
		for (int i = 0; i < keys.size(); i++) {
			for (int j = i; j < keys.size(); j++) {
				String idi = keys.get(i);
				String idj = keys.get(j);

				List<ActivityFacility> facs_i = zone2Fac.get(idi);
				List<ActivityFacility> facs_j = zone2Fac.get(idj);

				if (facs_i != null && facs_j != null) {

					Double vol = matrix.get(idi, idj);
					if (vol != null) {

						int count = (int) Math.floor(vol * proba);
						double remainder = (vol * proba) - count;
						if (random.nextDouble() < remainder) {
							count++;
						}

						for (int k = 0; k < count; k++) {
							ActivityFacility fac_i = facs_i.get(random.nextInt(facs_i.size()));
							ActivityFacility fac_j = facs_j.get(random.nextInt(facs_j.size()));
							String id = String.format("%s.%s.%s.%s", idi, idj, type, k);

							Person person = buildPerson(fac_i, fac_j, type, "car", population.getFactory(), id);
							population.addPerson(person);
						}
					}
				}
			}
			
			ProgressLogger.step();
		}
	}

	private Person buildPerson(ActivityFacility fac_i, ActivityFacility fac_j, String type, String mode, PopulationFactory factory, String id) {
		Person person = factory.createPerson(Id.createPersonId(id));

		Plan plan = factory.createPlan();
		/*
		 * home act
		 */
		ActivityImpl act = (ActivityImpl) factory.createActivityFromCoord(ActivityTypes.HOME, fac_i.getCoord());
		act.setStartTime(0);
		act.setEndTime(8 * 60 * 60);
		act.setFacilityId(fac_i.getId());

		plan.addActivity(act);
		/*
		 * outward leg
		 */
		Leg leg = factory.createLeg(mode);
		plan.addLeg(leg);
		/*
		 * target act
		 */
		act = (ActivityImpl) factory.createActivityFromCoord(type, fac_j.getCoord());
		act.setEndTime(17 * 60 * 60);
		act.setFacilityId(fac_j.getId());

		plan.addActivity(act);
		/*
		 * return leg
		 */
		leg = factory.createLeg(mode);
		plan.addLeg(leg);
		/*
		 * home act
		 */
		act = (ActivityImpl) factory.createActivityFromCoord(ActivityTypes.HOME, fac_i.getCoord());
		act.setEndTime(24 * 60 * 60);
		act.setFacilityId(fac_i.getId());

		plan.addActivity(act);

		person.addPlan(plan);

		return person;
	}

	private Map<String, List<ActivityFacility>> initFacilities(ActivityFacilities facilities, String type, ZoneCollection zones, String zoneKey) {
		Map<String, List<ActivityFacility>> zone2facs = new HashMap<String, List<ActivityFacility>>();

		ProgressLogger.init(facilities.getFacilities().values().size(), 2, 10);
		
		for (ActivityFacility fac : facilities.getFacilities().values()) {
			Zone zone = zones.get(new Coordinate(fac.getCoord().getX(), fac.getCoord().getY()));
			if (zone != null) {
				ActivityOption opt = fac.getActivityOptions().get(type);
				if (opt != null) {
					List<ActivityFacility> facs = zone2facs.get(zone.getAttribute(zoneKey));
					if(facs == null) {
						facs = new ArrayList<>(1000);
						zone2facs.put(zone.getAttribute(zoneKey), facs);
					}
					facs.add(fac);
				}
			}
			
			ProgressLogger.step();
		}
		ProgressLogger.terminate();
		
		return zone2facs;
	}
	
	public static void main(String args[]) throws IOException {
		String zonesFile = args[0];
		String facilitiesFile = args[1];
		String matrixDir = args[2];
		File dir = new File(matrixDir);
		String[] matrixFiles = dir.list();

		double frac = Double.parseDouble(args[3]);
		String outFile = args[4];

		ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zonesFile, "plz", null);
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MatsimFacilitiesReader fReader = new MatsimFacilitiesReader(scenario);
		fReader.readFile(facilitiesFile);
		
		for (String file : matrixFiles) {
//			Matrix vMatrix = new Matrix("1", null);
//			VisumMatrixReader vReader = new VisumMatrixReader(vMatrix);
//			vReader.readFile(matrixDir + file);
			NumericMatrix m = NumericMatrixIO.read(matrixDir + file);
			
//			String type = file.split("\\.")[1];
			String type = "misc";
			
			logger.info("Initializing facilities...");
//			Matrix2Population m2p = new Matrix2Population(scenario.getActivityFacilities(), zones, type, "NO");
			Matrix2Population m2p = new Matrix2Population(scenario.getActivityFacilities(), zones, type, "plz");
			
			logger.info(String.format("Generating persons out of %s %s trips...", MatrixOperations.sum(m), type));
//			m2p.generatePersons(m, scenario.getPopulation(), 1/11.8);
			m2p.generatePersons(m, scenario.getPopulation(), frac);
			logger.info(String.format("Generated %s persons.", scenario.getPopulation().getPersons().size()));
		}
		
		
		logger.info("Writing population...");
		PopulationWriter pWriter = new PopulationWriter(scenario.getPopulation());
		pWriter.write(outFile);
		logger.info("Done.");
	}
}
