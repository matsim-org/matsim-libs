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

package playground.johannes.synpop.gis;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import java.util.Random;

public class FacilityDataLoader implements DataLoader {

	private static final Logger logger = Logger.getLogger(FacilityDataLoader.class);
	
	public static final String KEY = "facilityData";
	
	private final String file;
	
	private final Random random;
	
	public FacilityDataLoader(String file, Random random) {
		this.file = file;
		this.random = random;
	}
	
	@Override
	public Object load() {
		logger.info("Loading facility data...");
		Level level = Logger.getRootLogger().getLevel();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		FacilitiesReaderMatsimV1 reader = new FacilitiesReaderMatsimV1(scenario);
		reader.readFile(file);
		
		FacilityData data = new FacilityData(scenario.getActivityFacilities(), random);
		
		Logger.getRootLogger().setLevel(level);
		
		logger.info(String.format("Loaded %s facilities.", data.getAll().getFacilities().size()));
		
		return data;
	}

}
