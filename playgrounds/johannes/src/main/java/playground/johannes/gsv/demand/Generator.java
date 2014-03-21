/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.demand;

import java.io.IOException;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class Generator {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Random random = new XORShiftRandom();
		String year = "2009";
		String baseDir = "/home/johannes/gsv/matsim/studies/netz2030/data/raw/innoz/";
		NutsLevel3Zones.zonesFile = "/home/johannes/gsv/matsim/studies/netz2030/data/raw/Zonierung_Kreise_WGS84_Stand2008Attr_WGS84_region.shp";
		NutsLevel3Zones.idMappingsFile = baseDir + "inhabitants.csv";
		/*
		 * create N persons
		 */
		int N = 1000;
		
		Population pop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		for(long i = 0; i < N; i++) {
			Person p = pop.getFactory().createPerson(new IdImpl(i));
			pop.addPerson(p);
			
			Plan plan = pop.getFactory().createPlan();
			p.addPlan(plan);
		}
		/*
		 * 
		 */
		PopulationTaskComposite tasks = new PopulationTaskComposite();
		tasks.addComponent(new PersonEqualZoneDistributionWrapper(baseDir + "inhabitants.csv" , "Einw_g_" + year, random));
		tasks.addComponent(new PersonGenderWrapper(baseDir + "inhabitants.csv", "Einw_m_" + year, "Einw_w_" + year, random));
		tasks.addComponent(new PersonAgeWrapper(baseDir, "Einw_g_" + year, random));
		tasks.addComponent(new PersonEmployedLoader(baseDir + "employees.csv", "EWTAO_" + year, baseDir + "inhabitants.csv" , "Einw_g_" + year, random));
		tasks.addComponent(new PersonCarAvailabilityLoader("/home/johannes/gsv/matsim/studies/netz2030/data/raw/mid/caravailability.age.txt", random));
		
		tasks.apply(pop);
		
		PopulationWriter writer = new PopulationWriter(pop, null);
		writer.write(baseDir + "population.xml");
	}

}
