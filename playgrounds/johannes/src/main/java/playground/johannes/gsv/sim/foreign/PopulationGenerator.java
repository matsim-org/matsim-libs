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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.collections.ChoiceSet;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.XMLWriter;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author johannes
 * 
 */
public class PopulationGenerator {

	private static final Logger logger = Logger.getLogger(PopulationGenerator.class);

	private static final Random random = new XORShiftRandom();

	private static final double scaleFactor = 11;

	private static Map<Zone, List<ActivityFacility>> facilityMap;

	private static ChoiceSet<String> dayProbas;
	
	private static ChoiceSet<String> monthProbas;
	
	private static ChoiceSet<String> vacationProbas;

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

		int numThreads = 20;
		/*
		 * load zones
		 */
		logger.info("Loading geometries...");
		String data = new String(Files.readAllBytes(Paths.get(zonesFile)));
		Set<Zone> zones = ZoneGeoJsonIO.parseFeatureCollection(data);
		ZoneCollection zoneCollection = new ZoneCollection(null);
		zoneCollection.addAll(zones);
		zoneCollection.setPrimaryKey("NO");

		Set<Zone> deZones = new HashSet<>();
		Set<Zone> euZones = new HashSet<>();
		for (Zone zone : zones) {
			if (zone.getAttribute("NUTS0_CODE").equalsIgnoreCase("DE")) {
				deZones.add(zone);
			} else {
				euZones.add(zone);
			}
		}
		logger.info(String.format("%s de zones, %s eu zones.", deZones.size(), euZones.size()));

		ZoneCollection deZoneLayer = new ZoneCollection(null);
		deZoneLayer.addAll(deZones);
		deZoneLayer.setPrimaryKey("NO");
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
		 * 
		 */
		dayProbas = new ChoiceSet<>(random);
		dayProbas.addOption(CommonValues.MONDAY, 1.02);
		dayProbas.addOption(CommonValues.TUESDAY, 1.04);
		dayProbas.addOption(CommonValues.WEDNESDAY, 1.08);
		dayProbas.addOption(CommonValues.WEDNESDAY, 1.08);
		dayProbas.addOption(CommonValues.FRIDAY, 1.15);
		dayProbas.addOption(CommonValues.SATURDAY, 0.95);
		dayProbas.addOption(CommonValues.SUNDAY, 0.67);
		/*
		 * 
		 */
		monthProbas = new ChoiceSet<>(random);
		monthProbas.addOption(MiDValues.JANUARY, 0.083);
		monthProbas.addOption(MiDValues.FEBRUARY, 0.094);
		monthProbas.addOption(MiDValues.MARCH, 0.099);
		monthProbas.addOption(MiDValues.APRIL, 0.095);
		monthProbas.addOption(MiDValues.MAY, 0.091);
		monthProbas.addOption(MiDValues.JUNE, 0.085);
		// july is missing in mid2008
		monthProbas.addOption(MiDValues.AUGUST, 0.092);
		monthProbas.addOption(MiDValues.SEPTEMBER, 0.093);
		monthProbas.addOption(MiDValues.OCTOBER, 0.088);
		monthProbas.addOption(MiDValues.NOVEMBER, 0.092);
		monthProbas.addOption(MiDValues.DECEMBER, 0.088);
		/*
		 * 
		 */
		vacationProbas = new ChoiceSet<>(random);
		vacationProbas.addOption("vacations_short", 0.76);
		vacationProbas.addOption("vacations_long", 0.24);
		/*
		 * load matrix
		 */
		Set<PlainPerson> persons = new HashSet<>();

		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		Future<?>[] futures = new Future[numThreads];

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
			ProgressLogger.init(euZones.size(), 1, 10);

			double origVolume = 0;
			double intVolume = 0;

			RunThread threads[] = new RunThread[numThreads];
			List<Zone>[] segments = CollectionUtils.split(euZones, numThreads);
			/*
			 * submit tasks
			 */
			for (int i = 0; i < segments.length; i++) {
				threads[i] = new RunThread(m, segments[i], deZones, type);

				futures[i] = executor.submit(threads[i]);
			}
			/*
			 * wait for threads
			 */
			for (int i = 0; i < segments.length; i++) {
				try {
					futures[i].get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}

			ProgressLogger.terminate();

			for (RunThread thread : threads) {
				origVolume += thread.origVolume;
				intVolume += thread.intVolume;
				persons.addAll(thread.persons);
			}

			logger.info(String.format("Original = %s, discretized = %s", origVolume, intVolume));
			logger.info(String.format("Added %s persons.", persons.size()));
		}

		executor.shutdown();

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
		ProgressLogger.terminate();
		if (cnt > 0) {
			logger.info(String.format("%s facilities cannot be assigned to a zone.", cnt));
		}

		Map<Zone, List<ActivityFacility>> listMap = new HashMap<>(setMap.size());
		for (java.util.Map.Entry<Zone, Set<ActivityFacility>> entry : setMap.entrySet()) {
			listMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		return listMap;
	}

	private static int getVolume(double volume) {
		int intVolume = (int) Math.floor(volume / scaleFactor);
		double left = (volume / scaleFactor) - intVolume;
		if (random.nextDouble() < left) {
			intVolume++;
		}
		return intVolume;
	}

	private static PlainPerson buildPerson(String id, ActivityFacility orig, ActivityFacility target, String origType, String targetType) {
		if(origType.equalsIgnoreCase("vacations")) {
			origType = vacationProbas.randomWeightedChoice();
		}
		if(targetType.equalsIgnoreCase("vacations")) {
			targetType = vacationProbas.randomWeightedChoice();
		}
		
		PlainPerson person = new PlainPerson(id);
		person.setAttribute(CommonKeys.DAY, dayProbas.randomWeightedChoice());
		person.setAttribute(MiDKeys.PERSON_MONTH, monthProbas.randomWeightedChoice());

		Episode plan = new PlainEpisode();
		plan.setAttribute(CommonKeys.DATA_SOURCE, "foreign");

		PlainSegment origAct = new PlainSegment();
		origAct.setAttribute(CommonKeys.ACTIVITY_TYPE, origType);
		origAct.setAttribute(CommonKeys.ACTIVITY_FACILITY, orig.getId().toString());
		origAct.setAttribute(CommonKeys.ACTIVITY_START_TIME, "0");
		origAct.setAttribute(CommonKeys.ACTIVITY_END_TIME, "1");

		PlainSegment leg = new PlainSegment();
		leg.setAttribute(CommonKeys.LEG_MODE, "car");

		PlainSegment targetAct = new PlainSegment();
		targetAct.setAttribute(CommonKeys.ACTIVITY_TYPE, targetType);
		targetAct.setAttribute(CommonKeys.ACTIVITY_FACILITY, target.getId().toString());
		targetAct.setAttribute(CommonKeys.ACTIVITY_START_TIME, "86399");
		targetAct.setAttribute(CommonKeys.ACTIVITY_END_TIME, "86400");

		plan.addActivity(origAct);
		plan.addLeg(leg);
		plan.addActivity(targetAct);

		person.addEpisode(plan);
		return person;
	}

	private static class RunThread implements Runnable {

		private final Matrix m;

		private final Collection<Zone> euZones;

		private final Set<Zone> deZones;

		private double origVolume;

		private double intVolume;

		private final Set<PlainPerson> persons;

		private final String type;

		private final Random random = new XORShiftRandom();

		public RunThread(Matrix m, Collection<Zone> euZones, Set<Zone> deZones, String type) {
			this.m = m;
			this.euZones = euZones;
			this.deZones = deZones;
			this.type = type;
			persons = new HashSet<>();
		}

		@Override
		public void run() {
			for (Zone euZone : euZones) {
				List<ActivityFacility> euList = facilityMap.get(euZone);
				String euZoneId = euZone.getAttribute("NO");

				if (euList == null || euList.isEmpty()) {
					logger.warn(String.format("No facilitites found for eu zone %s.", euZoneId));
				} else {
					ActivityFacility euFac = euList.get(random.nextInt(euList.size()));

					for (Zone deZone : deZones) {
						List<ActivityFacility> deList = facilityMap.get(deZone);
						String deZoneId = deZone.getAttribute("NO");
						/*
						 * outgoing
						 */
						Entry de2Eu = m.getEntry(deZoneId, euZoneId);
						if (de2Eu != null) {
							int volume = getVolume(de2Eu.getValue());
							origVolume += de2Eu.getValue();

							if (volume > 0) {
								if (deList == null || deList.isEmpty()) {
									logger.warn(String.format("No facilitites found for de zone %s.", deZoneId));
								} else {
									for (int i = 0; i < volume; i++) {
										ActivityFacility deFac = deList.get(random.nextInt(deList.size()));
										PlainPerson person = buildPerson(String.format("foreign.%s.%s.%s.%s", deZoneId, euZoneId, i, type), deFac, euFac,
												ActivityTypes.HOME, type);
										persons.add(person);
									}
									intVolume += volume;
								}
							}
						}
						/*
						 * incoming
						 */
						Entry eu2de = m.getEntry(euZoneId, deZoneId);
						if (eu2de != null) {
							int volume = getVolume(eu2de.getValue());
							origVolume += eu2de.getValue();
							if (volume > 0) {
								if (deList == null || deList.isEmpty()) {
									logger.warn(String.format("No facilitites found for de zone %s.", deZoneId));
								} else {
									for (int i = 0; i < volume; i++) {
										ActivityFacility deFac = deList.get(random.nextInt(deList.size()));
										PlainPerson person = buildPerson(String.format("foreign.%s.%s.%s.%s", euZoneId, deZoneId, i, type), euFac, deFac, type,
												ActivityTypes.HOME);
										persons.add(person);
									}
									intVolume += volume;
								}
							}
						}
					}
				}
				ProgressLogger.step();
			}

		}

	}
}
