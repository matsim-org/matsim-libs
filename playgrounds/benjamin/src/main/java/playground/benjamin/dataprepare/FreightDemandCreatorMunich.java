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
package playground.benjamin.dataprepare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.ScenarioFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.RouteUtils;

import playground.benjamin.BkPaths;

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
		Scenario sc = new ScenarioFactoryImpl().createScenario();
		Config cf = sc.getConfig();
		cf.network().setInputFile(netFile);
		cf.plans().setInputFile(plansFile);
		
		ScenarioLoader sl = new ScenarioLoaderImpl(sc);
		sl.loadScenario();
		Population population = sc.getPopulation();
		Network network = sc.getNetwork();
		
		//instancing the new population
		Scenario newScenario = new ScenarioImpl();
		filteredPlans = newScenario.getPopulation();
		
		List<Id> nodesInBavaria = createListWithNodesInBavaria(vertexFile, shapeFile);
		
		for(Person person : population.getPersons().values()){
			//getting nodes from route
			Set<Id> nodesFromNodes = getNodesFromNodes(person, network);
			//getting nodes from linkIds, if route is empty (origin/destination at same node)
			Set<Id> nodesFromLinks = getNodesFromLinks(person, network);
			
			//checking if at least one node (or link) from a person's plan is in Bavaria
			for (Id bavariaId : nodesInBavaria) {
				if (nodesFromNodes.contains(bavariaId)){
					//adding filtered plans to a new, empty population
					if(filteredPlans.getPersons().keySet().contains(person.getId())){
						//do nothing
					}
					else{
						filteredPlans.addPerson(person);
					}
				} 
				else if (nodesFromLinks.contains(bavariaId)){
					//adding these people only if they are not yet in population
					if(filteredPlans.getPersons().keySet().contains(person.getId())){
						//do nothing
					}
					else{
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
	private Set<Id> getNodesFromNodes(Person person, Network network) {
		Set<Id> list = new HashSet<Id>();
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
	private Set<Id> getNodesFromLinks(Person person, Network network) {
		Set<Id> list = new HashSet<Id>();
		for (Plan plan : person.getPlans()) {
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity){
					Activity act = (Activity) planElement;
					Id linkId = act.getLinkId();
					String link = linkId.toString();
					String[] endNodes = link.split("-");
					Id firstNode = new IdImpl(endNodes[0]);
					Id secondNode = new IdImpl(endNodes[1]);
					
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
	private List<Id> createListWithNodesInBavaria(String vertexFile, String shapeFile) throws IOException {
		List<Id> list = new ArrayList<Id>();
		//getting a list of all regionIds in Bavaria
		Set<Id> regionsInBavaria = getRegionsFromShape(shapeFile);
		//getting a map of "all nodeIds to Regions" in network 2004 (prognose_2025)
		Map<Id, Id> allNodeIds2Region = getNodeIds2Region(vertexFile);
		
		//nochmal nachvollziehen!
		for(Id nodeId : allNodeIds2Region.keySet()){
			Id regionId = allNodeIds2Region.get(nodeId);
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
	private Map<Id, Id> getNodeIds2Region(final String vertexFile) throws IOException {
		final Map<Id, Id> nodeIds2Region = new HashMap<Id, Id>();
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
				Id nodeNumber = new IdImpl(row[KNOTENNUMMER]);
				Id regionNumber = new IdImpl(row[ZONE]);
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
	@SuppressWarnings("unchecked")
	private Set<Id> getRegionsFromShape(String shapeFile) throws IOException {
		Set<Id> regions = new HashSet<Id>();
		FeatureSource fts = ShapeFileReader.readDataFile(shapeFile);
		for (Feature ft : (Collection<Feature>) fts.getFeatures()){
			String kkz = ft.getAttribute("KKZ").toString();
			Id kkzId = new IdImpl(kkz);
			regions.add(kkzId);
		}
		return regions;
	}
}
