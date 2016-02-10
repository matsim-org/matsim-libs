/* *********************************************************************** *
 * project: org.matsim.*
 * RunFacilityRelocator.java
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
package playground.jjoubert.projects.capeTownFreight.moveFacilities;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

/**
 * @author jwjoubert
 *
 */
public class RunFacilityRelocator {
	final private static Logger LOG = Logger.getLogger(RunFacilityRelocator.class);

	/**
	 * Class to execute the {@link FacilityRelocator} class.
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RunFacilityRelocator.class.toString(), args);
		
		String populationToRelocate = args[0];
		String relocation = args[1];
		String populationRelocated = args[2];
		String network = args[3];
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).parse(network);
		
		FacilityRelocator fr = new FacilityRelocator(sc.getNetwork(), relocation);
		
		new MatsimPopulationReader(sc).parse(populationToRelocate);
		
		Scenario newSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory pf = newSc.getPopulation().getFactory();
		
		int count = 0;
		
		List<Id<Person>> affectedPersons = new ArrayList<>();
		for(Person person : sc.getPopulation().getPersons().values()){
			Person newPerson = pf.createPerson(person.getId());
			
			Plan plan = person.getSelectedPlan();
			int relocationCount = CTUtilities.countNumberOfAffectedFacilities(plan);
			if(relocationCount > 0){
				Plan newPlan = fr.processPlan(plan);
				newPerson.addPlan(newPlan);
				if(count++ < 5){
					LOG.warn("Relocated agent: " + person.getId().toString());
					if(count == 5){
						LOG.warn("Future occurences of this warning will be surpressed.");
					}
				}
				affectedPersons.add(person.getId());
			} else{
				newPerson.addPlan(plan);
			}
			newSc.getPopulation().addPerson(newPerson);
		}

		/* Write the resulting population to file. */
		new PopulationWriter(newSc.getPopulation()).write(populationRelocated);
		
		/* Write the list of IDs that are affected. */
		BufferedWriter bw = IOUtils.getBufferedWriter(args[4]);
		try{
			bw.write("Id");
			bw.newLine();
			for(Id<Person> id : affectedPersons){
				bw.write(id.toString());
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + args[4]);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + args[4]);
			}
		}
		
		
		Header.printFooter();
	}

}
