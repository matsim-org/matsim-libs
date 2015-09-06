/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class DemoScenario {

	/**
	 * @param args
	 * @throws FactoryException 
	 */
	public static void main(String[] args) throws FactoryException {
		String popFile = args[0];
		String facFile = args[1];
		String netFile = args[2];
		int n = Integer.parseInt(args[3]);
		String outDir = args[4];
		
		Logger logger = Logger.getLogger(DemoScenario.class);
		
		MathTransform transform = CRS.findMathTransform(CRSUtils.getCRS(31467), CRSUtils.getCRS(3857));
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		/*
		 * remove foreign persons and extract subsample
		 */
		logger.info("Loading persons...");
		MatsimPopulationReader pReader = new MatsimPopulationReader(scenario);
		pReader.readFile(popFile);
		logger.info("Done.");
		
		logger.info("Removing foreign persons...");
		Set<Id<Person>> remove = new HashSet<>();
		for(Id<Person> id : scenario.getPopulation().getPersons().keySet()) {
			if(id.toString().startsWith("foreign")) {
				remove.add(id);
			}
		}
		
		int cnt = 0;
		for(Id<Person> id : remove) {
			if(scenario.getPopulation().getPersons().remove(id) != null) {
				cnt++;
			}
		}
		logger.info(String.format("Done. Removed %s foreign persons.", cnt));
		
		logger.info("Drawing population subsample...");
		List<Person> persons = new ArrayList<>(scenario.getPopulation().getPersons().values());
		Collections.shuffle(persons);
		Population population = PopulationUtils.createPopulation(config);
		cnt = 0;
		for(int i = 0; i < n; i++) {
			population.addPerson(persons.get(i));
		}
		logger.info("Done.");
		
		logger.info("Bluring activity end times...");
		Random random = new XORShiftRandom();
		for(Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i+=2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					double endTim = act.getEndTime() - 15*60 + (random.nextDouble() * 30*60);
					act.setEndTime(endTim);
					double startTim = act.getStartTime() - 15*60 + (random.nextDouble() * 30*60);
					act.setStartTime(startTim);

				}
			}
		}
		logger.info("Done.");
		
		logger.info("Writing population...");
		PopulationWriter pWriter = new PopulationWriter(population);
		pWriter.write(String.format("%s/plans.xml.gz", outDir));
		logger.info("Done.");
		/*
		 * filter only used facilities
		 */
		logger.info("Loading facilities...");
		MatsimFacilitiesReader fReader = new MatsimFacilitiesReader(scenario);
		fReader.readFile(facFile);
		logger.info("Done.");
		
		logger.info("Removing unsused facilities...");
		Set<Id<ActivityFacility>> unused = new HashSet<>(scenario.getActivityFacilities().getFacilities().keySet());
		for(Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i+=2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					unused.remove(act.getFacilityId());
				}
			}
		}
		logger.info("Done.");
		
		logger.info("Transforming facility coordinates...");
		for(ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()) {
			double[] points = new double[] { fac.getCoord().getX(), fac.getCoord().getY() };
			try {
				transform.transform(points, 0, points, 0, 1);
			} catch (TransformException e) {
				e.printStackTrace();
			}

			((ActivityFacilityImpl)fac).setCoord(new Coord(points[0], points[1]));
		}
		logger.info("Done.");
		
		logger.info("Writing facilities...");
		FacilitiesWriter fWrtier = new FacilitiesWriter(scenario.getActivityFacilities());
		fWrtier.write(String.format("%s/facilities.xml.gz", outDir));
		logger.info("Done.");
		/*
		 * clean network from foreign links
		 */
		logger.info("Loading network...");
		MatsimNetworkReader nReader = new MatsimNetworkReader(scenario);
		nReader.readFile(netFile);
		logger.info("Done.");
		
		logger.info("Removing foreign links...");
		Set<Id<Link>> linksRemove = new HashSet<>();
		for(Id<Link> id : scenario.getNetwork().getLinks().keySet()) {
			if(id.toString().contains(".l")) {
				linksRemove.add(id);
			}
		}
		
		for(Id<Link> id : linksRemove) {
			scenario.getNetwork().removeLink(id);
		}
		logger.info("Done.");
		
		logger.info("Removing foreign nodes...");
		Set<Id<Node>> nodesRemove = new HashSet<>();
		for(Id<Node> id : scenario.getNetwork().getNodes().keySet()) {
			if(id.toString().contains(".n")) {
				nodesRemove.add(id);
			}
		}
		
		for(Id<Node> id : nodesRemove) {
			scenario.getNetwork().removeNode(id);
		}
		logger.info("Done.");
		
		logger.info("Transforming node coordinates...");
		for(Node node : scenario.getNetwork().getNodes().values()) {
			double[] points = new double[] { node.getCoord().getX(), node.getCoord().getY() };
			try {
				transform.transform(points, 0, points, 0, 1);
			} catch (TransformException e) {
				e.printStackTrace();
			}

			((NodeImpl) node).setCoord(new Coord(points[0], points[1]));
		}
		logger.info("Done.");
		
		logger.info("Writing network...");
		NetworkWriter nWriter = new NetworkWriter(scenario.getNetwork());
		nWriter.write(String.format("%s/network.xml.gz", outDir));
		logger.info("Done.");
	}

}
