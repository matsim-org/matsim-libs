/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseNmbmSamplePopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.population.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

public class AnalyseNmbmSamplePopulation {
	private final static Logger LOG = Logger.getLogger(AnalyseNmbmSamplePopulation.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AnalyseNmbmSamplePopulation.class.toString(), args);
		
		String outputFolder = args[0];
		/* Read in the population. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReaderMatsimV5 pr = new PopulationReaderMatsimV5(sc);
		pr.parse(args[1]);
		
		/* Determine statistics for activity types. */
		Map<String, List<Coord>> typeCoords = new HashMap<String, List<Coord>>();
		for(Id person : sc.getPopulation().getPersons().keySet()){
			Plan plan = sc.getPopulation().getPersons().get(person).getSelectedPlan();
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
					if(!typeCoords.containsKey(act.getType())){
						typeCoords.put(act.getType(), new ArrayList<Coord>());
					}
					typeCoords.get(act.getType()).add(act.getCoord());
				}
			}
		}
		LOG.info("----------------------------------------------------------------------");
		LOG.info("Activity types:");
		for(String type : typeCoords.keySet()){
			BufferedWriter bw = IOUtils.getBufferedWriter(outputFolder + "coords_" + type + ".txt"); 
			LOG.info("   " + type + ": " + typeCoords.get(type).size());
			try{
				bw.write("Long,Lat");
				bw.newLine();
				for(Coord c : typeCoords.get(type)){
					bw.write(String.format("%.0f,%.0f\n", c.getX(), c.getY() ) ) ;
				}				
			} catch (IOException e) {
				Gbl.errorMsg("Could not write to BufferedWriter.");
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					Gbl.errorMsg("Could not close BufferedWriter.");
				}
			}
		}
		LOG.info("----------------------------------------------------------------------");
		
		
		Header.printFooter();
	}

}

