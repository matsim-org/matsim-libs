/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.utilities.Header;

/**
 * Class to take the original population components of the 2010 SANRAL project
 * and convert them so that:
 * <ul>
 * 		<li> the {@link Person}{@link Id} reflects the subpopulation;
 * 		<li> each {@link Person} is given an object attribute to reflect its 
 * 			 specific subpopulation;
 * </ul>
 *
 * @author jwjoubert
 */
public class SanralPopulationConverter {
	private final static Logger LOG = Logger.getLogger(SanralPopulationConverter.class);

	/**
	 * Running the population converter. 
	 * 
	 * @param args The following arguments are required, and in the following 
	 * order:
	 * <ol>
	 * 		<li> input population file;
	 * 		<li> prefix to be used in the {@link Id}s of each {@link Person};
	 * 		<li> the attribute value to be used to indicate the subpopulation to
	 * 			 which the {@link Person}s belong (it is assumed to be the same 
	 * 			 for all {@link Person}s in the population file); 
	 * 		<li> the fraction (between 0.0 and 1.0) of the original 
	 * 			 {@link Person}s that must be included in the output population;
	 * 		<li> output population file; and
	 * 		<li> output file for the {@link ObjectAttributes} of the population. 
	 */
	public static void main(String[] args) {
		Header.printHeader(SanralPopulationConverter.class.toString(), args);
		
		String inputFile = args[0];
		String idPrefix = args[1];
		String subPopulation = args[2];
		Double fraction = Double.parseDouble(args[3]);
		String outputFile = args[4];
		String attributesFile = args[5];
		String inputCRS = args[6];
		String outputCRS = args[7];
		Boolean convertDurationToEndTime= Boolean.parseBoolean(args[8]);
		
		SanralPopulationConverter.Run(inputFile, idPrefix, inputCRS, subPopulation, fraction, outputFile, attributesFile, outputCRS, convertDurationToEndTime);
		
		Header.printFooter();
	}
	
	
	public static void Run(String inputFile, String idPrefix, String inputCRS, String subPopulation, double fraction, String outputFile, String attributesFile, String outputCRS, boolean convertDurationToEndTime){
		Scenario scNew = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory pf = scNew.getPopulation().getFactory();
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(inputFile);
		int id = 0;
		
		for(Person person : sc.getPopulation().getPersons().values()){
			if(MatsimRandom.getRandom().nextDouble() <= fraction){
				/* Create new person. */
				Id<Person> newId = Id.create(idPrefix + "_" + id++, Person.class);
				Person newPerson = pf.createPerson(newId);
				
				for(Plan plan : person.getPlans()){
					/* Convert all the activity locations if necessary. */
					if(!inputCRS.equalsIgnoreCase(outputCRS)){
						CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
						for(PlanElement pe : plan.getPlanElements()){
							if(pe instanceof Activity){
								ActivityImpl act = (ActivityImpl)pe;
								act.setCoord(ct.transform(act.getCoord()));
								
								/* Remove link IDs associated with activities. */
								act.setLinkId(null);
							}
						}
					}
					
					/* Convert duration to end time if required. */
					if(convertDurationToEndTime){
						changeDurationToEndTime(plan);
					}
					
					newPerson.addPlan(plan);
				}
				
				scNew.getPopulation().addPerson(newPerson);
				
				/* Create person attributes. */
				scNew.getPopulation().getPersonAttributes().putAttribute(newId.toString(), scNew.getConfig().plans().getSubpopulationAttributeName(), subPopulation);
			}
		}
		
		/* Write everything to file. */
		new PopulationWriter(scNew.getPopulation(), null).write(outputFile);
		new ObjectAttributesXmlWriter(scNew.getPopulation().getPersonAttributes()).writeFile(attributesFile);
	}
	
	
	private static void changeDurationToEndTime(Plan plan){
		double estimatedTravelTime = Time.parseTime("00:15:00");
		double cumulativeEndTime = ((Activity)plan.getPlanElements().get(0)).getEndTime();
		for(int i = 2 ; i < plan.getPlanElements().size()-2; i+= 2){ // Don't adapt final activity.
			PlanElement pe = plan.getPlanElements().get(i);
			if(pe instanceof Activity){
				Activity act = (Activity)pe;
				Double duration = act.getMaximumDuration();
				Double endTime = act.getEndTime();
				if(duration == Time.UNDEFINED_TIME){
					LOG.warn("Expected maximum duration to be set, but it was not. Activity not changed.");
				} else{
					if(endTime != Time.UNDEFINED_TIME){
						LOG.warn("Overwriting end time for " + plan.getPerson().getId() + " from " + Time.writeTime(endTime) + " to " + Time.writeTime(cumulativeEndTime));
					}
					cumulativeEndTime += estimatedTravelTime + duration;
					act.setEndTime(cumulativeEndTime);
				}
				act.setMaximumDuration(Time.UNDEFINED_TIME);
			} else{
				throw new RuntimeException("Expected `Activity', but got " + pe.getClass().getName());
			}
		}
	}

}
