/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionTree.java
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

package playground.gregor.withindayevac;

import java.util.ArrayList;

import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Plan;

import playground.gregor.withindayevac.analyzer.Analyzer;
import playground.gregor.withindayevac.analyzer.ChooseRandomLinkAnalyzer;
import playground.gregor.withindayevac.analyzer.FollowFastestAgentAnalyzer;
import playground.gregor.withindayevac.analyzer.FollowHerdAnalyzer;
import playground.gregor.withindayevac.analyzer.FollowPlanAnalyzer;
import playground.gregor.withindayevac.analyzer.Option;
import playground.gregor.withindayevac.analyzer.ReRouteAnalyzer;
import playground.gregor.withindayevac.communication.InformationExchanger;
import playground.gregor.withindayevac.debug.DebugDecisionTree;

public class DecisionTree {
	

	private final Plan plan;
	private final Beliefs beliefs;
	private Node root;
	private final Intentions intentions;
	private final NetworkLayer network;
	private final InformationExchanger informationExchanger;


	public DecisionTree(final Beliefs beliefs, final Plan plan, final Intentions intentions, final NetworkLayer network, final int iteration, final InformationExchanger informationExchanger) {
		this.beliefs = beliefs;
		this.plan = plan;
		this.intentions = intentions;
		this.network = network;
		this.informationExchanger = informationExchanger;
		init(iteration);
	}
	
	
	public Option getNextOption(final double now){
		Node current = this.root;
		while(current.isInternal) {
			current = chooseNextNode(current,now);
		}
		//DEBUG
		DebugDecisionTree.incr(current.analyzer);
		
		Link cL = this.beliefs.getCurrentLink();
		Link nextLink = current.getOption().getNextLink();
		boolean found = false;
		for (Link l : cL.getToNode().getOutLinks().values()) {
			if (l == nextLink) {
				found = true;
			}
		}
		
		if (!found) {
			throw new RuntimeException("this should not happen");
		}
		
		
		return current.getOption();
	}

	private Node chooseNextNode(final Node current, final double now) {
		double weightSum = 0;
		for (Node n : current.getChildren()) {
			n.run(now);
			weightSum += n.getNodeWeight();
		}

		double selNum = weightSum * MatsimRandom.random.nextDouble();
		for (Node n : current.getChildren()) {
			selNum -= n.getNodeWeight();
			if (selNum <= 0) {
				return n;
			}
		}
		
		
		return null;
	}


	//for now every thing is hardcoded
	private void init(final int iteration) {
		this.root = new Node();
		
		Node internal = new Node();
		this.root.addChildNode(internal);
		internal.setNodeWeight(0.99);
		
		Node followPlan = new Node();
		FollowPlanAnalyzer fpa = new FollowPlanAnalyzer(this.beliefs,this.plan, this.informationExchanger);
		fpa.setCoefficient(1);

		followPlan.setAnalyzer(fpa);

		
		
//		this.root.addChildNode(followPlan);
		
		Node randomLink = new Node();
		ChooseRandomLinkAnalyzer crla = new ChooseRandomLinkAnalyzer(this.beliefs);
		crla.setCoefficient(1);

		randomLink.setAnalyzer(crla);
//		this.root.addChildNode(randomLink);
//		internal.addChildNode(randomLink);
		
//		Node internal2 = new Node();
//		internal.addChildNode(internal2);
//		internal2.setNodeWeight(0.8);
		
		Node followFastest = new Node();
		FollowFastestAgentAnalyzer fha = new FollowFastestAgentAnalyzer(this.beliefs);
//		FollowHerdAnalyzer fha = new FollowHerdAnalyzer(this.beliefs);
		fha.setCoefficient(1);
		followFastest.setAnalyzer(fha);
		if (MatsimRandom.random.nextDouble() < 0.2) {
			internal.addChildNode(followPlan);	
		} else {
			internal.addChildNode(followFastest);	
		}
		
		
		Node internal1 = new Node();
		internal1.setNodeWeight(0.0000000001);
		internal.addChildNode(internal1);
		
		
		Node internal2 = new Node();
		internal2.setNodeWeight(0.0000000001);
		internal1.addChildNode(internal2);
		
		Node reRoute = new Node();
		ReRouteAnalyzer rra = new ReRouteAnalyzer(this.beliefs,this.network,this.intentions);
		rra.setCoefficient(1);
		reRoute.setAnalyzer(rra);
//		internal2.addChildNode(reRoute);
		internal2.addChildNode(randomLink);
		
		Node followHerd = new Node();
		FollowHerdAnalyzer herdAna = new FollowHerdAnalyzer(this.beliefs);
		followHerd.setAnalyzer(herdAna);
		herdAna.setCoefficient(1);
		internal1.addChildNode(followHerd);
		
	}
	
	
	private static class Node {
		private boolean isInternal = false;
		private Analyzer analyzer = null;
		private double nodeWeight = 0.0;
		private final ArrayList<Node> children = new ArrayList<Node>();
		private Option option = null;
		
		public void addChildNode(final Node n) {
			this.isInternal = true;
			this.children.add(n);
		}
		
		public ArrayList<Node> getChildren() {
			return this.children;
		}
		
		public void setAnalyzer(final Analyzer ana) {
			this.analyzer = ana;
		}
		
		public void setNodeWeight(final double weight) {
			this.nodeWeight = weight;
		}
		
		public double getNodeWeight() {
			if (this.isInternal) {
				return this.nodeWeight;
			}
			
			if (this.option == null) {
				return 0;
			}
			
			return this.option.getConfidence();
		}

		public void run(final double now) {
			if(this.analyzer != null) {
				this.option = this.analyzer.getAction(now);
			}
			
		}
		
		public Option getOption(){
			return this.option;
		}
		
//		private Analyzer getAnalyzer() {
//			return this.analyzer;
//		}
	}

}
