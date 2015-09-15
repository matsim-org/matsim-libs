/* *********************************************************************** *
 * project: org.matsim.*
 * TransitDemandCreatorMunich.java
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
package playground.benjamin.scenarios.munich;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.opengis.feature.simple.SimpleFeature;

import playground.benjamin.BkPaths;
import playground.benjamin.utils.CheckingTabularFileHandler;

/**
 * @author benjamin
 *
 */
public class FreightDemandCreatorMunich {
	private static final Logger log = Logger.getLogger(FreightDemandCreatorMunich.class);
	
	String netFile = BkPaths.RUNSSVN + "run1051/output_network.xml.gz";
	String plansFile = BkPaths.RUNSSVN + "run1051/output_plans.xml.gz";
	
	public static final String vertexFile = BkPaths.SHAREDSVN + "studies/countries/de/prognose_2025/orig/netze/netz-2004/strasse/knoten.csv";
	public static final String shapeFile = "../../detailedEval/Net/shapeFromVISUM/Verkehrszellen_Umrisse_area.SHP";
	
	Population filteredPlans = null;
	String outputFile = "../../detailedEval/pop/gueterVerkehr/freightDemandBavaria.xml";
	
	
	public static void main(String[] args) throws IOException {
		FreightDemandCreatorMunich fdc = new FreightDemandCreatorMunich();
		fdc.run(args);
	}

	private void run(String[] args) throws IOException{
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Config cf = sc.getConfig();
		cf.network().setInputFile(netFile);
		cf.plans().setInputFile(plansFile);
		
		ScenarioUtils.loadScenario(sc);
		Population population = sc.getPopulation();
		Network network = sc.getNetwork();
		
		//instancing the new population
		Scenario newScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		filteredPlans = newScenario.getPopulation();
		
		List<Id<Node>> nodesInBavaria = createListWithNodesInBavaria(vertexFile, shapeFile);
		
		for(Person person : population.getPersons().values()){
			//getting nodes from route
			Set<Id<Node>> nodesFromNodes = getNodesFromNodes(person, network);
			//getting nodes from linkIds, if route is empty (origin/destination at same node)
			Set<Id<Node>> nodesFromLinks = getNodesFromLinks(person, network);
			
			//checking if at least one node (or link) from a person's plan is in Bavaria
			for (Id<Node> bavariaId : nodesInBavaria) {
				if (nodesFromNodes.contains(bavariaId)){
					//check if population already contains person
					if(filteredPlans.getPersons().keySet().contains(person.getId())){
						//do nothing
					}
					else{
						//add person
						filteredPlans.addPerson(person);
					}
				} 
				else if (nodesFromLinks.contains(bavariaId)){
					//check if population already contains person
					if(filteredPlans.getPersons().keySet().contains(person.getId())){
						//do nothing
					}
					else{
						//add person
						filteredPlans.addPerson(person);
					}
				}
			}
		}
		writePlans(filteredPlans, network);
	}

	/**
	 * @param filteredPlans
	 * @param network 
	 */
	private void writePlans(Population filteredPlans, Network network) {
		PopulationWriter populationWriter = new PopulationWriter(filteredPlans, network);
		populationWriter.write(outputFile);
	}

	/**
	 * @param person
	 * @param network 
	 * @return
	 */
	//muss ich erst noch verstehen...   -.-
	private Set<Id<Node>> getNodesFromNodes(Person person, Network network) {
		Set<Id<Node>> list = new HashSet<>();
		for (Plan plan : person.getPlans()) {
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg){
					Leg leg = (Leg) planElement;
					if (leg.getRoute() instanceof NetworkRoute){
						NetworkRoute route = (NetworkRoute) leg.getRoute();
						List<Node> nodes = RouteUtils.getNodes(route, network);
							for(Node n : nodes){
							list.add(n.getId());
							}
					}
				}
			}
		}
		return list;
	}

	/**
	 * @param person
	 * @param network 
	 * @return
	 */
	private Set<Id<Node>> getNodesFromLinks(Person person, Network network) {
		Set<Id<Node>> list = new HashSet<>();
		for (Plan plan : person.getPlans()) {
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity){
					Activity act = (Activity) planElement;
					Id<Link> linkId = act.getLinkId();
					String link = linkId.toString();
					String[] endNodes = link.split("-");
					Id<Node> firstNode = Id.create(endNodes[0], Node.class);
					Id<Node> secondNode = Id.create(endNodes[1], Node.class);
					
					list.add(firstNode);
					list.add(secondNode);
				}
			}
		}
		return list;
	}

	/**
	 * @param vertexFile
	 * @param shapeFile
	 * @return
	 * @throws IOException 
	 */
	private List<Id<Node>> createListWithNodesInBavaria(String vertexFile, String shapeFile) throws IOException {
		List<Id<Node>> list = new ArrayList<>();
		//getting a list of all regionIds in Bavaria
		Set<String> regionsInBavaria = getRegionsFromShape(shapeFile);
		//getting a map of "all nodeIds to Regions" in network 2004 (prognose_2025)
		Map<Id<Node>, String> allNodeIds2Region = getNodeIds2Region(vertexFile);
		
		//nochmal nachvollziehen!
		for(Id<Node> nodeId : allNodeIds2Region.keySet()){
			String regionId = allNodeIds2Region.get(nodeId);
			if(regionsInBavaria.contains(regionId)){
				list.add(nodeId);
			}
		}
		return list;
	}

	/**
	 * @param vertexFile
	 * @return
	 * @throws IOException 
	 */
	private Map<Id<Node>, String> getNodeIds2Region(final String vertexFile) throws IOException {
		final Map<Id<Node>, String> nodeIds2Region = new HashMap<>();
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(vertexFile);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

			private static final int KNOTENNUMMER = 0;
			private static final int ZONE = 5;
			
			@Override
			public void startRow(String[] row) {
				check(row);
				if(!first) {
					parseAndAddNode(row);
				} else {
					// This is the header. Nothing to do.
				}
				first = false;
				}

			private void parseAndAddNode(String[] row) {
				Id<Node> nodeNumber = Id.create(row[KNOTENNUMMER], Node.class);
				String regionNumber = row[ZONE];
				nodeIds2Region.put(nodeNumber, regionNumber);
			}
		});
		return nodeIds2Region;
	}

	/**
	 * @param shapeFile
	 * @return
	 * @throws IOException 
	 */
	private Set<String> getRegionsFromShape(String shapeFile) throws IOException {
		Set<String> regions = new HashSet<>();
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(shapeFile)){
			String kkz = ft.getAttribute("KKZ").toString();
			regions.add(kkz);
		}
		return regions;
	}
}
