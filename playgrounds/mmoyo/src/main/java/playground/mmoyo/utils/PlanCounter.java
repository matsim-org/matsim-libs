/* *********************************************************************** *
 * project: org.matsim.*
 * PlanCounter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.mmoyo.utils;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.apache.log4j.Logger;
import java.util.Map;
import java.util.TreeMap;

public class PlanCounter {
	private final static Logger log = Logger.getLogger(PlanCounter.class);
	
	/**
	 * Counts the number of plan per agent
	 */
	public void run(final Population pop){
		Map <Integer, Integer> numPlansMap = new TreeMap <Integer, Integer>();
		for(Person person : pop.getPersons().values()){
			int numPlans = person.getPlans().size(); 
			
			if (numPlansMap.containsKey(numPlans)){
				numPlansMap.put(numPlans, numPlansMap.get(numPlans)+1 );
			}else{
				numPlansMap.put(numPlans, 1);
			}
		}
		
		for(Map.Entry <Integer,Integer> entry: numPlansMap.entrySet() ){
			int key = entry.getKey(); 
			int value = entry.getValue();
			final String SEP = " ";
			log.info(key + SEP + value );
		}
		
	}
	
	public static void main(String[] args) {
		String popFilePath;
		if(args.length==0){
			popFilePath = "../playgrounds/mmoyo/output/cadyts/w6d0t1200_w10d0t240_w8d0.5t720_w10d0t1020_w10d0.4t60_w8d0t900_WITHHOME.xml.gz"; 
		}else{
			popFilePath = args[0];
		}
			
		Population pop = new DataLoader().readPopulation(popFilePath);	
		new PlanCounter().run(pop);
	}

}
