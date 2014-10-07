/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;

import playground.mmoyo.algorithms.PopulationCleaner;

public class MultimodalScnCreator {
	Network mivNet;
	Network ptNet;
	Population pop;
	final static String MIV = "miv_";
	
	public MultimodalScnCreator (final Network mivNet, final Network ptNet, final Population pop){
		this.mivNet = mivNet;
		this.ptNet = ptNet;
		this.pop= pop;
	}
	
	private void createMultimodalNet(final String outDir){
		NetworkImpl multiModalNet = (NetworkImpl) new DataLoader().createScenario().getNetwork();
		//MergeNetworks.merge(this.mivNet, MIV, this.ptNet, null, multiModalNet);
		
		//add MIVS nodes and links
		for (Node node : this.mivNet.getNodes().values()){
			Id<Node> newId = Id.create(MIV + node.getId(), Node.class);
			multiModalNet.createAndAddNode(newId, node.getCoord());
		}
		
		for (Link l : this.mivNet.getLinks().values()){
			Id<Link> newId = Id.create(MIV + l.getId(), Link.class);
			Id<Node> fromNodeId = Id.create(MIV + l.getFromNode().getId(), Node.class); 
			Node fromNode = multiModalNet.getNodes().get(fromNodeId); 
			Id<Node> toNodeId = Id.create(MIV + l.getToNode().getId(), Node.class); 
			Node toNode = multiModalNet.getNodes().get(toNodeId);
			multiModalNet.createAndAddLink(newId, fromNode, toNode, l.getLength(), l.getFreespeed(), l.getCapacity(), l.getNumberOfLanes(), ((LinkImpl)l).getOrigId(), ((LinkImpl)l).getType());
			LinkImpl newLink = (LinkImpl) multiModalNet.getLinks().get(newId);
			newLink.setAllowedModes(l.getAllowedModes());
		}
		new NetworkWriter(multiModalNet).write(outDir + "mivNetWithSuffix.xml.gz");
		System.out.println("done writting:" + outDir + "mivNetWithSuffix.xml.gz");		

		/*
		//add pt nodes and links
		for (Node node : this.ptNet.getNodes().values()){
			multiModalNet.createAndAddNode(node.getId(), node.getCoord());
		}
		
		for (Link l : this.ptNet.getLinks().values()){
			Node fromNode = multiModalNet.getNodes().get(l.getFromNode().getId()); 
			Node toNode = multiModalNet.getNodes().get(l.getToNode().getId());
			multiModalNet.createAndAddLink(l.getId(), fromNode, toNode, l.getLength(), l.getFreespeed(), l.getCapacity(), l.getNumberOfLanes(), ((LinkImpl)l).getOrigId(), ((LinkImpl)l).getType());
			LinkImpl newLink = (LinkImpl) multiModalNet.getLinks().get(l.getId());
			newLink.setAllowedModes(l.getAllowedModes());
		}
			
		new NetworkWriter(multiModalNet).write(outDir + "multimodalNet.xml.gz");
		System.out.println("done writting:" + outDir + "multimodalNet.xml.gz");
		*/				
	}
	
	private void preparePop(Population pop){
		new PopulationCleaner().run(pop);   //<-- caution!
		/*
		for (Person person: pop.getPersons().values()){
			for (Plan plan : person.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if ((pe instanceof Activity)) {
						ActivityImpl act = (ActivityImpl)pe;
						act.setLinkId(linkId)
					}
				}
			}
		}*/
	}
	
	
	
	public static void main(String[] args) {
		String mivNetPath;
		String ptNetPath;
		String popfilePath;
		
		if (args.length>0){
			mivNetPath = args[0];
			ptNetPath = args[1];
			popfilePath = args[2];
		}else{
			mivNetPath = "../../input/newDemand/network.final.xml.gz";
			ptNetPath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_network.xml.gz";
			popfilePath = "../../input/newDemand/bvg.run190.25pct.100.plans.xml";
		}

		DataLoader dLoader = new DataLoader();
		Network mivNet = dLoader.readNetwork(mivNetPath);
		Network ptNet = dLoader.readNetwork(ptNetPath);
		//Population pop = dLoader.readPopulation(popfilePath);
		
		new MultimodalScnCreator(mivNet, ptNet, null).createMultimodalNet("../../input/newDemand/");
	}
}
