/* *********************************************************************** *
 * project: org.matsim.*
 * DensityZoneLayer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.spatial;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.IOException;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**
 * @author illenberger
 *
 */
public class DensityZoneLayer {

	private static final Logger logger = Logger.getLogger(DensityZoneLayer.class);
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(args[0]);
		loader.loadScenario();
		Scenario data = loader.getScenario();
//		Config config = data.getConfig();
		PopulationImpl population = (PopulationImpl) data.getPopulation();
		
		ZoneLayerLegacy zoneLayer = ZoneLayerLegacy.createFromShapeFile(args[1]);
		ZoneLayerDouble zoneLayerDouble = new ZoneLayerDouble(new HashSet<ZoneLegacy>(zoneLayer.getZones()));
		
		TObjectIntHashMap<ZoneLegacy> inhabitants = new TObjectIntHashMap<ZoneLegacy>();
		
		logger.info("Counting persons...");
		int n = 0;
		int total = population.getPersons().size();
		for(Person person : population.getPersons().values()) {
			Coord homeLoc = ((PlanImpl) person.getSelectedPlan()).getFirstActivity().getCoord();
			
			ZoneLegacy zone = zoneLayerDouble.getZone(homeLoc);
			if(zone == null)
				logger.warn(String.format("No zone for coordingate %1$s,%2$s found.", homeLoc.getX(), homeLoc.getY()));
			else
				inhabitants.adjustOrPutValue(zone, 1, 1);
			
			n++;
			if(n % 1000 == 0)
				logger.info(String.format("Processed %1$s of %2$s persons (%3$.4f).", n, total, (float)n/total));
		}
		
		logger.info("Calculating density...");
		
		TObjectIntIterator<ZoneLegacy> it = inhabitants.iterator();
		for(int i = 0; i < inhabitants.size(); i++) {
			it.advance();
			ZoneLegacy zone = it.key();
			int count = it.value();
			double a = zone.getBorder().getArea();
			a = a / (1000 * 1000);
			zoneLayerDouble.setValue(it.key(), count/a);
		}
		
		logger.info("Writing file...");
		zoneLayerDouble.toFile(args[2]);
		logger.info("Done.");
	}

}
