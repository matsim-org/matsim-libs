/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertPersonPlansToTrips.java
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

/**
 * 
 */
package playground.southafrica.projects.toronto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

/**
 * Class to extract the trips of individuals, not commercial vehicles, into a 
 * format that reflects the trip sequence.
 * 
 * @author jwjoubert
 */
public class ConvertPersonPlansToTrips {
	final private static Logger LOG = Logger.getLogger(ConvertPersonPlansToTrips.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ConvertPersonPlansToTrips.class.toString(), args);
		String population = args[0];
		String output = args[1];

		/* Read the population. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sc).readFile(population);

		LOG.info("Processing each (natural) person...");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		Counter counter = new Counter("  person # ");
		try{
			bw.write("id,activity_number,activity_type,lon,lat");
			bw.newLine();

			for(Id<Person> id : sc.getPopulation().getPersons().keySet()){
				if(id.toString().startsWith("coct_p")){
					/* It is a natural person, use its selected plan. */
					String[] sa = id.toString().split("_");
					String thisId = sa[2];
					int i = 1;
					Plan plan = sc.getPopulation().getPersons().get(id).getSelectedPlan();
					Iterator<PlanElement> elements = plan.getPlanElements().iterator();
					while(elements.hasNext()){
						PlanElement pe = elements.next();
						if(pe instanceof Activity){
							Activity activity = (Activity) pe;
							String prefix = activity.getType().substring(0, 1);
							String type = null;
							switch (prefix) {
							case "h":
								type = "home";
								break;
							case "w":
								type = "work";
								break;
							case "e":
								String edu = activity.getType().substring(0, 2);
								switch (edu) {
								case "e1":
									type = "school";
									break;
								case "e2":
									type = "post_school";
									break;
								case "e3":
									type = "school_drop";
									break;
								default:
									break;
								}
							case "s":
								type = "shopping";
								break;
							case "l":
								type = "leisure";
								break;
							case "v":
								type = "visit";
								break;
							case "m":
								type = "medical";
								break;
							case "o":
								type = "other";
								break;
							default:
								type= "other";
								break;
							}
							
							Coord c = activity.getCoord();
							
							String line = String.format("%s,%d,%s,%.0f,%.0f\n", 
									thisId, i++, type, c.getX(), c.getY());
							bw.write(line);
						}
					}
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		counter.printCounter();

		Header.printFooter();
	}

}
