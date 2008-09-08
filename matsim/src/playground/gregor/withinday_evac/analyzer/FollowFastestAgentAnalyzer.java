/* *********************************************************************** *
 * project: org.matsim.*
 * FollowAgentAnalyzer.java
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

package playground.gregor.withinday_evac.analyzer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.network.Node;

import playground.gregor.withinday_evac.Beliefs;
import playground.gregor.withinday_evac.communication.InformationEntity;
import playground.gregor.withinday_evac.communication.NextLinkWithEstimatedTravelTimeMessage;
import playground.gregor.withinday_evac.communication.InformationEntity.MSG_TYPE;
import playground.gregor.withinday_evac.debug.DebugFollowFastestAgent;

public class FollowFastestAgentAnalyzer implements Analyzer {
	
	private final Beliefs beliefs;
	private double coef;
	private static final double NORMALIZER = -3600/6;
	private static double EPSILON = 0.01;

	public FollowFastestAgentAnalyzer(final Beliefs beliefs) {
		this.beliefs = beliefs;
		this.coef = 1;
		if (Controler.getIteration() >= 50) {
			EPSILON = (0.01 * 1 / (Controler.getIteration() - 49));
		}
	}
	
	
//	//progressive
//	public NextLinkOption getAction(final double now) {
//		Node to = this.beliefs.getCurrentLink().getToNode();
//		Node from = this.beliefs.getCurrentLink().getFromNode();
//		
//		final HashMap<Link,Counter> counts = new HashMap<Link,Counter>();
//		for (Link l : to.getOutLinks().values()) {
//			if (l.getToNode() == from) {
//				continue;
//			}
//			
//			Collection<InformationEntity> ies = this.beliefs.getInfos(now, MSG_TYPE.MY_NEXT_LINK_W_EST_TRAVELTIME, l.getToNode().getId());
//			if (ies.size() == 0) {
//				continue;
//			}
//			Counter c = new Counter(0,0);
//			for (InformationEntity ie : ies) {
//				final NextLinkWithEstimatedTravelTimeMessage m = (NextLinkWithEstimatedTravelTimeMessage) ie.getMsg();
//				c.value += 1;
//				c.estTimeSum += m.getEstTTime() / NORMALIZER;
//			}
//			counts.put(l, c);
//		}
//		
//		double weightSum = 0;
//		for (final Counter c : counts.values()) {
//			c.value = Math.exp(c.estTimeSum/c.value);
//			weightSum += c.value; 
//		}
//		
//		double selNum = weightSum * MatsimRandom.random.nextDouble();
//		for (Entry<Link,Counter> e : counts.entrySet()) {
//			selNum -= e.getValue().value;
//			if (selNum <= 0) {
//				return new NextLinkOption(e.getKey(),1 * this.coef);
//			}			
//		}
//		
//		return null;
//	}
	
	
//	current link
	public NextLinkOption getAction(final double now) {
		Id nodeId = this.beliefs.getCurrentLink().getToNode().getId();
		Collection<InformationEntity> ies = this.beliefs.getInfos(now, MSG_TYPE.MY_NEXT_LINK_W_EST_TRAVELTIME, nodeId);
		if (ies.size() == 0) {
			DebugFollowFastestAgent.increNull();
			return null;
		}
		
		
		final HashMap<Link,Counter> counts = new HashMap<Link,Counter>();
		ArrayList<Link> indices = new ArrayList<Link>();
		for (final InformationEntity ie : ies) {
			final NextLinkWithEstimatedTravelTimeMessage m = (NextLinkWithEstimatedTravelTimeMessage) ie.getMsg();
			final Counter c = counts.get(m.getLink());
			if (c != null) {
				c.value += 1.0;				
				c.estTimeSum += m.getEstTTime()/NORMALIZER;
				
//				c.value = 1.0;
//				c.estTimeSum = Math.max(c.estTimeSum, m.getEstTTime()/NORMALIZER);
			} else {
				counts.put(m.getLink(), new Counter(1,m.getEstTTime()/NORMALIZER));
				indices.add(m.getLink());
				
			}
			//DEBUG
			DebugFollowFastestAgent.updateAlt(indices.size());
		}
				
		return getNextLinkGreedy(indices,counts);	
	


	}
	

	
	
	
	private NextLinkOption getNextLinkGreedy(final ArrayList<Link> indices,
			final HashMap<Link, Counter> counts) {
		
		if(MatsimRandom.random.nextDouble() < EPSILON){
			//DEBUG
			DebugFollowFastestAgent.incrGreedy();
			
			Node n = this.beliefs.getCurrentLink().getToNode();
			double selNum = n.getOutLinks().size() * MatsimRandom.random.nextDouble();
			for (Link l : n.getOutLinks().values()) {
				selNum--;
				if (selNum <= 0) {
					return new NextLinkOption(l,1 * this.coef);
				}
			}
			return null;
		} else {
			double bestEstTime = Double.POSITIVE_INFINITY;
			Link link = null;
			for (final Link l : indices) {
				Counter c = counts.get(l);
				double currEstTime = c.estTimeSum / c.value;
				if (currEstTime < bestEstTime) {
					bestEstTime = currEstTime;
					link = l;
				}
			}
			if (link == null) {
				throw new RuntimeException("this should not happen!!");
				
			}
			return new NextLinkOption(link,1 * this.coef);
		}
		

	}


	private NextLinkOption getNextLinkLogit(final ArrayList<Link> indices,
			final HashMap<Link, Counter> counts) {
		double weightSum = 0;
		for (final Link l : indices) {
			Counter c = counts.get(l);
			c.value = Math.exp(c.estTimeSum/c.value);
			weightSum += c.value; 
		}
		
		double selNum = weightSum * MatsimRandom.random.nextDouble();
		for (final Link l : indices) {
			Counter c = counts.get(l);
			selNum -= c.value;
			if (selNum <= 0) {
				return new NextLinkOption(l,1 * this.coef);
			}
		}
				
		
		
		return null;
	}





	private static class Counter {
		double value;
		private double estTimeSum;
		public Counter(final double i, final double estTime) {
			this.value = i;
			this.estTimeSum = estTime;
		}
		
		@Override
		public String toString(){
			return this.value + " " + this.estTimeSum;
		}
		
	}



	public void setCoefficient(final double coef) {
		this.coef = coef;
		
	}


}
