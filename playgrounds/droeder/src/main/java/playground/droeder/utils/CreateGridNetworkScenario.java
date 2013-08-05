/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * @author droeder
 *
 */
class CreateGridNetworkScenario {
	
	private static final Logger log = Logger
			.getLogger(CreateGridNetworkScenario.class);

	
	private static final String[] ARGUMENTS = {
		null // [[outputdirectory]]
	};
	
	@SuppressWarnings("serial")
	private static List<Relation> relations = new ArrayList<Relation>(){{
		add(new Relation(new IdImpl("A"), new IdImpl("B"), TransportMode.pt, 4000));
		add(new Relation(new IdImpl("B"), new IdImpl("A"), TransportMode.pt, 4000));
		
		add(new Relation(new IdImpl("A"), new IdImpl("C"), TransportMode.pt, 2000));
		add(new Relation(new IdImpl("C"), new IdImpl("A"), TransportMode.pt, 2000));
		
		add(new Relation(new IdImpl("B"), new IdImpl("C"), TransportMode.pt, 2000));
		add(new Relation(new IdImpl("C"), new IdImpl("B"), TransportMode.pt, 2000));
	}};
	
	/**
	 * does not work currently. need links for relations...
	 * @param args
	 */
	@Deprecated
	public static void main(String[] args){
		String[] arguments;
		if(args.length != 0){
			arguments = args;
		}else{
			arguments = ARGUMENTS;
		}
		String outputdirectory = DaFileUtil.checkFileEnding(arguments[0]);
		log.info("output-directory: " + outputdirectory);
		DaFileUtil.checkAndMaybeCreateDirectory(outputdirectory, true);
		
		log.info("creating network.");
		Network net = createTestGridNetwork(6, 11, 1000.);
		new NetworkWriter(net).write(outputdirectory + "gridNetwork.xml.gz");
		log.info("Done (creating network).");
		
		log.info("creating population.");
		Population p = createPopulation(relations, net);
		new PopulationWriter(p, net).write(outputdirectory + "gridNEtworkPopulation.xml.gz");
		log.info("done (creating population).");
		
		log.info("finished.");
	}
	
	private static Network createTestGridNetwork(int rows, int columns, double distanceFromNodeToNode) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Network network = scenario.getNetwork();
		NetworkFactory nf = network.getFactory();

		Node n;
		// creates nodes
		for(int row = 1; row < (rows + 1); row++){
			for(int column = 1; column < (columns + 1); column++){
				n = nf.createNode(scenario.createId(String.valueOf(column) + "." + String.valueOf(row)), 
						scenario.createCoord(column * distanceFromNodeToNode, row * distanceFromNodeToNode));
				network.addNode(n);
			}
		}
		
		// create links
		Node from = null, to;	
		Link l;
		//create rows
		for(int row = 1; row <(rows + 1); row++){
			from = null;
			for(int col = 1; col < (columns + 1); col++){
				if(from == null){
					from = network.getNodes().get(scenario.createId(String.valueOf(col) + "." + String.valueOf(row)));
				}else{
					to = network.getNodes().get(scenario.createId(String.valueOf(col) + "." + String.valueOf(row)));
					l = nf.createLink(scenario.createId(from.getId().toString() + "_" + to.getId().toString()), from, to);
					network.addLink(l);
					l = nf.createLink(scenario.createId(to.getId().toString() + "_" + from.getId().toString()), to, from);
					network.addLink(l);
					from = to;
				}
			}
		}
		
		//create columns
		for(int col = 1; col < (columns + 1); col++){
			from = null;
			for(int row = 1; row <(rows + 1); row++){
				if(from == null){
					from = network.getNodes().get(scenario.createId(String.valueOf(col) + "." + String.valueOf(row)));
				}else{
					to = network.getNodes().get(scenario.createId(String.valueOf(col) + "." + String.valueOf(row)));
					l = nf.createLink(scenario.createId(from.getId().toString() + "_" + to.getId().toString()), from, to);
					network.addLink(l);
					l = nf.createLink(scenario.createId(to.getId().toString() + "_" + from.getId().toString()), to, from);
					network.addLink(l);
					from = to;
				}
			}
		}
		
		
		Set<String> modes = new TreeSet<String>();
		modes.add(TransportMode.car);
		for (Link link : network.getLinks().values()) {
			link.setLength(distanceFromNodeToNode);
			link.setCapacity(2000.0);
			link.setFreespeed(13.8);
			link.setAllowedModes(modes);
			link.setNumberOfLanes(1.0);
		}
		
		return scenario.getNetwork();
	}
	
	
	private static Population createPopulation(List<Relation> relations, Network net){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(sc).readFile(netFile);
//		Network net = sc.getNetwork();
		Population p = sc.getPopulation();
		PopulationFactory pFac = p.getFactory();
		
		Link o, d;
		Coord origin, destination;
		double start, interval;
		Leg l;
		Activity home, work;
		Person per;
		Plan plan;
		
		for(Relation r: relations){
			o = net.getLinks().get(r.getOriginLinkId());
			d = net.getLinks().get(r.getDestinationLinkId());
			origin = o.getToNode().getCoord();
			destination = d.getToNode().getCoord();
			start = r.getStart();
			interval = (r.getEnd()-start)/r.getNrAgents();
			
			
			for(int i = 0; i< r.getNrAgents(); i++){
				per = pFac.createPerson(sc.createId(r.getOriginLinkId() + "_" + r.getDestinationLinkId() + "_" + i));
				plan =  pFac.createPlan();
				
				home = pFac.createActivityFromCoord("home", origin);
				home.setEndTime(start);
				((ActivityImpl) home).setLinkId(o.getId());
				plan.addActivity(home);
				
				l = pFac.createLeg(TransportMode.pt);
				l.setDepartureTime(start);
				plan.addLeg(l);
				
				work = pFac.createActivityFromCoord("work", destination);
				((ActivityImpl) work).setLinkId(d.getId());
				plan.addActivity(work);
				
				per.addPlan(plan);
				p.addPerson(per);
				
				start+=interval;
			}
		}
		
		return sc.getPopulation();
	}

	private static class Relation{
		
		private Id from;
		private Id to;
		private double nr;
		private double start = 8 * 3600;
		private double end = 10 * 3600;
	
		public Relation(Id fromLink, Id toLink, String mode, double nrOfAgents){
			this.from = fromLink;
			this.to = toLink;
			this.nr = nrOfAgents;
		}
	
		/**
		 * @return the from
		 */
		public Id getOriginLinkId() {
			return from;
		}
	
		/**
		 * @return the to
		 */
		public Id getDestinationLinkId() {
			return to;
		}
	
		/**
		 * @return the nr
		 */
		public double getNrAgents() {
			return nr;
		}
	
		/**
		 * @return the start
		 */
		public double getStart() {
			return start;
		}
	
		/**
		 * @return the end
		 */
		public double getEnd() {
			return end;
		}
	}
}
