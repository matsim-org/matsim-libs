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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.johannes.gsv.demand.loader.*;
import playground.johannes.gsv.demand.tasks.PlanModeCarPT;
import playground.johannes.gsv.demand.tasks.PlanNetworkConnect;
import playground.johannes.gsv.demand.tasks.PlanRouteLegs;
import playground.johannes.gsv.demand.tasks.PlanTransformCoord;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

import java.io.IOException;
import java.util.Random;

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
		Config config = ConfigUtils.createConfig();
		new ConfigReader(config).readFile(args[0]);
		
		Random random = new XORShiftRandom(4711);
		String year = "2009";

		String baseDir = config.getParam("gsv", "rawDataDir");//[0]; //"/home/johannes/gsv/matsim/studies/netz2030/data/raw/";
		
		NutsLevel3Zones.zonesFile = config.getParam("gsv", "zonesFile");//baseDir + "Zonierung_Kreise_WGS84_Stand2008Attr_WGS84_region.shp";
		NutsLevel3Zones.idMappingsFile = config.getParam("gsv", "idMappingsFile");//baseDir + "innoz/inhabitants.csv";
		/*
		 * create N persons
		 */
		Population pop = null;
		String popFile = config.findParam("plans", "inputPlansFile");
		if(popFile == null || popFile.equalsIgnoreCase("null")) { // returns "null"???
			int N = Integer.parseInt(config.getParam("gsv", "numPersons"));
			pop = create(N);
		} else { 
			pop = loadFromFile(popFile);
		}
		/*
		 * 
		 */
		String netFile = config.getParam("network", "inputNetworkFile");//baseDir + "network.gk3.xml";
		String roadNetFile = config.getParam("gsv", "roadNetworkFile");//baseDir + "network.road.gk3.xml";
		String scheduleFile =  config.getParam("transit", "transitScheduleFile");//baseDir + "transitSchedule.routed.gk3.xml";
//		String scheduleFile =  baseDir + "transitSchedule.longdist.linked.xml";
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		config.transit().setUseTransit(true);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(netFile);
		
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		reader.readFile(scheduleFile);
		/*
		 * 
		 */
		PopulationTaskComposite tasks = new PopulationTaskComposite();
//		tasks.addComponent(new PersonEqualZoneDistribuionWrapper(baseDir + "innoz/inhabitants.csv" , "Einw_g_" + year, random));
		tasks.addComponent(new PersonStopDistributionLoader(baseDir + "innoz/inhabitants.csv" , "Einw_g_" + year, scenario, random));
		tasks.addComponent(new PersonGenderWrapper(baseDir + "innoz/inhabitants.csv", "Einw_m_" + year, "Einw_w_" + year, random));
		tasks.addComponent(new PersonAgeWrapper(baseDir+"innoz/", "Einw_g_" + year, random));
		tasks.addComponent(new PersonEmployedLoader(baseDir + "innoz/employees.csv", "EWTAO_" + year, baseDir + "innoz/inhabitants.csv" , "Einw_g_" + year, random));
		tasks.addComponent(new PersonCarAvailabilityLoader(baseDir + "mid/caravailability.age.txt", random));
		
//		tasks.addComponent(new PlanPrimaryActivityLoader(baseDir + "StrukturAttraktivitaet2010.csv", "A2010", scenario, random));
		tasks.addComponent(new PlanTransformCoord(CRSUtils.getCRS(4326), CRSUtils.getCRS(31467)));
		tasks.addComponent(new PlanPrimaryActivityLoader2(scenario, baseDir + "de.nuts0.shp", random));
		tasks.addComponent(new PlanDepartureTimeLoader(baseDir + "Ganglinien_Standardtag.csv", random));
		tasks.addComponent(new PlanModeCarPT(0.0, random));
		
		tasks.addComponent(new PlanNetworkConnect(roadNetFile));
		tasks.addComponent(new PlanRouteLegs(scenario));
		tasks.apply(pop);
		
		PopulationWriter writer = new PopulationWriter(pop, null);
		writer.write(config.getParam("gsv", "popOutFile"));
	}

	private static Population create(int N) {
		Population pop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		for(long i = 0; i < N; i++) {
			Person p = pop.getFactory().createPerson(Id.create(i, Person.class));
			pop.addPerson(p);
			
			Plan plan = pop.getFactory().createPlan();
			p.addPlan(plan);
		}
		
		return pop;
	}
	
	private static Population loadFromFile(String file) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
//		reader.setValidating(false);
//		reader.setNamespaceAware(false);
		reader.readFile(file);
		return scenario.getPopulation();
	}
}
