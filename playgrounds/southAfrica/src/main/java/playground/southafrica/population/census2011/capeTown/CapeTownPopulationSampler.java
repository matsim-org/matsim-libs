/* *********************************************************************** *
 * project: org.matsim.*
 * CapeTownPopulationSampler.java                                                                        *
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
package playground.southafrica.population.census2011.capeTown;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to sample from the population prepared by the {@link CapeTownScenarioCleaner}.
 * 
 * @author jwjoubert
 */
public class CapeTownPopulationSampler {
	private final static Logger LOG = Logger.getLogger(CapeTownPopulationSampler.class);

	/**
	 * It is important that the files generated and cleaned through 
	 * {@link CapeTownScenarioCleaner} is in the given input folder.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CapeTownPopulationSampler.class.toString(), args);
		
		String inputFolder = args[0];
		inputFolder += inputFolder.endsWith("/") ? "" : "/";
		String outputFolder = args[1];
		outputFolder += outputFolder.endsWith("/") ? "" : "/";
		double fraction = Double.parseDouble(args[2]);
		
		Scenario scIn = parseInputScenario(inputFolder);
		Scenario scOut = sample(scIn, fraction);
		
		LOG.info("Population statistics:");
		LOG.info("Original population:");
		printPopulationStatistics(scIn);
		LOG.info("Sampled population:");
		printPopulationStatistics(scOut);
		
		writeOutputScenario(scOut, outputFolder);
		
		/* Copy the config file as well because it contains crucial information
		 * about the plans scoring and routing parameters.  */
		try {
			FileUtils.copyFile(new File(inputFolder + "config.xml"), new File(outputFolder + "config.xml"));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not copy the config file.");
		}
		
		Header.printFooter();
	}
	
	
	private CapeTownPopulationSampler(){
	}
	
	private static Scenario parseInputScenario(String input){
		ComprehensivePopulationReader cpr = new ComprehensivePopulationReader();
		cpr.parse(input);
		
		return cpr.getScenario();
	}
	
	private static void writeOutputScenario(Scenario sc, String folder){
		new PopulationWriter(sc.getPopulation()).write(folder + "population.xml.gz");
		new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(folder + "populationAttributes.xml.gz");
		
		new HouseholdsWriterV10(sc.getHouseholds()).writeFile(folder + "households.xml.gz");
		ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(sc.getHouseholds().getHouseholdAttributes());
		oaw.putAttributeConverter(Coord.class, new CoordConverter());
		oaw.writeFile(folder + "householdAttributes.xml.gz");
		
		new FacilitiesWriter(sc.getActivityFacilities()).write(folder + "facilities.xml.gz");
	}
	
	private static Scenario sample(Scenario scIn, double fraction){
		Scenario scOut = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		/* Add all the facilities. */
		for(ActivityFacility facility : scIn.getActivityFacilities().getFacilities().values()){
			scOut.getActivityFacilities().addActivityFacility(facility);
		}
		
		ObjectAttributes pAttrIn = scIn.getPopulation().getPersonAttributes();
		ObjectAttributes pAttrOut = scOut.getPopulation().getPersonAttributes();

		/* Sample the households. 
		 * 
		 * NOTE: we have to measure the private persons based on the number of
		 * households, and NOT individuals in the population as the latter 
		 * includes commercial vehicles, which is sampled separately. */
		Iterator<Household> iterator = scIn.getHouseholds().getHouseholds().values().iterator();
		while(scOut.getHouseholds().getHouseholds().size() < fraction*((double)scIn.getHouseholds().getHouseholds().size())
				&& iterator.hasNext()){
			Household hh = iterator.next();
			double r = MatsimRandom.getLocalInstance().nextDouble();
			if(r <= fraction){
				/* Copy the household. */
				scOut.getHouseholds().getHouseholds().put(hh.getId(), hh);
				
				/* Copy household attributes. */
				ObjectAttributes hha = scOut.getHouseholds().getHouseholdAttributes();
				hha.putAttribute(hh.getId().toString(), "homeCoord", 
						scIn.getHouseholds().getHouseholdAttributes().getAttribute(hh.getId().toString(), "homeCoord"));
				hha.putAttribute(hh.getId().toString(), "housingType", 
						scIn.getHouseholds().getHouseholdAttributes().getAttribute(hh.getId().toString(), "housingType"));
				hha.putAttribute(hh.getId().toString(), "mainDwellingType", 
						scIn.getHouseholds().getHouseholdAttributes().getAttribute(hh.getId().toString(), "mainDwellingType"));
				
				/* Copy each household member. */
				for(Id<Person> pId : hh.getMemberIds()){
					scOut.getPopulation().addPerson(scIn.getPopulation().getPersons().get(pId));
					
					/* Copy member attributes. */
					pAttrOut.putAttribute(pId.toString(), "age", pAttrIn.getAttribute(pId.toString(), "age"));
					pAttrOut.putAttribute(pId.toString(), "gender", pAttrIn.getAttribute(pId.toString(), "gender"));
					pAttrOut.putAttribute(pId.toString(), "householdId", pAttrIn.getAttribute(pId.toString(), "householdId"));
					pAttrOut.putAttribute(pId.toString(), "population", pAttrIn.getAttribute(pId.toString(), "population"));
					pAttrOut.putAttribute(pId.toString(), "relationship", pAttrIn.getAttribute(pId.toString(), "relationship"));
					pAttrOut.putAttribute(pId.toString(), "school", pAttrIn.getAttribute(pId.toString(), "school"));
					pAttrOut.putAttribute(pId.toString(), "subpopulation", pAttrIn.getAttribute(pId.toString(), "subpopulation"));
				}
			}
		}
		
		/* Sample the commercial vehicles. */
		for(Id<Person> pId : scIn.getPopulation().getPersons().keySet()){
			if(pId.toString().startsWith("coct_c")){
				double r = MatsimRandom.getLocalInstance().nextDouble();
				if(r <= fraction){
					/* Add the commercial vehicle to the population. */
					scOut.getPopulation().addPerson(scIn.getPopulation().getPersons().get(pId));
					
					/* Copy the commercial vehicle attributes. */
					pAttrOut.putAttribute(pId.toString(), "subpopulation", pAttrIn.getAttribute(pId.toString(), "subpopulation"));
				}
			}
		}
		
		return scOut;
	}

	
	private static void printPopulationStatistics(Scenario sc){
		int persons = 0;
		int commercials = 0;
		
		for(Id<Person> pId : sc.getPopulation().getPersons().keySet()){
			if(pId.toString().startsWith("coct_p")){
				persons++;
			} else if(pId.toString().startsWith("coct_c")){
				commercials++;
			} else{
				LOG.error("Don't know what type of agent '" + pId.toString() + "' is.");
			}
		}
		
		LOG.info("--------------------------------------------------");
		LOG.info("           Number of households: " + sc.getHouseholds().getHouseholds().size());
		LOG.info("    Number of (natural) persons: " + persons);
		LOG.info("  Number of commercial vehicles: " + commercials);
		LOG.info("--------------------------------------------------");
	}

}
