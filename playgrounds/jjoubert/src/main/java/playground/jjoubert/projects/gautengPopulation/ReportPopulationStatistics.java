/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
package playground.jjoubert.projects.gautengPopulation;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.utilities.Header;

/**
 * Reports some basic population statistics to check/validate the output 
 * generated after running {@link PuttingGautengPopulationTogether}.
 * 
 * @author jwjoubert
 */
public class ReportPopulationStatistics {
	private final static Logger LOG = Logger.getLogger(ReportPopulationStatistics.class);
	private static Scenario sc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ReportPopulationStatistics.class.toString(), args);
		
		String population = args[0];
		String populationAttributes = args[1];
		
		ReportPopulationStatistics.Run(population, populationAttributes);
		
		Header.printFooter();
	}
	
	
	public static void Run(String population, String populationAttributes){
		/* Read the population and population attributes. */
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(population);
		new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes()).parse(populationAttributes);
		
		/* Parse and report some numbers. */
		parseAttribute("subpopulation");
		parseAttribute("vehicleTollClass");
		parseAttribute("eTag");
		parseAttribute("intraGauteng");
	}
	
	
	private static Map<String, Integer> parseAttribute(String attribute){
		Map<String, Integer> map = new TreeMap<String, Integer>();
		
		for(Id id : sc.getPopulation().getPersons().keySet()){
			Object o = sc.getPopulation().getPersonAttributes().getAttribute(id.toString(), attribute);
			if(o != null){
				if(map.containsKey(o.toString())){
					int oldInt = map.get(o.toString());
					map.put(o.toString(), oldInt+1);
				} else{
					map.put(o.toString(), new Integer(1));
				}
			}
		}
		
		LOG.info("  ==>  Statistics: " + attribute);
		for(String s : map.keySet()){
			LOG.info(String.format("         |_ %s: %d (%.2f%%)", s, map.get(s), ((double)map.get(s)/(double)sc.getPopulation().getPersons().size())*100));
		}

		return map;
	}
	

}
