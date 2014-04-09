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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.johannes.gsv.demand.loader.PersonAgeWrapper;
import playground.johannes.gsv.demand.loader.PersonCarAvailabilityLoader;
import playground.johannes.gsv.demand.loader.PersonEmployedLoader;
import playground.johannes.gsv.demand.loader.PersonGenderWrapper;
import playground.johannes.gsv.demand.loader.PersonStopDistributionLoader;
import playground.johannes.gsv.demand.loader.PlanDepartureTimeLoader;
import playground.johannes.gsv.demand.loader.PlanPrimaryActivityLoader;
import playground.johannes.gsv.demand.tasks.PlanModeCarPT;
import playground.johannes.gsv.demand.tasks.PlanNetworkConnect;
import playground.johannes.gsv.demand.tasks.PlanRouteLegs;
import playground.johannes.gsv.demand.tasks.PlanTransformCoord;
import playground.johannes.sna.gis.CRSUtils;
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
//		System.getProperties().put("http.proxyHost", "localhost");
//		System.getProperties().put("http.proxyHost", "wwwproxy.bahn-net.db.de");
//		System.getProperties().put("http.proxyPort", "3128");
//		System.getProperties().put("http.proxyPort", "8080");
//		System.getProperties().put("http.proxySet", "true");
		
//		System.setProperty("org.geotools.referencing.forceXY", "true");
		
		Random random = new XORShiftRandom(4711);
		String year = "2009";
		String baseDir = args[0]; //"/home/johannes/gsv/matsim/studies/netz2030/data/raw/";
		NutsLevel3Zones.zonesFile = baseDir + "Zonierung_Kreise_WGS84_Stand2008Attr_WGS84_region.shp";
		NutsLevel3Zones.idMappingsFile = baseDir + "innoz/inhabitants.csv";
		/*
		 * create N persons
		 */
		int N = Integer.parseInt(args[1]); //1000;
		Population pop = create(N);
		
//		Population pop = loadFromFile(args[1]);
		/*
		 * 
		 */
		String netFile = baseDir + "network.gk3.xml";
		String roadNetFile = baseDir + "network.road.gk3.xml";
		String scheduleFile =  baseDir + "transitSchedule.routed.gk3.xml";
//		String scheduleFile =  baseDir + "transitSchedule.longdist.linked.xml";
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		config.scenario().setUseTransit(true);
		
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
		tasks.addComponent(new PlanPrimaryActivityLoader(baseDir + "StrukturAttraktivitaet2010.csv", "A2010", scenario, random));
		tasks.addComponent(new PlanDepartureTimeLoader(baseDir + "Ganglinien_Standardtag.csv", random));
		tasks.addComponent(new PlanModeCarPT(0.0, random));
		tasks.addComponent(new PlanTransformCoord(CRSUtils.getCRS(4326), CRSUtils.getCRS(31467)));
		tasks.addComponent(new PlanNetworkConnect(roadNetFile));
		tasks.addComponent(new PlanRouteLegs(scenario));
		tasks.apply(pop);
		
		PopulationWriter writer = new PopulationWriter(pop, null);
		writer.write(baseDir + "population.xml");
	}

	private static Population create(int N) {
		Population pop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		for(long i = 0; i < N; i++) {
			Person p = pop.getFactory().createPerson(new IdImpl(i));
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
