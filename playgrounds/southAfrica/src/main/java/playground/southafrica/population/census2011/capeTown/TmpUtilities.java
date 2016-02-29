/* *********************************************************************** *
 * project: org.matsim.*
 * TmpUtilities.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.southafrica.population.census2011.capeTown;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * Just some utilities to do checks and execute code snippets.
 * 
 * @author jwjoubert
 */
public class TmpUtilities {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		checkConfig();
		checkWalkLegs();
	}
	
	public static void checkConfig(){
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, "/Users/jwjoubert/workspace/areas-capeTown/population/20160212_100pct/config.xml");
		
		ModeRoutingParams ride = config.plansCalcRoute().getOrCreateModeRoutingParams("ride");
		ride = config.plansCalcRoute().getModeRoutingParams().get("ride");
		ride.setBeelineDistanceFactor(1.3);
		ride.setTeleportedModeFreespeedFactor(0.8); /* Free speed-based. */
		
		new ConfigWriter(config).write("/Users/jwjoubert/workspace/areas-capeTown/population/20160212_100pct/config.xml");
	}
	
	public static void checkWalkLegs(){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse("/Users/jwjoubert/Documents/Projects/CapeTownMultimodal/001pct/population.xml.gz");
		
		BufferedWriter bw = IOUtils.getBufferedWriter("/Users/jwjoubert/Downloads/walkLegs.csv");
		try{
			bw.write("mode,dist");
			bw.newLine();
			
			for(Person person : sc.getPopulation().getPersons().values()){
				for(int i = 1; i < person.getSelectedPlan().getPlanElements().size(); i+=2){
					Leg leg = (Leg)person.getSelectedPlan().getPlanElements().get(i);
					bw.write(leg.getMode());
					bw.write(",");
					Coord a = ((Activity)person.getSelectedPlan().getPlanElements().get(i-1)).getCoord();
					Coord b = ((Activity)person.getSelectedPlan().getPlanElements().get(i+1)).getCoord();
					double distance = CoordUtils.calcEuclideanDistance(a, b)*1.3;
					bw.write(String.format("%.0f\n", distance));
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("cannot close.");
			}
		}
		
	}

}
