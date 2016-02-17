/* *********************************************************************** *
 * project: org.matsim.*
 * NmbmSurveyParser.java
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

package playground.southafrica.population.utilities.activityTypeManipulation;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.population.utilities.PopulationUtils;

/**
 * Once a population has been generated, the {@link Activity} types often 
 * requires adaption to account for different durations. For example, when
 * <i>shopping</i> (s) duration has a large time distribution, it may be  
 * useful to change them to s1, s2, etc., each with a different <i>typical
 * duration</i>. This may result in more accurate simulation results.
 * 
 * This class expected that activity durations have already been analysed. This
 * can be done using {@link PopulationUtils#extractActivityDurations(String, String)}
 * and analysing the durations in R using the script plotSurveyActivityDurations.
 * (TODO That R class should be updated as it is used for more than just survey
 * activities, JWJ - June 2014)
 *
 * @author jwjoubert
 */
public abstract class ActivityTypeManipulator {
	protected final static Logger LOG = Logger.getLogger(ActivityTypeManipulator.class);
	protected Scenario sc;
	protected Map<String, TreeMap<String, Tuple<String, String>>> deciles
		= new TreeMap<String, TreeMap<String, Tuple<String,String>>>();

	
	protected void parsePopulation(String population){
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		/* Read the population file. */
		MatsimPopulationReader mpr = new MatsimPopulationReader(sc);
		mpr.readFile(population);
		
		LOG.info("Population: " + sc.getPopulation().getPersons().size());
		LOG.info("Network: " + sc.getNetwork().getNodes().size() + " nodes; " 
				+ sc.getNetwork().getLinks().size() + " links");
	}
	
	
	/**
	 * Runs through the entire {@link Population}, adapting the activity type 
	 * of each {@link Person}'s selected {@link Plan} based on the area-specific 
	 * implementation of {@link #getAdaptedActivityType(String, double)} method.
	 */
	protected void run(){
		LOG.info("Start manipulating person plans...");
		Counter counter = new Counter("  person # ");
		if(sc == null){
			throw new RuntimeException("Cannot process the population if it has " +
					"not been parsed yet. First run the parsePopulation() method.");
		} else{
			for(Person person : sc.getPopulation().getPersons().values()){
				if(person.getPlans().size() > 1){
					LOG.warn("Person " + person.getId() + " has multiple plans. " +
							"Only the selected plan will be adapted.");
				}
				this.adaptActivityTypes(person.getSelectedPlan());
				counter.incCounter();
			}
		}
		counter.printCounter();
		LOG.info("Done manipulating plans.");
	}
	
	protected Scenario getScenario(){
		return this.sc;
	}
	
	abstract protected void adaptActivityTypes(Plan plan);
	
	abstract protected List<ActivityParams> parseDecileFile(String filename);

}
