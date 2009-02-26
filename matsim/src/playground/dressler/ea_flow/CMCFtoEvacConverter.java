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
package playground.dressler.ea_flow;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.matsim.basic.v01.BasicActImpl;
import org.matsim.basic.v01.BasicActivity;
import org.matsim.basic.v01.BasicKnowledge;
import org.matsim.basic.v01.BasicLegImpl;
import org.matsim.basic.v01.BasicPopulationImpl;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.BasicPerson;
import org.matsim.interfaces.basic.v01.BasicPlan;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.population.PersonImpl;
import org.matsim.population.PopulationWriterV5;
import org.matsim.utils.geometry.CoordImpl;
/**
 * 
 * @author Manuel Schneider
 *
 */
public class CMCFtoEvacConverter {

	private NetworkLayer network;
	private Population population;
	
	public static NetworkLayer constructNetwork(String networkfile, String demandfile) throws JDOMException, IOException{
		// read networ and add en1 and en2 and el1
		NetworkLayer network = CMCFNetworkConverter.readCMCFNetwork(networkfile);
		Coord coord1 = new CoordImpl("0","0");
		Coord coord2 = new CoordImpl("1","1");
		Id matsimid1  = new IdImpl("en1");
		Id matsimid2  = new IdImpl("en2");
		Id matsimid3  = new IdImpl("el1");
		network.createNode(matsimid1, coord1);
		network.createNode(matsimid2, coord2);
		network.createLink(matsimid3, network.getNode("en1"), network.getNode("en2"),
				 10.,100000. ,100000000000000000000.,1.);
		
		//Add links to en1
		SAXBuilder builder = new SAXBuilder();
		Document cmcfdemands = builder.build(demandfile);
		Element demandgraph = cmcfdemands.getRootElement();
		// read and set the nodes
		Element nodes = demandgraph.getChild("demands");
		LinkedList<String> evacnodes = new LinkedList<String>();
		List<Element> commoditylist = nodes.getChildren();
		for (Element commodity : commoditylist){
			 //read the values of the node xml Element as Strings
			 String to = commodity.getChildText("to");
			 if(!evacnodes.contains(to)){
				 evacnodes.add(to);
			 }
		 }
		 Integer counter = 10;
		 for(String id : evacnodes){
			 Id matsimid  = new IdImpl("el"+counter.toString());
			 network.createLink(matsimid, network.getNode(id), network.getNode("en1"), 1.,100000. ,100000000000000000000.,1.);
			 counter++;
		 }
		return network;
	}
	
	@SuppressWarnings("unchecked")
	public static BasicPopulationImpl<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> readCMCFDemands(String filename, NetworkLayer network, boolean coordinates) throws JDOMException, IOException{
		BasicPopulationImpl result = new BasicPopulationImpl();
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
			 String to = "en2";
			 String demand = commodity.getChildText("demand");
			 //build  new Plans in the Population
			 int dem = (int) Math.round(Double.parseDouble(demand));
			 Node tonode = network.getNode(to);
			 Node fromnode = network.getNode(from);
			 Coord coordfrom = fromnode.getCoord();
			 Coord coordto = tonode.getCoord();
			 Link fromlink = null;
			 Link tolink = null;
			 //find edges
			 if (!coordinates){
				LinkedList<Link> tolinks = new LinkedList<Link>();
				tolinks.addAll( tonode.getInLinks().values());
				if(tolinks.isEmpty()){
					throw new IllegalArgumentException(tonode.getOrigId()+ " has no ingoing edges!!!");
				}
				tolink = tolinks.getFirst();
				LinkedList<Link> fromlinks = new LinkedList<Link>();
				fromlinks.addAll( fromnode.getOutLinks().values());
				if(tolinks.isEmpty()){
					throw new IllegalArgumentException(tonode.getOrigId()+ " has no outgoing edges!!!");
				}
				fromlink = fromlinks.getFirst();
				 
			 }
			 for (int i = 1 ; i<= dem ;i++) {
				 Id matsimid  = new IdImpl(id+"."+i);
				 Person p = new PersonImpl(matsimid);
				 Plan plan = new org.matsim.population.PlanImpl(p);
				 BasicActImpl home = new BasicActImpl("home");
				 home.setEndTime(0.);
				 BasicActImpl work = new BasicActImpl("work");
				 BasicLegImpl leg = new BasicLegImpl(BasicLeg.Mode.walk);
				 if (coordinates){
					home.setCoord(coordfrom);
					work.setCoord(coordto);
				 }else{
					home.setLinkId(fromlink.getId());
					work.setLinkId(tolink.getId());
				 }
				 plan.addAct(home);
				 plan.addLeg(leg);
				 plan.addAct(work);
				 p.addPlan(plan);
				 result.addPerson(p);
			 
			 }
		 }
		 
		return result;
	}
	
	
	public static void main(String[] args) {
		String networkfile = "~/skywalker/testcases/swiss_old/org/swissold.xml";
		String demandfile = "~/skywalker/testcases/swiss_old/org/demandsswissold.xml";
		String networkfileout = "~/skywalker/testcases/swiss_old/matsimevac/swiss_old_network_evac.xml";
		String plansfileout = "~/skywalker/testcases/swiss_old/matsimevac/swiss_old_plans_evac.xml";
		
		try {
			NetworkLayer network = constructNetwork(networkfile, demandfile);
			NetworkWriter writer = new NetworkWriter( network, networkfileout);
			writer.write();
			System.out.println(networkfile+"  conveted successfully \n"+"output written in: "+networkfileout);
			BasicPopulationImpl<BasicPerson<BasicPlan, BasicKnowledge<BasicActivity>>> population = readCMCFDemands(demandfile, network, false);
			PopulationWriterV5 pwriter = new PopulationWriterV5( population);
			pwriter.writeFile(plansfileout);
			System.out.println(demandfile+"conveted succssfully \n"+"output written in :\n"+plansfileout);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
