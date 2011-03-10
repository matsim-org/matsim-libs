/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkReaderMatsimV1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dressler.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;
/**
 *
 * @author Manuel Schneider
 *
 */
public class CMCFPopulationConverter {


	@SuppressWarnings("unchecked")
	public static Population readCMCFDemands(String filename, NetworkImpl network, boolean coordinates) throws JDOMException, IOException{
		Population result = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		PopulationFactory pb = result.getFactory() ;
		SAXBuilder builder = new SAXBuilder();
		Document cmcfdemands = builder.build(filename);
		Element demandgraph = cmcfdemands.getRootElement();
		// read and set the nodes
		Element nodes = demandgraph.getChild("demands");
		 List<Element> commoditylist = nodes.getChildren();
		 for (Element commodity : commoditylist){
			 //read the values of the node xml Element as Strings
			 String id = commodity.getAttributeValue("id");
			 String from = commodity.getChildText("from");
			 String to = commodity.getChildText("to");
			 String demand = commodity.getChildText("demand");
			 //build  new Plans in the Population
			 int dem = (int) Math.round(Double.parseDouble(demand));
			 Node tonode = network.getNodes().get(new IdImpl(to));
			 Node fromnode = network.getNodes().get(new IdImpl(from));
			 Coord coordfrom = fromnode.getCoord();
			 Coord coordto = tonode.getCoord();
			 Link fromlink = null;
			 Link tolink = null;
			 //find edges
			 if (!coordinates){
				LinkedList<Link> tolinks = new LinkedList<Link>();
				tolinks.addAll( tonode.getInLinks().values());
				if(tolinks.isEmpty()){
					throw new IllegalArgumentException(((NodeImpl) tonode).getOrigId()+ " has no ingoing edges!!!");
				}
				tolink = tolinks.getFirst();
				LinkedList<Link> fromlinks = new LinkedList<Link>();
				fromlinks.addAll( fromnode.getOutLinks().values());
				if(tolinks.isEmpty()){
					throw new IllegalArgumentException(((NodeImpl) tonode).getOrigId()+ " has no outgoing edges!!!");
				}
				fromlink = fromlinks.getFirst();

			 }
			 for (int i = 1 ; i<= dem ;i++) {
				 Id matsimid  = new IdImpl(id+"."+i);
				 PersonImpl p = new PersonImpl(matsimid);
				 PlanImpl plan = new org.matsim.core.population.PlanImpl(p);
//				 BasicActivityImpl home = new BasicActivityImpl("home");
//				 BasicActivityImpl work = new BasicActivityImpl("work");
//				 BasicLegImpl leg = new BasicLegImpl(TransportMode.walk);
				 Activity home ;
				 Activity work ;
				 Leg leg = pb.createLeg(TransportMode.walk);
				 if (coordinates){
//					home.setCoord(coordfrom);
//					work.setCoord(coordto);
					 home = pb.createActivityFromCoord("home", coordfrom) ;
					 work = pb.createActivityFromCoord("work", coordto ) ;
				 }else{
//					home.setLinkId(fromlink.getId());
//					work.setLinkId(tolink.getId());
					 home = pb.createActivityFromLinkId( "home" , fromlink.getId() ) ;
					 work = pb.createActivityFromLinkId( "work" , tolink.getId() ) ;
				 }
				 home.setEndTime(0.);
				 plan.addActivity(home);
				 plan.addLeg(leg);
				 plan.addActivity(work);
				 p.addPlan(plan);
				 result.addPerson(p);

			 }
		 }

		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if((args.length<3) || (args.length > 4)){
			System.out.println("usage:1. c ore e for coordinates or edges in plans 2. argument network file 3. argument inputfile 4. argument outfile (optional)");
			return;
		}
		boolean coordinates= true;
		String coord = args[0].trim();
		if (coord.equals("e")){
			coordinates = false;
		}
		String netfile = args[1].trim();
		String inputfile = args[2].trim();
		String outfile = inputfile.substring(0, inputfile.length()-4)+"_msimDEM.xml";
		if(args.length == 4){
			outfile = args[3];
		}
		try {
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			NetworkImpl network = scenario.getNetwork();
			NetworkReaderMatsimV1 netreader = new NetworkReaderMatsimV1(scenario);
			netreader.parse(netfile);
			Population population = readCMCFDemands(inputfile,network,coordinates);
			new PopulationWriter(population, network).write(outfile);
//			PopulationWriterV5 writer = new PopulationWriterV5( population);
			System.out.println(inputfile+"conveted "+"output written in :"+outfile);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}


	}
}
