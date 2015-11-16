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

package playground.johannes.gsv.synPop.sim3;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.gsv.synPop.data.LandUseData;
import playground.johannes.gsv.synPop.data.LandUseDataLoader;
import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.gis.FacilityDataLoader;
import playground.johannes.synpop.processing.PersonsTask;

import java.util.*;

/**
 * @author johannes
 * 
 */
public class InitHomeLocations implements PersonsTask {

	private static final Logger logger = Logger.getLogger(InitHomeLocations.class);

	private final DataPool dataPool;

	private final Random random;

	public InitHomeLocations(DataPool dataPool, Random random) {
		this.dataPool = dataPool;
		this.random = random;
	}

	@Override
	public void apply(Collection<? extends Person> persons) {
		LandUseData landUseData = (LandUseData) dataPool.get(LandUseDataLoader.KEY);

		// ZoneLayer<Map<String, Object>> zoneLayer =
		// landUseData.getNuts3Layer();
		ZoneLayer<Map<String, Object>> zoneLayer = landUseData.getModenaLayer();
		List<Zone<Map<String, Object>>> zones = new ArrayList<>(zoneLayer.getZones());
		TObjectDoubleHashMap<Zone<?>> zoneProba = new TObjectDoubleHashMap<>();

		double sum = 0;
		for (Zone<Map<String, Object>> zone : zoneLayer.getZones()) {
			sum += (Double) zone.getAttribute().get(LandUseData.POPULATION_KEY);
		}

		for (Zone<Map<String, Object>> zone : zoneLayer.getZones()) {
			double inhabs = (Double) zone.getAttribute().get(LandUseData.POPULATION_KEY);
			double p = inhabs / sum;
			zoneProba.put(zone, p);
		}

		logger.info("Assigning facilities to zones...");

		int unassigned = 0;

		FacilityData facilityData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
		Map<Zone<?>, List<ActivityFacility>> zoneFacilities = new IdentityHashMap<>(zones.size());
		List<ActivityFacility> homeFacils = facilityData.getFacilities(ActivityTypes.HOME);
		ProgressLogger.init(homeFacils.size(), 2, 10);
		for (ActivityFacility facility : homeFacils) {
			Zone<?> zone = zoneLayer.getZone(MatsimCoordUtils.coordToPoint(facility.getCoord()));
			if (zone != null) {
				List<ActivityFacility> facilities = zoneFacilities.get(zone);
				if (facilities == null) {
					facilities = new ArrayList<>();
					zoneFacilities.put(zone, facilities);
				}
				facilities.add(facility);
			} else {
				unassigned++;
			}
			ProgressLogger.step();
		}

		if (unassigned > 0) {
			logger.warn(String.format("%s facilities are out if zone bounds.", unassigned));
		}

		logger.info("Assigning facilities to persons...");
		ProgressLogger.init(persons.size(), 2, 10);
		List<Person> shuffledPersons = new ArrayList<>(persons);
		Collections.shuffle(shuffledPersons, random);
		TObjectDoubleIterator<Zone<?>> it = zoneProba.iterator();
		int j = 0;
		for (int i = 0; i < zoneProba.size(); i++) {
			it.advance();
			int n = (int) Math.ceil(persons.size() * it.value());
			List<ActivityFacility> facilities = zoneFacilities.get(it.key());
			if (facilities != null) {
				if (n + j > persons.size()) {
					n = persons.size() - j;
				}
				for (int k = j; k < (j + n); k++) {
					ActivityFacility f = facilities.get(random.nextInt(facilities.size()));
					((PlainPerson)shuffledPersons.get(k)).setUserData(SwitchHomeLocation.USER_FACILITY_KEY, f);

					ProgressLogger.step();
				}

				j += n;
			}
		}
		
		logger.info("Checking for homeless persons...");
		int cnt = 0;
		for(Person person : shuffledPersons) {
			if(((PlainPerson)person).getUserData(SwitchHomeLocation.USER_FACILITY_KEY) == null) {
				ActivityFacility f = homeFacils.get(random.nextInt(homeFacils.size()));
				((PlainPerson)person).setUserData(SwitchHomeLocation.USER_FACILITY_KEY, f);
				cnt++;
			}
		}
		if(cnt > 0) {
			logger.info(String.format("Assigend %s persons a random home.", cnt));
		}

		ProgressLogger.termiante();
	}

}
