/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.scenarioSetup;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class BeelineDistanceFromActivitiesAnalyzer {

	private static final Logger LOG = Logger.getLogger(BeelineDistanceFromActivitiesAnalyzer.class);

	private final List<Integer> distanceClasses = new ArrayList<>(Arrays.asList( 2000, 4000, 6000, 8000, 10000, 20000000 // last dist class sufficiently high
			));
	private final String inputUrbanPlansFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/urban/"+PatnaUtils.PATNA_NETWORK_TYPE+"/initial_urban_plans_1pct.xml.gz";
	private final Map<Integer, Map<String, Integer>> distanceClass2Mode2Count = new TreeMap<>();
	private final Map<Integer, Integer> distanceClass2Count = new TreeMap<>();

	public static void main(String[] args) {
		BeelineDistanceFromActivitiesAnalyzer pupbda = new BeelineDistanceFromActivitiesAnalyzer();
		pupbda.initializeMaps();
		pupbda.parsePlansFile();
		pupbda.writeData(PatnaUtils.INPUT_FILES_DIR+ "/tripDiary_tripLengthDistributionData.txt");
	}
	
	private void writeData(final String outFile) {
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("distanceClass \t pctDistri \t");
			for (String mode : this.distanceClass2Mode2Count.get(2000).keySet() ) {
				writer.write(mode+"\t");
			}
			writer.newLine();
			
			for (Integer ii : this.distanceClass2Mode2Count.keySet() ) {
				writer.write(ii+"\t");
				writer.write(this.distanceClass2Count.get(ii)+"\t");
				for (String mode : this.distanceClass2Mode2Count.get(ii).keySet()){
					writer.write(this.distanceClass2Mode2Count.get(ii).get(mode)+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(" Data is not written. Reason :" + e);
		}
	}

	private void initializeMaps(){
		for (Integer i : distanceClasses) {
			this.distanceClass2Count.put(i, 0);
			this.distanceClass2Mode2Count.put(i, new TreeMap<>());
		}
	}

	private void parsePlansFile(){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(inputUrbanPlansFile);

		for (Person person : sc.getPopulation().getPersons().values() ) {

			Plan plan = person.getSelectedPlan();
			Coord startCoord = null;
			double dist = Double.NaN;
			String leg = null;

			for (PlanElement pe : plan.getPlanElements()) {

				if (pe instanceof Activity) {
					Coord cord = ((Activity) pe ).getCoord();
					if (startCoord == null ) startCoord = cord;
					else if (startCoord == cord) LOG.warn("The current activity and previous activity locations are same. "
							+ "Assuming this as a separate trip with zero beeline distance.");
					else {
						dist = CoordUtils.calcEuclideanDistance(startCoord, cord);
						startCoord = cord;
					}
				}  else if (pe instanceof Leg) {
					leg  = ((Leg)pe).getMode();
				}

				if( ! Double.isNaN(dist) ) {
					// store info
					int distClass = Integer.MAX_VALUE;
					for (Integer ii : this.distanceClass2Count.keySet()) {
						if ( (int) dist <= ii )  {
							distClass = ii;
							break;
						}
					}
					Map<String, Integer> mode2Count = this.distanceClass2Mode2Count.get(distClass);
					if (mode2Count.containsKey(leg)) mode2Count.put(leg, mode2Count.get(leg) + 1 );
					else mode2Count.put(leg,  1 );

					this.distanceClass2Count.put(distClass, this.distanceClass2Count.get(distClass) + 1 );
					dist = Double.NaN;
					startCoord = null;
				}
			}
		}
	}
}